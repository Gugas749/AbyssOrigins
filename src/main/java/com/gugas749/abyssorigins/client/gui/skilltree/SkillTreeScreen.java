package com.gugas749.abyssorigins.client.gui.skilltree;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.packet.UnlockSkillPacket;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SkillTreeScreen extends Screen {

    private static final int NODE_SIZE    = 40;
    private static final int GRID_SPACING = 90;

    private final Screen parent;

    private SkillTree       skillTree;
    private PlayerSkillData skills;
    private PlayerOriginData originData;
    private Origin          origin;

    private float   zoom       = 1.0f;
    private float   panX       = 0;
    private float   panY       = 0;
    private boolean isDragging = false;
    private double  lastMouseX, lastMouseY;

    private SkillNode hoveredNode = null;
    private final Set<String> excludedSkills = new HashSet<>();

    public SkillTreeScreen(Screen parent) {
        super(Component.translatable("gui.abyssorigins.skill_tree"));
        this.parent = parent;
    }

    public SkillTreeScreen() { this(null); }

    @Override
    protected void init() {
        super.init();

        Player player = minecraft.player;
        if (player == null) { onClose(); return; }

        origin     = AbyssOriginsAPI.getPlayerOrigin(player);
        skills     = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        originData = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        if (origin == null) {
            player.displayClientMessage(Component.literal("\u00a7eYou do not have an origin assigned."), false);
            onClose(); return;
        }

        skillTree = SkillTreeRegistry.getTree(origin.getId()).orElse(null);

        if (skillTree == null) {
            Abyssorigins.LOGGER.warn("[AbyssOrigins] No skill tree found for origin: {}", origin.getId());
            player.displayClientMessage(Component.literal("\u00a7eNo skill tree available for this origin yet."), false);
            onClose(); return;
        }

        calculateExcludedSkills();
        panX = this.width / 2f;
        panY = this.height / 2f - 50;

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(this.width / 2 - 40, this.height - 25, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> zoom = Math.min(2.0f, zoom + 0.1f))
            .bounds(this.width - 30, 50, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> zoom = Math.max(0.5f, zoom - 0.1f))
            .bounds(this.width - 30, 75, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("R"), b -> {
            zoom = 1.0f; panX = this.width / 2f; panY = this.height / 2f - 50;
        }).bounds(this.width - 30, 100, 20, 20).build());
    }

    private void calculateExcludedSkills() {
        excludedSkills.clear();
        for (String skillId : skills.getUnlockedSkills()) {
            SkillNode node = skillTree.getSkill(skillId);
            if (node != null) excludedSkills.addAll(node.getExclusions());
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xDD101020);

        if (skillTree == null || skills == null) {
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        drawGridBackground(gfx);
        drawConnections(gfx);

        hoveredNode = null;
        for (SkillNode node : skillTree.getAllSkills())
            drawSkillNode(gfx, node, mouseX, mouseY);

        drawUIOverlay(gfx);

        if (hoveredNode != null) drawTooltip(gfx, hoveredNode, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawGridBackground(GuiGraphics gfx) {
        int gridSize = (int)(50 * zoom);
        int ox = (int)(panX % gridSize), oy = (int)(panY % gridSize);
        for (int x = ox; x < this.width;  x += gridSize) gfx.fill(x, 0, x + 1, this.height, 0x20FFFFFF);
        for (int y = oy; y < this.height; y += gridSize) gfx.fill(0, y, this.width, y + 1,    0x20FFFFFF);
    }

    private void drawConnections(GuiGraphics gfx) {
        for (SkillTree.SkillConnection conn : skillTree.getConnections()) {
            SkillNode from = skillTree.getSkill(conn.getFromSkillId());
            SkillNode to   = skillTree.getSkill(conn.getToSkillId());
            if (from == null || to == null) continue;

            int[] fp = screenPos(from.getGridX(), from.getGridY());
            int[] tp = screenPos(to.getGridX(),   to.getGridY());

            boolean fromDone = skills.hasSkill(from.getId());
            boolean toDone   = skills.hasSkill(to.getId());
            boolean toExcl   = excludedSkills.contains(to.getId());

            int color;
            if (toDone)        color = 0xFF55FF55;
            else if (toExcl)   color = 0xFF553333;
            else if (fromDone) color = 0xFFFFFF55;
            else               color = 0xFF444444;

            drawConnection(gfx, fp[0], fp[1], tp[0], tp[1], color);
        }
    }

    private void drawConnection(GuiGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        int t = Math.max(1, (int)(2 * zoom));
        int midY = (y1 + y2) / 2;
        gfx.fill(x1 - t/2, y1, x1 + t/2 + 1, midY, color);
        gfx.fill(Math.min(x1,x2) - t/2, midY - t/2, Math.max(x1,x2) + t/2 + 1, midY + t/2 + 1, color);
        gfx.fill(x2 - t/2, midY, x2 + t/2 + 1, y2, color);
    }

    private void drawSkillNode(GuiGraphics gfx, SkillNode node, int mouseX, int mouseY) {
        int[] pos    = screenPos(node.getGridX(), node.getGridY());
        int x        = pos[0], y = pos[1];
        int size     = (int)(NODE_SIZE * zoom);
        int half     = size / 2;
        int left     = x - half, top    = y - half;
        int right    = x + half, bottom = y + half;

        if (right < 0 || left > this.width || bottom < 0 || top > this.height) return;

        boolean unlocked  = skills.hasSkill(node.getId());
        boolean excluded  = excludedSkills.contains(node.getId());
        boolean canUnlock = node.canUnlock(skills.getUnlockedSkillsSet(), excludedSkills, originData.getOriginLevel());
        boolean hasPoints = skills.getSkillPoints() >= node.getPointCost();
        boolean hovered   = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;

        if (hovered) hoveredNode = node;

        int bgColor, borderColor;
        if (unlocked)               { bgColor = 0xFF225522; borderColor = 0xFF55FF55; }
        else if (excluded)          { bgColor = 0xFF331111; borderColor = 0xFF662222; }
        else if (canUnlock && hasPoints) { bgColor = 0xFF333322; borderColor = 0xFFFFFF55; }
        else if (canUnlock)         { bgColor = 0xFF332222; borderColor = 0xFFFF8855; }
        else                        { bgColor = 0xFF222222; borderColor = 0xFF444444; }

        if (hovered && !excluded)   { bgColor = brighten(bgColor, 30); borderColor = 0xFFFFFFFF; }

        int bw = Math.max(2, (int)(3 * zoom));
        gfx.fill(left - bw, top - bw, right + bw, bottom + bw, borderColor);
        gfx.fill(left, top, right, bottom, bgColor);

        int indSize = Math.max(4, (int)(8 * zoom));
        gfx.fill(right - indSize - 2, top + 2, right - 2, top + indSize + 2, node.getBranchColor());
        gfx.fill(left + 2, top + 2, left + indSize + 2, top + indSize + 2, node.getTierColor());

        if (zoom >= 0.7f) {
            int textColor = unlocked ? 0xFFFFFFFF : (excluded ? 0xFF666666 : (canUnlock ? 0xFFEEEEEE : 0xFFAAAAAA));

            String icon = node.getIcon();
            gfx.drawString(font, icon, x - font.width(icon) / 2, y - 8, textColor, true);

            // FIX: .getString() to resolve the Component to translated text
            String name = node.getDisplayName().getString();
            if (font.width(name) > size - 4) name = name.substring(0, Math.min(7, name.length())) + "..";
            gfx.drawString(font, name, x - font.width(name) / 2, y + 2, textColor, true);

            if (!unlocked) {
                String cost = String.valueOf(node.getPointCost());
                gfx.drawString(font, cost, x - font.width(cost) / 2, bottom - 10,
                    hasPoints ? 0xFFFFFF00 : 0xFFFF4444, true);
            } else {
                gfx.drawString(font, "\u00a7a\u2713", x - font.width("\u00a7a\u2713") / 2, bottom - 10, 0xFFFFFF, true);
            }
        }
    }

    private void drawUIOverlay(GuiGraphics gfx) {
        int cx = this.width / 2;
        gfx.fill(0, 0, this.width, 45, 0xCC000000);

        // FIX: .getString() on tree.getDisplayName()
        String title = skillTree.getDisplayName().getString() + " Skill Tree";
        gfx.drawString(font, title, cx - font.width(title) / 2, 8, 0xFFFFFFFF, true);

        String info = String.format("Level %d | Skill Points: %d", originData.getOriginLevel(), skills.getSkillPoints());
        gfx.drawString(font, info, cx - font.width(info) / 2, 22, 0xFF55FF55, true);

        String zoomStr = String.format("Zoom: %.0f%%", zoom * 100);
        gfx.drawString(font, zoomStr, this.width - 70, 35, 0xFFAAAAAA, true);

        String instructions = "Drag to pan | Scroll to zoom | Click to unlock";
        gfx.drawString(font, instructions, cx - font.width(instructions) / 2, this.height - 40, 0xFF888888, true);

        int lx = 10, ly = 55;
        gfx.drawString(font, "Branches:", lx, ly, 0xFFAAAAAA, true); ly += 11;
        drawLegendItem(gfx, lx, ly, 0xFFFF4444, "Offense");   ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFF4444FF, "Defense");   ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFF44FF44, "Utility");   ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFFFF8800, "Path A");    ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFF00CCFF, "Path B");    ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFFAA44AA, "Hybrid");    ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFFFFFF00, "Specialist");ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFF880000, "Forbidden"); ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFFFFFFAA, "Ascended");  ly += 9;
        drawLegendItem(gfx, lx, ly, 0xFFFF44FF, "Synergy");
    }

    private void drawLegendItem(GuiGraphics gfx, int x, int y, int color, String text) {
        gfx.fill(x, y, x + 8, y + 8, color);
        gfx.drawString(font, text, x + 12, y, 0xFFCCCCCC, true);
    }

    private void drawTooltip(GuiGraphics gfx, SkillNode node, int mouseX, int mouseY) {
        boolean unlocked  = skills.hasSkill(node.getId());
        boolean excluded  = excludedSkills.contains(node.getId());
        boolean canUnlock = node.canUnlock(skills.getUnlockedSkillsSet(), excludedSkills, originData.getOriginLevel());
        boolean hasPoints = skills.getSkillPoints() >= node.getPointCost();

        List<String> lines = new ArrayList<>();
        String prefix = unlocked ? "\u00a7a" : (excluded ? "\u00a74\u00a7m" : (canUnlock ? "\u00a7e" : "\u00a77"));

        // FIX: .getString() on all node.getDisplayName() / node.getDescription() calls
        lines.add(prefix + "\u00a7l" + node.getDisplayName().getString());
        lines.add("\u00a75Tier " + node.getTier().getTier() + " \u00a77| \u00a7"
            + branchColorCode(node.getBranch()) + node.getBranch().name());
        lines.add("");
        lines.add("\u00a7f" + node.getDescription().getString());
        lines.add("");
        lines.add("\u00a76Requirements:");

        boolean levelMet = originData.getOriginLevel() >= node.getTier().getLevelRequired();
        lines.add((levelMet ? "\u00a7a" : "\u00a7c") + " Level " + node.getTier().getLevelRequired()
            + (levelMet ? " \u2713" : " \u2717"));
        lines.add((hasPoints || unlocked ? "\u00a7a" : "\u00a7c") + " Cost: " + node.getPointCost() + " SP"
            + (hasPoints || unlocked ? " \u2713" : " \u2717"));

        for (String prereqId : node.getPrerequisites()) {
            SkillNode prereq = skillTree.getSkill(prereqId);
            if (prereq != null) {
                boolean met = skills.hasSkill(prereqId);
                // FIX: .getString() on prereq.getDisplayName()
                lines.add((met ? "\u00a7a" : "\u00a7c") + " Requires: " + prereq.getDisplayName().getString()
                    + (met ? " \u2713" : " \u2717"));
            }
        }

        if (!node.getExclusions().isEmpty() && !unlocked) {
            lines.add("");
            lines.add("\u00a7c\u00a7lWARNING: Locks out:");
            for (String exclId : node.getExclusions()) {
                SkillNode excl = skillTree.getSkill(exclId);
                // FIX: .getString() on excl.getDisplayName()
                if (excl != null) lines.add("\u00a7c \u2717 " + excl.getDisplayName().getString());
            }
        }

        lines.add("");
        if (unlocked)               lines.add("\u00a7a\u00a7l\u2713 UNLOCKED");
        else if (excluded)          { lines.add("\u00a74\u00a7l\u2717 LOCKED OUT"); lines.add("\u00a77(Another skill excludes this)"); }
        else if (canUnlock && hasPoints) lines.add("\u00a7e\u00a7lClick to unlock!");
        else if (canUnlock)         lines.add("\u00a7cNeed more skill points");
        else                        lines.add("\u00a77Requirements not met");

        int maxW = lines.stream().mapToInt(l -> font.width(l.replaceAll("\u00a7.", ""))).max().orElse(0);
        maxW = Math.min(maxW + 16, 280);
        int th = lines.size() * 10 + 8;

        int tx = mouseX + 15, ty = mouseY - 10;
        if (tx + maxW > this.width  - 10) tx = mouseX - maxW - 15;
        if (ty + th   > this.height - 10) ty = this.height - th - 10;
        if (ty < 50) ty = 50;

        gfx.fill(tx - 4, ty - 4, tx + maxW + 4, ty + th + 4, 0xF0100010);
        gfx.fill(tx - 3, ty - 3, tx + maxW + 3, ty + th + 3, 0xFF5000AA);
        gfx.fill(tx - 2, ty - 2, tx + maxW + 2, ty + th + 2, 0xF0100010);

        int textY = ty;
        for (String line : lines) {
            gfx.drawString(font, line, tx, textY, 0xFFFFFFFF, true);
            textY += 10;
        }
    }

    private boolean wasMousePressed = false;

    @Override
    public void tick() {
        super.tick();
        long win = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(win,
            org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        double mx = minecraft.mouseHandler.xpos() * width  / minecraft.getWindow().getWidth();
        double my = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getHeight();
        if (pressed && !wasMousePressed) {
            if (hoveredNode != null) tryUnlock(hoveredNode);
            else { isDragging = true; lastMouseX = mx; lastMouseY = my; }
        }
        if (pressed && isDragging) { panX += (float)(mx - lastMouseX); panY += (float)(my - lastMouseY); lastMouseX = mx; lastMouseY = my; }
        if (!pressed) isDragging = false;
        wasMousePressed = pressed;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        float oldZoom = zoom;
        zoom = Math.max(0.4f, Math.min(2.5f, zoom + (float)(scrollY * 0.1)));
        float factor = zoom / oldZoom;
        panX = (float)(mx - (mx - panX) * factor);
        panY = (float)(my - (my - panY) * factor);
        return true;
    }

    private void tryUnlock(SkillNode node) {
        if (!skills.hasSkill(node.getId()) && !excludedSkills.contains(node.getId())
                && node.canUnlock(skills.getUnlockedSkillsSet(), excludedSkills, originData.getOriginLevel())
                && skills.getSkillPoints() >= node.getPointCost()) {
            PacketDistributor.sendToServer(new UnlockSkillPacket(node.getId()));
            skills.unlockSkill(node.getId());
            skills.spendSkillPoints(node.getPointCost());
            calculateExcludedSkills();
        }
    }

    private int[] screenPos(int gridX, int gridY) {
        return new int[]{ (int)(panX + gridX * GRID_SPACING * zoom), (int)(panY + gridY * GRID_SPACING * zoom) };
    }

    private static int brighten(int color, int amt) {
        int a=(color>>24)&0xFF, r=Math.min(255,((color>>16)&0xFF)+amt),
            g=Math.min(255,((color>>8)&0xFF)+amt), b=Math.min(255,(color&0xFF)+amt);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private static char branchColorCode(SkillNode.SkillBranch branch) {
        return switch (branch) {
            case CORE -> 'f'; case OFFENSE -> 'c'; case DEFENSE -> '9'; case UTILITY -> 'a';
            case ELEMENTAL_A -> '6'; case ELEMENTAL_B -> 'b'; case HYBRID -> 'e';
            case SPECIALIST -> 'd'; case FORBIDDEN -> '4'; case ASCENDED -> 'e';
            case SYNERGY_AD -> '5'; case SYNERGY_AU -> '2'; case SYNERGY_DU -> '3';
        };
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
}
