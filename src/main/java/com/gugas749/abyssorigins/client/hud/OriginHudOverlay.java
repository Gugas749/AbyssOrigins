package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.client.ClientOriginData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * Origin HUD Overlay.
 * Ported from VeilOrigins' OriginHudOverlay, reads from ClientOriginData.
 *
 * Renders:
 *  - Top-left panel: origin name + level badge + XP bar
 *  - Right side: ability cooldown boxes
 *
 * Blood bar for vampires is handled separately by VampireHudHandler
 * (it replaces the vanilla hunger bar in that exact position).
 */
public class OriginHudOverlay implements LayeredDraw.Layer {

    private static final int COLOR_WHITE       = 0xFFFFFFFF;
    private static final int COLOR_GRAY        = 0xFFAAAAAA;
    private static final int COLOR_DARK_GRAY   = 0xFF666666;
    private static final int COLOR_GREEN       = 0xFF55FF55;
    private static final int COLOR_GOLD        = 0xFFFFAA00;
    private static final int COLOR_RED         = 0xFFFF5555;
    private static final int COLOR_CYAN        = 0xFF55FFFF;

    private static final int PANEL_BG        = 0xCC1A1A2E;
    private static final int PANEL_BORDER    = 0xFF3D3D5C;
    private static final int PANEL_HIGHLIGHT = 0x33FFFFFF;

    @Override
    public void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        Origin origin = ClientOriginData.getOrigin();
        if (origin == null) origin = com.gugas749.abyssorigins.api.AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        // Tick client-side cooldowns
        ClientOriginData.tickCooldowns();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        renderOriginInfo      (gfx, mc.font, sw, sh, origin);
        renderAbilityIndicators(gfx, mc.font, sw, sh, player, origin);
    }

    // ---- Origin info panel (top-left) ----------------------------------

    private static void renderOriginInfo(GuiGraphics gfx, Font font, int sw, int sh, Origin origin) {
        int x = 4, y = 4;
        int level = ClientOriginData.getLevel();
        float xpProgress = ClientOriginData.getXpProgress();
        int originColor = getOriginColor(origin);

        int panelW = 140, panelH = 48;
        drawPanel(gfx, x, y, panelW, panelH);

        // Origin name
        gfx.drawString(font, origin.getDisplayName(), x + 6, y + 6, originColor, true);

        // Level badge
        String levelText = "Lv." + level;
        int badgeW = font.width(levelText) + 8;
        int badgeX = x + panelW - badgeW - 2;
        gfx.fill(badgeX, y + 4, badgeX + badgeW, y + 16, darkenColor(originColor, 0.6f));
        gfx.fill(badgeX, y + 4, badgeX + badgeW, y + 5, originColor);
        gfx.drawString(font, levelText, badgeX + 4, y + 6, COLOR_WHITE, true);

        // XP bar
        int xpBarX = x + 6, xpBarY = y + 22, xpBarW = panelW - 12, xpBarH = 8;
        gfx.fill(xpBarX, xpBarY, xpBarX + xpBarW, xpBarY + xpBarH, 0xFF1A1A1A);
        gfx.fill(xpBarX, xpBarY, xpBarX + xpBarW, xpBarY + 1, 0xFF000000);

        int fw = (int)(xpBarW * xpProgress);
        if (fw > 0) {
            int xpColor = 0xFF44DD44;
            gfx.fill(xpBarX, xpBarY + 1, xpBarX + fw, xpBarY + xpBarH, xpColor);
            gfx.fill(xpBarX, xpBarY + 1, xpBarX + fw, xpBarY + 3, lightenColor(xpColor, 0.3f));
        }

        String xpText = (ClientOriginData.getXp() % 100) + " / " + ClientOriginData.getXpForNextLevel() + " XP";
        int xpTW = font.width(xpText);
        gfx.drawString(font, xpText, xpBarX + xpBarW / 2 - xpTW / 2, xpBarY + xpBarH + 3, COLOR_GRAY, false);
    }

    // ---- Ability cooldown boxes (right side) ---------------------------

    private static void renderAbilityIndicators(GuiGraphics gfx, Font font, int sw, int sh,
                                                Player player, Origin origin) {
        int x = sw - 100;
        int startY = sh - 80;

        java.util.List<OriginAbility> abilities = origin.getAbilities();

        for (int i = 0; i < abilities.size(); i++) {
            OriginAbility ability = abilities.get(i);
            int abilityY = startY - (i * 32);

            int cdTicks  = ClientOriginData.getAbilityCooldown(i);
            int maxCd    = ClientOriginData.getAbilityMaxCooldown(i);
            boolean onCd = cdTicks > 0;
            boolean canUse = !onCd;

            int boxSize = 24;
            int bgColor     = canUse ? 0xCC224422 : (onCd ? 0xCC222222 : 0xCC442222);
            int borderColor = canUse ? COLOR_GREEN : (onCd ? COLOR_GOLD : COLOR_RED);

            gfx.fill(x, abilityY, x + boxSize, abilityY + boxSize, bgColor);
            drawBoxBorder(gfx, x, abilityY, boxSize, boxSize, borderColor);

            if (onCd) {
                float pct = (float) cdTicks / Math.max(1, maxCd);
                int cdH = (int)(boxSize * pct);
                gfx.fill(x + 1, abilityY + boxSize - cdH, x + boxSize - 1, abilityY + boxSize - 1, 0xAA000000);
                String cds = String.valueOf((cdTicks + 19) / 20);
                gfx.drawString(font, cds, x + (boxSize - font.width(cds)) / 2, abilityY + (boxSize - 8) / 2, COLOR_GOLD, true);
            } else {
                gfx.drawString(font, "\u2713", x + (boxSize - font.width("\u2713")) / 2,
                    abilityY + (boxSize - 8) / 2, COLOR_GREEN, true);
            }

            // Ability name + keybind
            String name = formatAbilityName(ability.getDisplayName());
            gfx.drawString(font, name, x + boxSize + 4, abilityY + 4,
                canUse ? COLOR_WHITE : COLOR_DARK_GRAY, true);

            String kb = "[" + getKeybind(i) + "]";
            gfx.drawString(font, kb, x + boxSize + 4, abilityY + 14, COLOR_GRAY, false);
        }
    }

    // ---- Drawing helpers -----------------------------------------------

    private static void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, PANEL_BG);
        gfx.fill(x,     y,         x + w, y + 1,     PANEL_BORDER);
        gfx.fill(x,     y + h - 1, x + w, y + h,     PANEL_BORDER);
        gfx.fill(x,     y,         x + 1, y + h,      PANEL_BORDER);
        gfx.fill(x+w-1, y,         x + w, y + h,      PANEL_BORDER);
        gfx.fill(x + 1, y + 1,     x+w-1, y + 2,      PANEL_HIGHLIGHT);
    }

    private static void drawBoxBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,     y,         x + w, y + 1,     color);
        gfx.fill(x,     y + h - 1, x + w, y + h,     color);
        gfx.fill(x,     y,         x + 1, y + h,      color);
        gfx.fill(x+w-1, y,         x + w, y + h,      color);
    }

    // ---- Color helpers -------------------------------------------------

    private static int getOriginColor(Origin origin) {
        String id = origin.getId().getPath();
        return switch (id) {
            case "vampire"  -> 0xFF8B0000;
            case "human"    -> 0xFFffaa44;
            case "werewolf" -> 0xFF8B4513;
            default         -> 0xFFFFFFFF;
        };
    }

    private static int darkenColor(int color, float f) {
        int a=(color>>24)&0xFF, r=(int)(((color>>16)&0xFF)*f),
            g=(int)(((color>>8)&0xFF)*f), b=(int)((color&0xFF)*f);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private static int lightenColor(int color, float f) {
        int a=(color>>24)&0xFF,
            r=Math.min(255,(int)(((color>>16)&0xFF)*(1+f))),
            g=Math.min(255,(int)(((color>>8)&0xFF)*(1+f))),
            b=Math.min(255,(int)((color&0xFF)*(1+f)));
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private static String formatAbilityName(String name) {
        return name.length() > 12 ? name.substring(0, 11) + ".." : name;
    }

    private static String getKeybind(int index) {
        return switch (index) { case 0 -> "R"; case 1 -> "V"; default -> String.valueOf(index+1); };
    }
}
