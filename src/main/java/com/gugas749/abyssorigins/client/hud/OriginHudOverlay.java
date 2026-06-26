package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class OriginHudOverlay implements LayeredDraw.Layer {

    private static final int COLOR_WHITE     = 0xFFFFFFFF;
    private static final int COLOR_GRAY      = 0xFFAAAAAA;
    private static final int COLOR_DARK_GRAY = 0xFF666666;
    private static final int COLOR_GREEN     = 0xFF55FF55;
    private static final int COLOR_GOLD      = 0xFFFFAA00;
    private static final int COLOR_RED       = 0xFFFF5555;

    private static final int PANEL_BG        = 0xCC1A1A2E;
    private static final int PANEL_BORDER    = 0xFF3D3D5C;
    private static final int PANEL_HIGHLIGHT = 0x33FFFFFF;

    @Override
    public void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        Origin origin = ClientOriginData.getOrigin();
        if (origin == null) origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        ClientOriginData.tickCooldowns();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (AbyssOriginsConfig.showOriginHud) {
            renderOriginInfo(gfx, mc.font, sw, sh, origin);
        }

        if (AbyssOriginsConfig.showAbilityIndicators) {
            renderAbilityIndicators(gfx, mc.font, sw, sh, origin);
        }
    }

    // ---- Origin info panel ---------------------------------------------

    private static void renderOriginInfo(GuiGraphics gfx, Font font, int sw, int sh, Origin origin) {
        int panelW = 140, panelH = AbyssOriginsConfig.showXpBar ? 48 : 34;
        int[] pos  = panelPos(AbyssOriginsConfig.originInfoPosition, sw, sh, panelW, panelH, 4, 4);
        int x = pos[0], y = pos[1];

        int originColor = getOriginColor(origin);

        drawPanel(gfx, x, y, panelW, panelH);
        gfx.drawString(font, origin.getDisplayName(), x + 6, y + 6, originColor, true);

        String levelText = "Lv." + ClientOriginData.getLevel();
        int badgeW = font.width(levelText) + 8;
        int badgeX = x + panelW - badgeW - 2;
        gfx.fill(badgeX, y + 4, badgeX + badgeW, y + 16, darken(originColor, 0.6f));
        gfx.fill(badgeX, y + 4, badgeX + badgeW, y + 5, originColor);
        gfx.drawString(font, levelText, badgeX + 4, y + 6, COLOR_WHITE, true);

        if (AbyssOriginsConfig.showXpBar) {
            int xpBarX = x + 6, xpBarY = y + 22, xpBarW = panelW - 12, xpBarH = 8;
            gfx.fill(xpBarX, xpBarY, xpBarX + xpBarW, xpBarY + xpBarH, 0xFF1A1A1A);

            int fw = (int)(xpBarW * ClientOriginData.getXpProgress());
            if (fw > 0) {
                gfx.fill(xpBarX, xpBarY + 1, xpBarX + fw, xpBarY + xpBarH, 0xFF44DD44);
                gfx.fill(xpBarX, xpBarY + 1, xpBarX + fw, xpBarY + 3, 0xFF88FF88);
            }

            String xpText = (ClientOriginData.getXp() % 100) + "/" + ClientOriginData.getXpForNextLevel();
            gfx.drawString(font, xpText, xpBarX + xpBarW / 2 - font.width(xpText) / 2,
                xpBarY + xpBarH + 3, COLOR_GRAY, false);
        }
    }

    // ---- Ability cooldown boxes ----------------------------------------

    private static void renderAbilityIndicators(GuiGraphics gfx, Font font, int sw, int sh, Origin origin) {
        List<OriginAbility> abilities = origin.getAbilities();
        if (abilities.isEmpty()) return;

        int boxSize = 24;
        int gap     = 4;
        int totalH  = abilities.size() * (boxSize + gap) - gap;

        int[] pos = panelPos(AbyssOriginsConfig.abilityIndicatorsPosition, sw, sh,
            boxSize + 40, totalH, 4, 80);
        int x = pos[0];
        int startY = pos[1];

        for (int i = 0; i < abilities.size(); i++) {
            OriginAbility ability = abilities.get(i);
            int ay       = startY + i * (boxSize + gap);
            int cdTicks  = ClientOriginData.getAbilityCooldown(i);
            int maxCd    = ClientOriginData.getAbilityMaxCooldown(i);
            boolean onCd = cdTicks > 0;
            boolean ready = !onCd;

            int bgColor     = ready ? 0xCC224422 : 0xCC222222;
            int borderColor = ready ? COLOR_GREEN  : COLOR_GOLD;

            gfx.fill(x, ay, x + boxSize, ay + boxSize, bgColor);
            drawBoxBorder(gfx, x, ay, boxSize, boxSize, borderColor);

            if (onCd && AbyssOriginsConfig.showCooldownOverlays) {
                float pct = (float) cdTicks / Math.max(1, maxCd);
                int cdH = (int)(boxSize * pct);
                gfx.fill(x + 1, ay + boxSize - cdH, x + boxSize - 1, ay + boxSize - 1, 0xAA000000);
            }

            if (onCd) {
                String cds = String.valueOf((cdTicks + 19) / 20);
                gfx.drawString(font, cds, x + (boxSize - font.width(cds)) / 2,
                    ay + (boxSize - 8) / 2, COLOR_GOLD, true);
            } else {
                gfx.drawString(font, "\u2713", x + (boxSize - font.width("\u2713")) / 2,
                    ay + (boxSize - 8) / 2, COLOR_GREEN, true);
            }

            if (AbyssOriginsConfig.showKeybindHints) {
                String name = ability.getDisplayName().getString().length() > 12
                    ? ability.getDisplayName().getString().substring(0, 11) + ".."
                    : ability.getDisplayName().getString();
                gfx.drawString(font, name, x + boxSize + 4, ay + 4,
                    ready ? COLOR_WHITE : COLOR_DARK_GRAY, true);
                String kb = "[" + getKeybind(i) + "]";
                gfx.drawString(font, kb, x + boxSize + 4, ay + 14, COLOR_GRAY, false);
            }
        }
    }

    // ---- Helpers -------------------------------------------------------

    /**
     * Computes the top-left corner (x, y) for a panel given a HudPosition,
     * screen dimensions, panel size, and insets from the screen edge.
     */
    private static int[] panelPos(AbyssOriginsConfig.HudPosition pos,
                                  int sw, int sh, int pw, int ph,
                                  int insetX, int insetY) {
        return switch (pos) {
            case TOP_LEFT     -> new int[]{ insetX,          insetY };
            case TOP_RIGHT    -> new int[]{ sw - pw - insetX, insetY };
            case BOTTOM_LEFT  -> new int[]{ insetX,          sh - ph - insetY };
            case BOTTOM_RIGHT -> new int[]{ sw - pw - insetX, sh - ph - insetY };
        };
    }

    private static void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, PANEL_BG);
        gfx.fill(x, y, x + w, y + 1, PANEL_BORDER);
        gfx.fill(x, y + h - 1, x + w, y + h, PANEL_BORDER);
        gfx.fill(x, y, x + 1, y + h, PANEL_BORDER);
        gfx.fill(x + w - 1, y, x + w, y + h, PANEL_BORDER);
        gfx.fill(x + 1, y + 1, x + w - 1, y + 2, PANEL_HIGHLIGHT);
    }

    private static void drawBoxBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x, y, x + w, y + 1, color);
        gfx.fill(x, y + h - 1, x + w, y + h, color);
        gfx.fill(x, y, x + 1, y + h, color);
        gfx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static int getOriginColor(Origin origin) {
        return switch (origin.getId().getPath()) {
            case "vampire"  -> 0xFF8B0000;
            case "human"    -> 0xFFffaa44;
            default         -> 0xFFFFFFFF;
        };
    }

    private static int darken(int color, float f) {
        int a=(color>>24)&0xFF, r=(int)(((color>>16)&0xFF)*f),
            g=(int)(((color>>8)&0xFF)*f), b=(int)((color&0xFF)*f);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private static String getKeybind(int index) {
        return switch (index) { case 0 -> "R"; case 1 -> "V"; default -> String.valueOf(index+1); };
    }
}
