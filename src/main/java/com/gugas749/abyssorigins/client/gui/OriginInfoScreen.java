package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.ImpactLevel;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import com.gugas749.abyssorigins.network.packet.ActivateAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Comprehensive Origin & Ability Dashboard.
 * Ported from VeilOrigins' AbilityBarScreen, adapted for AbyssOrigins API.
 */
public class OriginInfoScreen extends Screen {

    private static final int PANEL_PADDING = 12;
    private static final int SECTION_GAP   = 8;

    private static final int COLOR_BG           = 0xEE0a0a12;
    private static final int COLOR_PANEL        = 0xDD12121f;
    private static final int COLOR_PANEL_BORDER = 0xFF2a2a45;
    private static final int COLOR_PANEL_HEADER = 0xFF1a1a2a;
    private static final int COLOR_TEXT         = 0xFFe8e8f0;
    private static final int COLOR_TEXT_DIM     = 0xFF888899;
    private static final int COLOR_TEXT_BRIGHT  = 0xFFFFFFFF;
    private static final int COLOR_POSITIVE     = 0xFF55dd55;
    private static final int COLOR_NEGATIVE     = 0xFFdd5555;
    private static final int COLOR_WARNING      = 0xFFddaa55;
    private static final int COLOR_XP           = 0xFF55ddaa;
    private static final int COLOR_COOLDOWN     = 0xFFff6666;
    private static final int COLOR_READY        = 0xFF66ff66;

    private Origin          playerOrigin;
    private List<OriginAbility> abilities;
    private List<OriginPassive> passives;
    private int hoveredAbility = -1;
    private float animProgress = 0;
    private int originColor;
    private String resourceName;

    public OriginInfoScreen() {
        super(Component.translatable("gui.abyssorigins.origin_info"));
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { onClose(); return; }

        playerOrigin = ClientOriginData.getOrigin();
        if (playerOrigin == null) {
            // Fallback to API cache
            playerOrigin = com.gugas749.abyssorigins.api.AbyssOriginsAPI.getPlayerOrigin(mc.player);
        }
        if (playerOrigin == null) { onClose(); return; }

        abilities    = playerOrigin.getAbilities();
        passives     = playerOrigin.getPassives();
        originColor  = getOriginThemeColor();
        resourceName = getResourceName();
        animProgress = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (animProgress < 1.0f) animProgress = Math.min(1.0f, animProgress + 0.08f);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mx, int my, float pt) {
        // custom — no blur
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
        if (playerOrigin == null) return;

        gfx.fill(0, 0, width, height, COLOR_BG);

        int contentWidth = Math.min(width - 40, 900);
        int contentX     = (width - contentWidth) / 2;
        int contentY     = 25 + (int)((1f - animProgress) * 30);

        int leftW  = (int)(contentWidth * 0.35f) - SECTION_GAP;
        int leftX  = contentX;
        int centW  = (int)(contentWidth * 0.40f) - SECTION_GAP;
        int centX  = leftX + leftW + SECTION_GAP;
        int rightW = contentWidth - leftW - centW - SECTION_GAP * 2;
        int rightX = centX + centW + SECTION_GAP;

        hoveredAbility = -1;

        renderOriginPanel    (gfx, leftX,  contentY,       leftW);
        renderStatsPanel     (gfx, leftX,  contentY + 195, leftW);
        renderAbilitiesPanel (gfx, centX,  contentY,       centW, mouseX, mouseY);
        renderPassivesPanel  (gfx, rightX, contentY,       rightW);
        renderProgressionPanel(gfx, rightX, contentY + 180, rightW);
        renderTitleBar       (gfx);
        renderInstructions   (gfx);

        super.render(gfx, mouseX, mouseY, pt);
    }

    // ---- Panels -------------------------------------------------------

    private void renderTitleBar(GuiGraphics gfx) {
        gfx.fill(0, 0, width, 22, 0xDD000008);
        gfx.fill(0, 21, width, 22, originColor);
        String title = "\u2694 " + playerOrigin.getDisplayName().getString() + " Dashboard";
        gfx.drawString(font, title, (width - font.width(title)) / 2, 7, COLOR_TEXT_BRIGHT, true);
        gfx.drawString(font, "[ESC] Close", 10, 7, COLOR_TEXT_DIM, false);
    }

    private void renderOriginPanel(GuiGraphics gfx, int x, int y, int w) {
        drawPanel(gfx, x, y, w, 185, "ORIGIN INFO");
        int ix = x + PANEL_PADDING, iy = y + 28, iw = w - PANEL_PADDING * 2;

        gfx.fill(ix, iy, ix + 4, iy + 16, originColor);
        gfx.drawString(font, playerOrigin.getDisplayName().getString(), ix + 10, iy + 4, COLOR_TEXT_BRIGHT, true);
        iy += 22;

        gfx.drawString(font, "Impact: " + getImpactStars(playerOrigin.getImpactLevel()),
            ix, iy, getImpactColor(playerOrigin.getImpactLevel()), false);
        iy += 14;

        gfx.fill(ix, iy, ix + iw, iy + 1, COLOR_PANEL_BORDER); iy += 8;

        for (String line : wrapText(playerOrigin.getDescription().getString(), iw, 4)) {
            gfx.drawString(font, line, ix, iy, COLOR_TEXT_DIM, false); iy += 10;
        }
        iy += 6;

        gfx.fill(ix, iy, ix + iw, iy + 1, COLOR_PANEL_BORDER); iy += 8;
        gfx.drawString(font, resourceName, ix, iy, originColor, false); iy += 12;

        float resource = ClientOriginData.getResourceBar();
        drawResourceBar(gfx, ix, iy, iw, 12, resource, originColor); iy += 18;

        String rt = String.format("%.0f / 100", resource);
        gfx.drawString(font, rt, ix + (iw - font.width(rt)) / 2, iy, COLOR_TEXT, false);
    }

    private void renderStatsPanel(GuiGraphics gfx, int x, int y, int w) {
        int h = Math.min(height - y - 50, 150);
        drawPanel(gfx, x, y, w, h, "MODIFIERS");
        int ix = x + PANEL_PADDING, iy = y + 28, iw = w - PANEL_PADDING * 2;

        renderStatRow(gfx, ix, iy, iw, "\u2665 Health", playerOrigin.getHealthModifier()); iy += 18;
        renderStatRow(gfx, ix, iy, iw, "\u26A1 Speed",  playerOrigin.getSpeedModifier());  iy += 18;
        renderStatRow(gfx, ix, iy, iw, "\u2694 Damage", playerOrigin.getDamageModifier()); iy += 22;

        gfx.fill(ix, iy, ix + iw, iy + 1, COLOR_PANEL_BORDER); iy += 8;
        gfx.drawString(font, "Abilities: " + abilities.size(), ix, iy, COLOR_TEXT_DIM, false); iy += 12;
        gfx.drawString(font, "Passives:  " + passives.size(),  ix, iy, COLOR_TEXT_DIM, false);
    }

    private void renderStatRow(GuiGraphics gfx, int x, int y, int w, String label, float value) {
        gfx.drawString(font, label, x, y, COLOR_TEXT, false);
        String vs; int vc;
        if      (value > 1f) { vs = String.format("+%.0f%%", (value-1)*100); vc = COLOR_POSITIVE; }
        else if (value < 1f) { vs = String.format("%.0f%%",  (value-1)*100); vc = COLOR_NEGATIVE; }
        else                 { vs = "\u00b10%";                               vc = COLOR_TEXT_DIM; }
        gfx.drawString(font, vs, x + w - font.width(vs), y, vc, false);
    }

    private void renderAbilitiesPanel(GuiGraphics gfx, int x, int y, int w, int mouseX, int mouseY) {
        int h = height - y - 50;
        drawPanel(gfx, x, y, w, h, "ABILITIES (" + abilities.size() + ")");
        int ix = x + PANEL_PADDING, iy = y + 28, iw = w - PANEL_PADDING * 2;

        if (abilities.isEmpty()) {
            gfx.drawString(font, "No abilities available", ix, iy, COLOR_TEXT_DIM, false);
            return;
        }

        int slotH = 58, slotGap = 6;
        for (int i = 0; i < abilities.size(); i++) {
            int sy = iy + i * (slotH + slotGap);
            if (sy + slotH > y + h - 10) break;
            boolean hovered = mouseX >= ix && mouseX <= ix + iw && mouseY >= sy && mouseY <= sy + slotH;
            if (hovered) hoveredAbility = i;
            renderAbilitySlot(gfx, abilities.get(i), i, ix, sy, iw, slotH, hovered);
        }
    }

    private void renderAbilitySlot(GuiGraphics gfx, OriginAbility ability, int index, int x, int y, int w, int h, boolean hovered) {
        int cd    = ClientOriginData.getAbilityCooldown(index);
        int maxCd = ClientOriginData.getAbilityMaxCooldown(index);
        boolean onCd = cd > 0;

        gfx.fill(x, y, x + w, y + h, hovered ? 0xFF1f1f35 : 0xFF15152a);
        gfx.fill(x, y, x + 3, y + h, onCd ? COLOR_COOLDOWN : COLOR_READY);
        drawBorder(gfx, x, y, w, h, hovered ? originColor : COLOR_PANEL_BORDER, 1);

        if (onCd && maxCd > 0) {
            int ow = (int)(w * ((float)cd / maxCd));
            gfx.fill(x + w - ow, y + 1, x + w - 1, y + h - 1, 0x44000000);
        }

        int cx = x + 10, cy = y + 6;
        gfx.drawString(font, getAbilityIcon(ability.getId()), cx, cy, COLOR_TEXT_BRIGHT, true);

        String kb = "[" + getKeybindHint(index) + "]";
        int kbx = cx + 20;
        gfx.fill(kbx, cy - 1, kbx + font.width(kb) + 4, cy + 10, 0xAA000000);
        gfx.drawString(font, kb, kbx + 2, cy, 0xFFffff88, false);

        gfx.drawString(font, ability.getDisplayName().getString(), kbx + font.width(kb) + 10, cy, COLOR_TEXT_BRIGHT, false);
        cy += 14;

        String desc = ability.getDescription().getString();
        if (desc.length() > 45) desc = desc.substring(0, 42) + "...";
        gfx.drawString(font, desc, cx, cy, COLOR_TEXT_DIM, false);
        cy += 14;

        gfx.drawString(font, "Cost: " + ability.getResourceCost(), cx, cy,
            ability.getResourceCost() > 0 ? originColor : COLOR_TEXT_DIM, false);
        gfx.drawString(font, "CD: " + (ability.getBaseCooldownTicks() / 20) + "s", cx + 70, cy, COLOR_TEXT_DIM, false);

        String status = onCd ? formatCooldown(cd) : "READY";
        int sc = onCd ? COLOR_COOLDOWN : COLOR_READY;
        gfx.drawString(font, status, x + w - font.width(status) - 8, cy, sc, true);
    }

    private void renderPassivesPanel(GuiGraphics gfx, int x, int y, int w) {
        drawPanel(gfx, x, y, w, 170, "PASSIVES (" + passives.size() + ")");
        int ix = x + PANEL_PADDING, iy = y + 28, iw = w - PANEL_PADDING * 2;

        if (passives.isEmpty()) {
            gfx.drawString(font, "No passives", ix, iy, COLOR_TEXT_DIM, false);
            return;
        }

        for (int i = 0; i < Math.min(passives.size(), 6); i++) {
            OriginPassive p = passives.get(i);
            // Skip internal passives (bat form timer etc.)
            if (p.getId().equals("vampire_bat_form_tick")) continue;

            gfx.fill(ix, iy, ix + iw, iy + 18, 0x44000000);
            String icon = getPassiveIcon(p.getId());
            gfx.drawString(font, icon + " " + p.getDisplayName().getString(), ix + 4, iy + 5, COLOR_TEXT, false);

            iy += 22;
        }

        int extra = (int) passives.stream().filter(p -> !p.getId().equals("vampire_bat_form_tick")).count() - 6;

        if (extra > 0)
            gfx.drawString(font, "+" + extra + " more...", ix, iy, COLOR_TEXT_DIM, false);
    }

    private void renderProgressionPanel(GuiGraphics gfx, int x, int y, int w) {
        int h = Math.min(height - y - 50, 165);
        drawPanel(gfx, x, y, w, h, "PROGRESSION");
        int ix = x + PANEL_PADDING, iy = y + 28, iw = w - PANEL_PADDING * 2;

        int   level       = ClientOriginData.getLevel();
        int   xp          = ClientOriginData.getXp();
        int   skillPoints = ClientOriginData.getSkillPoints();
        float xpProg      = ClientOriginData.getXpProgress();
        int   xpNeeded    = ClientOriginData.getXpForNextLevel();

        String levelStr = "LVL " + level;
        gfx.drawString(font, levelStr, ix, iy, 0xFFffcc00, true); iy += 16;

        gfx.drawString(font, "Experience", ix, iy, COLOR_TEXT_DIM, false); iy += 10;
        drawProgressBar(gfx, ix, iy, iw, 10, xpProg, COLOR_XP); iy += 14;

        String xpText = (xp % 100) + " / " + xpNeeded + " XP";
        gfx.drawString(font, xpText, ix, iy, COLOR_TEXT_DIM, false); iy += 18;

        gfx.fill(ix, iy, ix + iw, iy + 1, COLOR_PANEL_BORDER); iy += 8;
        gfx.drawString(font, "Skill Points", ix, iy, COLOR_TEXT_DIM, false); iy += 12;

        String spStr = String.valueOf(skillPoints);
        int spColor = skillPoints > 0 ? COLOR_POSITIVE : COLOR_TEXT_DIM;
        gfx.drawString(font, spStr, ix, iy, spColor, true);
        if (skillPoints > 0)
            gfx.drawString(font, " available", ix + font.width(spStr), iy, COLOR_TEXT_DIM, false);
        iy += 18;

        int unlocked = ClientOriginData.getUnlockedSkills().size();
        gfx.drawString(font, "Skills: " + unlocked + " unlocked", ix, iy, COLOR_TEXT_DIM, false);
    }

    private void renderInstructions(GuiGraphics gfx) {
        String s = "Click ability or press keybind to activate  \u2022  [K] Skill Tree  \u2022  [ESC] Close";
        gfx.fill(0, height - 25, width, height, 0xCC000008);
        gfx.drawString(font, s, (width - font.width(s)) / 2, height - 17, COLOR_TEXT_DIM, false);
    }

    // ---- Drawing helpers -----------------------------------------------

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h, String title) {
        gfx.fill(x, y, x + w, y + h, COLOR_PANEL);
        gfx.fill(x, y, x + w, y + 22, COLOR_PANEL_HEADER);
        gfx.fill(x, y + 21, x + w, y + 22, originColor);
        gfx.drawString(font, title, x + 8, y + 7, COLOR_TEXT, false);
        drawBorder(gfx, x, y, w, h, COLOR_PANEL_BORDER, 1);
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color, int t) {
        gfx.fill(x, y, x + w, y + t, color);
        gfx.fill(x, y + h - t, x + w, y + h, color);
        gfx.fill(x, y, x + t, y + h, color);
        gfx.fill(x + w - t, y, x + w, y + h, color);
    }

    private void drawResourceBar(GuiGraphics gfx, int x, int y, int w, int h, float value, int color) {
        gfx.fill(x, y, x + w, y + h, 0xFF111118);
        int fw = (int)(w * (value / 100f));
        if (fw > 0) {
            gfx.fill(x, y, x + fw, y + h, color);
            gfx.fill(x, y, x + fw, y + 2, brighten(color, 40));
        }
        drawBorder(gfx, x, y, w, h, 0xFF333344, 1);
    }

    private void drawProgressBar(GuiGraphics gfx, int x, int y, int w, int h, float progress, int color) {
        gfx.fill(x, y, x + w, y + h, 0xFF111118);
        int fw = (int)(w * progress);
        if (fw > 0) gfx.fill(x, y, x + fw, y + h, color);
        drawBorder(gfx, x, y, w, h, 0xFF333344, 1);
    }

    // ---- Input ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hoveredAbility >= 0 && hoveredAbility < abilities.size()) {
            activateAbility(hoveredAbility);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scan, int mods) {
        if (keyCode == 82 && !abilities.isEmpty()) { activateAbility(0); return true; } // R
        if (keyCode == 86 && abilities.size() > 1) { activateAbility(1); return true; } // V
        if (keyCode == 75) { // K — open skill tree
            Minecraft.getInstance().setScreen(new SkillTreeScreen(this));
            return true;
        }
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scan, mods);
    }

    private void activateAbility(int index) {
        if (index < 0 || index >= abilities.size()) return;
        int cd = ClientOriginData.getAbilityCooldown(index);
        if (cd > 0) {
            Minecraft.getInstance().player.displayClientMessage(
                Component.literal("Ability on cooldown: " + formatCooldown(cd))
                    .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }
        PacketDistributor.sendToServer(new ActivateAbilityPacket(index));
        onClose();
    }

    // ---- Helpers -------------------------------------------------------

    private int brighten(int color, int amt) {
        int a = (color>>24)&0xFF, r = Math.min(255,((color>>16)&0xFF)+amt),
            g = Math.min(255,((color>>8)&0xFF)+amt), b = Math.min(255,(color&0xFF)+amt);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private List<String> wrapText(String text, int maxPx, int maxLines) {
        List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (font.width(test) <= maxPx) { if (cur.length()>0) cur.append(' '); cur.append(w); }
            else { if (cur.length()>0) lines.add(cur.toString()); cur = new StringBuilder(w); }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.size() > maxLines) {
            lines = lines.subList(0, maxLines);
            lines.set(maxLines - 1, lines.get(maxLines - 1) + "...");
        }
        return lines;
    }

    private String getImpactStars(ImpactLevel level) {
        return switch (level) { case LOW -> "\u2605\u2606\u2606"; case MEDIUM -> "\u2605\u2605\u2606"; case HIGH -> "\u2605\u2605\u2605"; };
    }

    private int getImpactColor(ImpactLevel level) {
        return switch (level) { case LOW -> COLOR_POSITIVE; case MEDIUM -> COLOR_WARNING; case HIGH -> COLOR_NEGATIVE; };
    }

    private int getOriginThemeColor() {
        if (playerOrigin == null) return 0xFF5566aa;
        String id = playerOrigin.getId().toString().toLowerCase();
        if (id.contains("vampire") || id.contains("blood")) return 0xFFcc3333;
        if (id.contains("human"))                           return 0xFFffaa44;
        if (id.contains("wolf")   || id.contains("were"))  return 0xFFbb8844;
        if (id.contains("necro"))                           return 0xFF7788aa;
        if (id.contains("shadow") || id.contains("umbra")) return 0xFF9955cc;
        return 0xFFffaa44;
    }

    private String getResourceName() {
        if (playerOrigin == null) return "Resource";
        if (playerOrigin.getResourceType() != null)
            return playerOrigin.getResourceType().getDisplayName().getString();
        return "None";
    }

    private String getAbilityIcon(String id) {
        id = id.toLowerCase();
        if (id.contains("blood") || id.contains("drain"))  return "\uD83E\uDE78";
        if (id.contains("bat")   || id.contains("fly"))    return "\uD83E\uDD87";
        if (id.contains("fire")  || id.contains("flame"))  return "\uD83D\uDD25";
        if (id.contains("dash")  || id.contains("speed"))  return "\uD83D\uDCA8";
        if (id.contains("heal")  || id.contains("regen"))  return "\uD83D\uDC9A";
        if (id.contains("adapt"))                           return "\u26A1";
        return "\u2694";
    }

    private String getPassiveIcon(String id) {
        id = id.toLowerCase();
        if (id.contains("regen")  || id.contains("heal"))  return "\uD83D\uDC9A";
        if (id.contains("night")  || id.contains("dark"))  return "\uD83C\uDF19";
        if (id.contains("sun")    || id.contains("fire"))  return "\u2600";
        if (id.contains("hunger") || id.contains("blood")) return "\uD83E\uDE78";
        if (id.contains("resist") || id.contains("armor")) return "\uD83D\uDEE1";
        if (id.contains("speed")  || id.contains("swift")) return "\uD83D\uDCA8";
        return "\u25C6";
    }

    private String getKeybindHint(int index) {
        return switch (index) { case 0 -> "R"; case 1 -> "V"; default -> String.valueOf(index+1); };
    }

    private String formatCooldown(int ticks) {
        int s = ticks / 20;
        return s >= 60 ? String.format("%dm%ds", s/60, s%60) : s + "s";
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
