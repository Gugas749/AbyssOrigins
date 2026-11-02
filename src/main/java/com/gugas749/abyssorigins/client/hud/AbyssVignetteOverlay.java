package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientTimers;
import com.gugas749.abyssorigins.client.gui.AbyssTheme;
import com.gugas749.abyssorigins.specialization.occultist.OccultVisionAbility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * Screen-edge atmosphere for active states, drawn only on the owning client:
 * Occult Vision (violet veil + indicator), Shadow Meld (ink), Bat Form (crimson),
 * and a heartbeat vignette when a vampire's blood runs low.
 *
 * Replaces the old hard-edged rectangle frame drawn by OccultVisionAbility.
 */
public class AbyssVignetteOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();

        if (ClientTimers.isActive(OccultVisionAbility.ID)) {
            float p = AbyssTheme.pulse(4f);
            AbyssTheme.vignette(g, w, h, 0x2A0A45, h / 3, 0.55f + 0.15f * p);
            AbyssTheme.mist(g, w, h, 0x7A3FC0, 0.35f);
            int left = ClientTimers.remaining(OccultVisionAbility.ID) / 20;
            String time = left > 100_000 ? "∞" : left + "s";
            Component label = Component.translatable("abyssorigins.message.occult_vision_indicator").append(" " + time);
            int lw = mc.font.width(label);
            AbyssTheme.gradientH(g, w / 2 - lw / 2 - 30, 3, w / 2, 15, 0x00140822, 0xA0140822);
            AbyssTheme.gradientH(g, w / 2, 3, w / 2 + lw / 2 + 30, 15, 0xA0140822, 0x00140822);
            AbyssTheme.centered(g, mc.font, label, w / 2, 5, AbyssTheme.alpha(0xFFC48BFF, 0.75f + 0.25f * p));
        }

        if (ClientTimers.isActive("shade_meld"))
            AbyssTheme.vignette(g, w, h, 0x05070F, h / 3, 0.8f);

        if (ClientTimers.isActive("vampire_bat_form"))
            AbyssTheme.vignette(g, w, h, 0x2A0008, h / 4, 0.5f);

        Origin origin = ClientOriginData.getOrigin();
        if (origin != null && "vampire".equals(origin.getPath()) && ClientOriginData.getResourceBar() < 20f) {
            float beat = (float) Math.pow(AbyssTheme.pulse(0.9f), 6);
            AbyssTheme.vignette(g, w, h, 0x6A0010, h / 4, 0.25f + 0.45f * beat);
        }
    }
}
