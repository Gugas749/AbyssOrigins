package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Abyssorigins.MODID)
public class OriginHudOverlay {

    private static final int BAR_WIDTH  = 100;
    private static final int BAR_HEIGHT = 8;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(mc.player);
        if (origin == null) return;

        PlayerOriginData data = mc.player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = 5;
        int y = screenH - 48;

        // ---- Resource bar ------------------------------------------------
        if (origin.getResourceType() != null) {
            float pct     = data.getResource() / origin.getResourceType().getMaxAmount();
            int fillWidth = (int)(BAR_WIDTH * pct);
            int color     = origin.getResourceType().getBarColor();

            // Background
            gfx.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF111111);
            // Fill
            if (fillWidth > 0) gfx.fill(x, y, x + fillWidth, y + BAR_HEIGHT, color);
            // Label
            gfx.drawString(mc.font,
                origin.getResourceType().getDisplayName().getString()
                    + ": " + (int)data.getResource() + "%",
                x, y - 10, 0xFFFFFF, false);

            y += BAR_HEIGHT + 14;
        }

        // ---- Ability cooldown icons --------------------------------------
        for (int i = 0; i < origin.getAbilities().size() && i < 2; i++) {
            var ability = origin.getAbilities().get(i);
            int cd    = i == 0 ? data.getCooldown1() : data.getCooldown2();
            int maxCd = ability.getBaseCooldownTicks();

            int bx = x + i * 26;
            int by = y;

            // Slot background
            gfx.fill(bx, by, bx + 22, by + 22, 0xFF222222);
            gfx.fill(bx + 1, by + 1, bx + 21, by + 21, 0xFF444444);

            if (cd > 0) {
                // Cooldown overlay (darker)
                float cdPct = (float)cd / maxCd;
                int overlayH = (int)(20 * cdPct);
                gfx.fill(bx + 1, by + 1, bx + 21, by + 1 + overlayH, 0x88000000);
                // Seconds remaining
                String secStr = String.valueOf(cd / 20 + 1);
                gfx.drawString(mc.font, secStr, bx + 2, by + 7, 0xFFFFAA, true);
            } else {
                // Ready indicator
                gfx.fill(bx + 1, by + 1, bx + 21, by + 21, 0xFF336633);
            }

            // Key hint
            String key = i == 0 ? "R" : "V";
            gfx.drawString(mc.font, key, bx + 7, by + 23, 0xAAAAAA, false);
        }
    }
}
