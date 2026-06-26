package com.gugas749.abyssorigins.client.gui.skilltree;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.packet.UnlockSkillPacket;
import com.gugas749.abyssorigins.network.packet.UnlockSpecSkillPacket;
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

    // Tooltip description max width in pixels — constrains text wrapping
    private static final int TOOLTIP_DESC_MAX_PX = 180;

    private final Screen parent;

    // ---- State -----------------------------------------------------------
    private int activeTab = 0; // 0 = origin, 1 = spec (only exists if player has spec)

    private SkillTree        originTree;
    private PlayerSkillData  originSkills;
    private PlayerOriginData originData;
    private Origin           origin;

    private SkillTree                specTree;
    private PlayerSpecializationData specData;
    private Specialization           spec;
    private boolean                  hasSpec = false;

    private float   zoom = 1.0f;
    private float   panX = 0, panY = 0;
    private boolean isDragging = false;
    private double  lastMouseX, lastMouseY;
    private SkillNode hoveredNode = null;
    private final Set<String> excludedSkills = new HashSet<>();

    // Tab button hit areas (only used when hasSpec)
    private static final int TAB_W = 22, TAB_H = 22, TAB_GAP = 4;

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

        origin      = AbyssOriginsAPI.getPlayerOrigin(player);
        originSkills= player.getData(SkillAttachments.PLAYER_SKILLS.get());
        originData  = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        if (origin == null) {
            player.displayClientMessage(
                Component.literal("\u00a7eYou do not have an origin assigned."), false);
            onClose(); return;
        }

        originTree = SkillTreeRegistry.getTree(origin.getId()).orElse(null);
        if (originTree == null) {
            player.displayClientMessage(
                Component.literal("\u00a7eNo skill tree available for this origin yet."), false);
            onClose(); return;
        }

        // Spec — only load if assigned
        spec     = ClientSpecializationData.getSpec();
        specData = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        specTree = spec != null ? spec.getSkillTree() : null;
        hasSpec  = spec != null && specTree != null;

        // Keep activeTab valid if spec was removed
        if (!hasSpec) activeTab = 0;

        calculateExcludedSkills();
        panX = this.width / 2f;
        panY = this.height / 2f - 50;

        // Controls — no tab buttons registered here; tabs are drawn manually
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(this.width / 2 - 40, this.height - 25, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
            b -> zoom = Math.min(2.5f, zoom + 0.1f)).bounds(this.width - 30, 50, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"),
            b -> zoom = Math.max(0.4f, zoom - 0.1f)).bounds(this.width - 30, 75, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("R"), b -> {
            zoom = 1.0f; panX = this.width / 2f; panY = this.height / 2f - 50;
        }).bounds(this.width - 30, 100, 20, 20).build());
    }

    private void calculateExcludedSkills() {
        excludedSkills.clear();
        if (activeTab == 0 && originTree != null) {
            for (String id : originSkills.getUnlockedSkills()) {
                SkillNode n = originTree.getSkill(id);
                if (n != null) excludedSkills.addAll(n.getExclusions());
            }
        } else if (activeTab == 1 && specTree != null && specData != null) {
            for (String id : specData.getUnlockedSkills()) {
                SkillNode n = specTree.getSkill(id);
                if (n != null) excludedSkills.addAll(n.getExclusions());
            }
        }
    }

    // ---- Active-tab helpers ---------------------------------------------

    private SkillTree activeTree()       { return activeTab == 0 ? originTree : specTree; }
    private boolean hasSkill(String id)  { return activeTab == 0 ? originSkills.hasSkill(id) : (specData != null && specData.hasSkill(id)); }
    private Set<String> unlockedSet()    { return activeTab == 0 ? originSkills.getUnlockedSkillsSet() : (specData != null ? specData.getUnlockedSkillsSet() : Collections.emptySet()); }
    private int playerLevel()            { return activeTab == 0 ? originData.getOriginLevel() : (specData != null ? specData.getSpecLevel() : 0); }
    private int skillPoints()            { return activeTab == 0 ? originSkills.getSkillPoints() : (specData != null ? specData.getSkillPoints() : 0); }
    private int themeColor()             { return activeTab == 0 ? 0xFF445588 : (spec != null ? spec.getThemeColor() : 0xFF9922CC); }

    // ---- Tab rendering (manual — node-card style) -----------------------

    /** Returns the X of the first tab button (top-right area). */
    private int tabX(int index) {
        // Tabs sit at top-right, just left of the +/-/R controls
        return this.width - 60 - (TAB_W + TAB_GAP) * (2 - index);
    }
    private static final int TAB_Y = 4;

    private void drawTabs(GuiGraphics gfx, int mouseX, int mouseY) {
        if (!hasSpec) return; // no tabs if no specialization

        String[] icons  = { "\u2694", "\u2605" }; // sword for origin, star for spec
        String[] labels = { "O", "S" };           // compact single-char labels

        for (int i = 0; i < 2; i++) {
            int tx = tabX(i), ty = TAB_Y;
            boolean isActive = (activeTab == i);
            boolean hovered  = mouseX >= tx && mouseX <= tx + TAB_W
                             && mouseY >= ty && mouseY <= ty + TAB_H;

            // Background — matches locked/available node card style
            int bg     = isActive ? 0xFF1A2A1A : (hovered ? 0xFF252530 : 0xFF141420);
            int border = isActive ? themeColor() : (hovered ? 0xFF555566 : 0xFF2A2A3A);

            gfx.fill(tx, ty, tx + TAB_W, ty + TAB_H, bg);
            // 1px border
            gfx.fill(tx,          ty,          tx + TAB_W, ty + 1,          border);
            gfx.fill(tx,          ty + TAB_H-1,tx + TAB_W, ty + TAB_H,      border);
            gfx.fill(tx,          ty,          tx + 1,     ty + TAB_H,       border);
            gfx.fill(tx + TAB_W-1,ty,          tx + TAB_W, ty + TAB_H,       border);

            // Active tab gets a bottom highlight line in theme color
            if (isActive) gfx.fill(tx + 1, ty + TAB_H - 2, tx + TAB_W - 1, ty + TAB_H - 1, themeColor());

            // Icon
            String icon = icons[i];
            int iconColor = isActive ? 0xFFFFFFFF : (hovered ? 0xFFCCCCCC : 0xFF888899);
            gfx.drawString(font, icon, tx + (TAB_W - font.width(icon)) / 2,
                ty + (TAB_H - 8) / 2, iconColor, true);

            // Tooltip on hover
            if (hovered) {
                String tip = i == 0
                    ? (originTree != null ? originTree.getDisplayName().getString() : "Origin Skills")
                    : (specTree  != null ? specTree.getDisplayName().getString()  : "Spec Skills");
                int tw = font.width(tip) + 8;
                int ttx = tx - tw - 2, tty = ty + TAB_H + 2;
                if (ttx < 0) ttx = tx;
                gfx.fill(ttx - 2, tty - 2, ttx + tw + 2, tty + 10, 0xEE0A0A14);
                gfx.fill(ttx - 2, tty - 2, ttx + tw + 2, tty - 1,   border);
                gfx.drawString(font, tip, ttx + 4, tty, 0xFFCCCCCC, false);
            }
        }
    }

    // ---- Render ---------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xDD101020);

        if (activeTree() == null) {
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        drawGridBackground(gfx);
        drawConnections(gfx);

        hoveredNode = null;
        for (SkillNode node : activeTree().getAllSkills())
            drawSkillNode(gfx, node, mouseX, mouseY);

        drawUIOverlay(gfx);
        drawTabs(gfx, mouseX, mouseY);

        if (hoveredNode != null) drawTooltip(gfx, hoveredNode, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawGridBackground(GuiGraphics gfx) {
        int gs = (int)(50 * zoom);
        int ox = (int)(panX % gs), oy = (int)(panY % gs);
        for (int x = ox; x < this.width;  x += gs) gfx.fill(x, 0, x+1, this.height, 0x20FFFFFF);
        for (int y = oy; y < this.height; y += gs) gfx.fill(0, y, this.width, y+1,   0x20FFFFFF);
    }

    private void drawConnections(GuiGraphics gfx) {
        for (SkillTree.SkillConnection conn : activeTree().getConnections()) {
            SkillNode from = activeTree().getSkill(conn.getFromSkillId());
            SkillNode to   = activeTree().getSkill(conn.getToSkillId());
            if (from == null || to == null) continue;
            int[] fp = sp(from.getGridX(), from.getGridY());
            int[] tp = sp(to.getGridX(),   to.getGridY());
            int color;
            if (hasSkill(to.getId()))                  color = 0xFF55FF55;
            else if (excludedSkills.contains(to.getId())) color = 0xFF553333;
            else if (hasSkill(from.getId()))            color = 0xFFFFFF55;
            else                                        color = 0xFF444444;
            int t = Math.max(1, (int)(2*zoom)), my = (fp[1]+tp[1])/2;
            gfx.fill(fp[0]-t/2, fp[1], fp[0]+t/2+1, my, color);
            gfx.fill(Math.min(fp[0],tp[0])-t/2, my-t/2, Math.max(fp[0],tp[0])+t/2+1, my+t/2+1, color);
            gfx.fill(tp[0]-t/2, my, tp[0]+t/2+1, tp[1], color);
        }
    }

    private void drawSkillNode(GuiGraphics gfx, SkillNode node, int mx, int my) {
        int[] p = sp(node.getGridX(), node.getGridY());
        int size = (int)(NODE_SIZE * zoom), half = size/2;
        int left = p[0]-half, top = p[1]-half, right = p[0]+half, bottom = p[1]+half;
        if (right<0||left>width||bottom<0||top>height) return;

        boolean unlocked  = hasSkill(node.getId());
        boolean excluded  = excludedSkills.contains(node.getId());
        boolean canUnlock = node.canUnlock(unlockedSet(), excludedSkills, playerLevel());
        boolean hasPoints = skillPoints() >= node.getPointCost();
        boolean hovered   = mx>=left&&mx<=right&&my>=top&&my<=bottom;
        if (hovered) hoveredNode = node;

        int bg, border;
        if (unlocked)              { bg=0xFF225522; border=0xFF55FF55; }
        else if (excluded)         { bg=0xFF331111; border=0xFF662222; }
        else if (canUnlock&&hasPoints) { bg=0xFF333322; border=0xFFFFFF55; }
        else if (canUnlock)        { bg=0xFF332222; border=0xFFFF8855; }
        else                       { bg=0xFF222222; border=0xFF444444; }
        if (hovered&&!excluded)    { bg=brighten(bg,30); border=0xFFFFFFFF; }

        int bw = Math.max(2,(int)(3*zoom));
        gfx.fill(left-bw,top-bw,right+bw,bottom+bw,border);
        gfx.fill(left,top,right,bottom,bg);
        int is = Math.max(4,(int)(8*zoom));
        gfx.fill(right-is-2,top+2,right-2,top+is+2,node.getBranchColor());
        gfx.fill(left+2,top+2,left+is+2,top+is+2,node.getTierColor());

        if (zoom >= 0.7f) {
            int tc = unlocked?0xFFFFFFFF:(excluded?0xFF666666:(canUnlock?0xFFEEEEEE:0xFFAAAAAA));
            String icon = node.getIcon();
            gfx.drawString(font, icon, p[0]-font.width(icon)/2, p[1]-8, tc, true);
            String name = node.getDisplayName().getString();
            if (font.width(name) > size-4) name = name.substring(0, Math.min(7,name.length()))+"..";
            gfx.drawString(font, name, p[0]-font.width(name)/2, p[1]+2, tc, true);
            if (!unlocked) {
                String cost = String.valueOf(node.getPointCost());
                gfx.drawString(font, cost, p[0]-font.width(cost)/2, bottom-10, hasPoints?0xFFFFFF00:0xFFFF4444, true);
            } else {
                gfx.drawString(font, "\u00a7a\u2713", p[0]-font.width("\u00a7a\u2713")/2, bottom-10, 0xFFFFFF, true);
            }
        }
    }

    private void drawUIOverlay(GuiGraphics gfx) {
        int cx = this.width/2;
        gfx.fill(0, 0, this.width, 45, 0xCC000000);
        gfx.fill(0, 44, this.width, 45, themeColor());

        String title = activeTree().getDisplayName().getString() + " Skill Tree";
        gfx.drawString(font, title, cx-font.width(title)/2, 8, 0xFFFFFFFF, true);

        String info = "Level " + playerLevel() + "  |  Skill Points: " + skillPoints();
        gfx.drawString(font, info, cx-font.width(info)/2, 22, 0xFF55FF55, true);

        String zoomStr = String.format("Zoom: %.0f%%", zoom*100);
        gfx.drawString(font, zoomStr, this.width-70, 35, 0xFFAAAAAA, true);

        String instructions = "Drag to pan  |  Scroll to zoom  |  Click to unlock";
        gfx.drawString(font, instructions, cx-font.width(instructions)/2, this.height-40, 0xFF888888, true);

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
        gfx.fill(x, y, x+8, y+8, color);
        gfx.drawString(font, text, x+12, y, 0xFFCCCCCC, true);
    }

    private void drawTooltip(GuiGraphics gfx, SkillNode node, int mouseX, int mouseY) {
        boolean unlocked  = hasSkill(node.getId());
        boolean excluded  = excludedSkills.contains(node.getId());
        boolean canUnlock = node.canUnlock(unlockedSet(), excludedSkills, playerLevel());
        boolean hasPoints = skillPoints() >= node.getPointCost();

        List<String> lines = new ArrayList<>();
        String pfx = unlocked?"\u00a7a":(excluded?"\u00a74\u00a7m":(canUnlock?"\u00a7e":"\u00a77"));
        lines.add(pfx+"\u00a7l"+node.getDisplayName().getString());
        lines.add("\u00a75Tier "+node.getTier().getTier()+" \u00a77| \u00a7"
            +branchColorCode(node.getBranch())+node.getBranch().name());
        lines.add("");

        // FIX: wrap description text so it stays inside the tooltip card
        for (String descLine : wrapTooltipText(node.getDescription().getString())) {
            lines.add("\u00a7f" + descLine);
        }

        lines.add("");
        lines.add("\u00a76Requirements:");
        boolean lvlOk = playerLevel() >= node.getTier().getLevelRequired();
        lines.add((lvlOk?"\u00a7a":"\u00a7c")+" Level "+node.getTier().getLevelRequired()+(lvlOk?" \u2713":" \u2717"));
        lines.add((hasPoints||unlocked?"\u00a7a":"\u00a7c")+" Cost: "+node.getPointCost()+" SP"+(hasPoints||unlocked?" \u2713":" \u2717"));

        for (String req : node.getPrerequisites()) {
            SkillNode r = activeTree().getSkill(req);
            if (r!=null) {
                boolean met = hasSkill(req);
                lines.add((met?"\u00a7a":"\u00a7c")+" Requires: "+r.getDisplayName().getString()+(met?" \u2713":" \u2717"));
            }
        }

        if (!node.getExclusions().isEmpty() && !unlocked) {
            lines.add(""); lines.add("\u00a7c\u00a7lWARNING: Locks out:");
            for (String e : node.getExclusions()) {
                SkillNode en = activeTree().getSkill(e);
                if (en!=null) lines.add("\u00a7c \u2717 "+en.getDisplayName().getString());
            }
        }

        lines.add("");
        if (unlocked)            lines.add("\u00a7a\u00a7l\u2713 UNLOCKED");
        else if (excluded)       { lines.add("\u00a74\u00a7l\u2717 LOCKED OUT"); lines.add("\u00a77(Another skill excludes this)"); }
        else if (canUnlock&&hasPoints) lines.add("\u00a7e\u00a7lClick to unlock!");
        else if (canUnlock)      lines.add("\u00a7cNeed more skill points");
        else                     lines.add("\u00a77Requirements not met");

        int maxW = lines.stream()
            .mapToInt(l -> font.width(l.replaceAll("\u00a7.", "")))
            .max().orElse(0);
        maxW = Math.min(maxW + 16, TOOLTIP_DESC_MAX_PX + 20);
        int th = lines.size()*10+8;

        int tx = mouseX+15, ty = mouseY-10;
        if (tx+maxW > this.width-10)  tx = mouseX-maxW-15;
        if (ty+th   > this.height-10) ty = this.height-th-10;
        if (ty < 50) ty = 50;

        gfx.fill(tx-4,ty-4,tx+maxW+4,ty+th+4, 0xF0100010);
        gfx.fill(tx-3,ty-3,tx+maxW+3,ty+th+3, 0xFF5000AA);
        gfx.fill(tx-2,ty-2,tx+maxW+2,ty+th+2, 0xF0100010);
        int ty2 = ty;
        for (String l : lines) { gfx.drawString(font, l, tx, ty2, 0xFFFFFFFF, true); ty2+=10; }
    }

    /**
     * Wraps a description string so each line fits within TOOLTIP_DESC_MAX_PX pixels.
     * Strips formatting codes before measuring, keeps them intact in output.
     */
    private List<String> wrapTooltipText(String text) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            String plain = test.replaceAll("\u00a7.", "");
            if (font.width(plain) > TOOLTIP_DESC_MAX_PX && cur.length() > 0) {
                result.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                if (cur.length() > 0) cur.append(' ');
                cur.append(word);
            }
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result.isEmpty() ? List.of(text) : result;
    }

    // ---- Input ----------------------------------------------------------

    private boolean wasMousePressed = false;

    @Override
    public void tick() {
        super.tick();
        long win = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(win,
            org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        double mx = minecraft.mouseHandler.xpos()*width /minecraft.getWindow().getWidth();
        double my = minecraft.mouseHandler.ypos()*height/minecraft.getWindow().getHeight();

        if (pressed && !wasMousePressed) {
            // Tab click check
            if (hasSpec) {
                for (int i = 0; i < 2; i++) {
                    int tx = tabX(i);
                    if (mx>=tx && mx<=tx+TAB_W && my>=TAB_Y && my<=TAB_Y+TAB_H) {
                        if (activeTab != i) {
                            activeTab = i;
                            panX = this.width / 2f;
                            panY = this.height / 2f - 50;
                            zoom = 1.0f;
                            calculateExcludedSkills();
                        }
                        wasMousePressed = true;
                        return;
                    }
                }
            }
            if (hoveredNode != null) tryUnlock(hoveredNode);
            else { isDragging = true; lastMouseX = mx; lastMouseY = my; }
        }

        if (pressed && isDragging) {
            panX += (float)(mx-lastMouseX); panY += (float)(my-lastMouseY);
            lastMouseX = mx; lastMouseY = my;
        }
        if (!pressed) isDragging = false;
        wasMousePressed = pressed;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        float oz = zoom;
        zoom = Math.max(0.4f, Math.min(2.5f, zoom + (float)(sy*0.1)));
        float f = zoom/oz;
        panX = (float)(mx-(mx-panX)*f);
        panY = (float)(my-(my-panY)*f);
        return true;
    }

    private void tryUnlock(SkillNode node) {
        if (hasSkill(node.getId())) return;
        if (excludedSkills.contains(node.getId())) return;
        if (!node.canUnlock(unlockedSet(), excludedSkills, playerLevel())) return;
        if (skillPoints() < node.getPointCost()) return;

        if (activeTab == 0) {
            PacketDistributor.sendToServer(new UnlockSkillPacket(node.getId()));
            originSkills.unlockSkill(node.getId());
            originSkills.spendSkillPoints(node.getPointCost());
        } else {
            PacketDistributor.sendToServer(new UnlockSpecSkillPacket(node.getId()));
            specData.unlockSkill(node.getId());
            specData.spendSkillPoints(node.getPointCost());
        }
        calculateExcludedSkills();
    }

    // ---- Helpers --------------------------------------------------------

    private int[] sp(int gx, int gy) {
        return new int[]{ (int)(panX+gx*GRID_SPACING*zoom), (int)(panY+gy*GRID_SPACING*zoom) };
    }

    private static int brighten(int c, int a) {
        return (((c>>24)&0xFF)<<24)|(Math.min(255,((c>>16)&0xFF)+a)<<16)
              |(Math.min(255,((c>>8)&0xFF)+a)<<8)|Math.min(255,(c&0xFF)+a);
    }

    private static char branchColorCode(SkillNode.SkillBranch b) {
        return switch(b) {
            case CORE->'f'; case OFFENSE->'c'; case DEFENSE->'9'; case UTILITY->'a';
            case ELEMENTAL_A->'6'; case ELEMENTAL_B->'b'; case HYBRID->'e';
            case SPECIALIST->'d'; case FORBIDDEN->'4'; case ASCENDED->'e';
            case SYNERGY_AD->'5'; case SYNERGY_AU->'2'; case SYNERGY_DU->'3';
        };
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
}
