package com.gugas749.abyssorigins.client.ascension;

import com.gugas749.abysscore.api.cinematic.client.AbyssCinematicsClient;
import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Ascension;
import com.gugas749.abyssorigins.api.Ascension.Timeline;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.ascension.AscensionCinematic;
import com.gugas749.abyssorigins.client.gui.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The new title at the end of the ascension cinematic (the rest of it is AbyssCore's, built by the server's
 * AscensionCinematic): "Level 25 reached", the title ("Noble Vampire") rising into place, a line in the
 * origin's colour and its sigil. Timed by the running cinematic, so it stops with it.
 *
 * Drawn after AbyssCore's cinematic layers (they draw in RenderGuiEvent.Post and hide every GUI layer while
 * the HUD is hidden), so the title sits over the burst's flash.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class AscensionTitle {

    private static Origin origin;
    private static int stage;

    private AscensionTitle() {}

    /** AscensionPacket: the local player started ascending into {@code toStage}. */
    public static void show(String originId, int toStage) {
        origin = AbyssOriginsAPI.getOrigin(ResourceLocation.parse(originId));
        stage = toStage;
    }

    public static void clear() { origin = null; }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (origin == null) return;
        float a = AbyssCinematicsClient.age(AscensionCinematic.ID, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (a < 0) { if (!AbyssCinematicsClient.isPlaying(AscensionCinematic.ID)) origin = null; return; }
        Timeline tl = Ascension.timeline(stage);
        float alpha = window(a, tl.burst() + 8, tl.total() - 12, 8);
        if (alpha <= 0.03f) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int w = g.guiWidth(), h = g.guiHeight();
        int color = origin.getAscensionColor(), color2 = origin.getAscensionColor2();
        int text = AbyssTheme.mix(0xFF000000 | color, 0xFFFFFFFF, 0.55f) & 0xFFFFFF;

        float rise = 1 - AbyssTheme.easeOut((a - tl.burst() - 8) / 20f);
        float cy = h * 0.36f + rise * 8;
        Component reached = Component.translatable("abyssorigins.ascension.reached", Ascension.levelFor(stage));
        textAt(g, font, reached, w / 2f, cy - 22, 1f, alpha, AbyssTheme.mix(0xFF000000 | color2, 0xFFFFFFFF, 0.3f) & 0xFFFFFF, false);
        Component title = Ascension.title(origin, stage);
        float scale = Math.min(stage >= Ascension.LORD ? 3.0f : 2.5f, w * 0.86f / Math.max(1, font.width(title))) * (1 + 0.12f * rise);
        textAt(g, font, title, w / 2f, cy, scale, alpha, text, true);

        float lineW = font.width(title) * scale * 0.6f * AbyssTheme.easeOut((a - tl.burst() - 10) / 18f);
        int y = Math.round(cy + font.lineHeight * scale / 2f + 6);
        int col = (Math.round(alpha * 220) << 24) | color;
        AbyssTheme.gradientH(g, Math.round(w / 2f - lineW), y, Math.round(w / 2f), y + 1, col & 0xFFFFFF, col);
        AbyssTheme.gradientH(g, Math.round(w / 2f), y, Math.round(w / 2f + lineW), y + 1, col, col & 0xFFFFFF);
        textAt(g, font, Component.literal(origin.getSigil()), w / 2f, y + 10, 1.2f, alpha, color, true);
    }

    /** 0 -> 1 -> 0 between {@code from} and {@code to}, fading over {@code fade} ticks at each end. */
    private static float window(float a, float from, float to, float fade) {
        if (a < from || a > to) return 0;
        return smooth((a - from) / fade) * (1 - smooth((a - (to - fade)) / fade));
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void textAt(GuiGraphics g, Font font, Component text, float cx, float cy, float scale,
                               float alpha, int rgb, boolean shadow) {
        int al = Math.round(Mth.clamp(alpha, 0, 1) * 255);
        if (al < 8) return;
        g.pose().pushPose();
        g.pose().translate(cx, cy - font.lineHeight * scale / 2f, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, -font.width(text) / 2, 0, (al << 24) | rgb, shadow);
        g.pose().popPose();
    }
}
