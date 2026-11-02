package com.gugas749.abyssorigins.client.eminence;

import com.gugas749.abyssorigins.network.packet.PresenceChoicePacket;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/** "Kneel or resist." Shown to every player caught in the Eminence's Presence. */
public class PresenceChoiceScreen extends Screen {

    private static final int GOLD_C = 0xFF000000 | Eminence.COLOR;
    private static final int DEFY = 0xFFE0566B;
    private static final int BW = 124, BH = 30;

    private final String caster;
    private final long opened = Util.getMillis(), expires;
    private Component status = null;

    public PresenceChoiceScreen(String caster, int ticks) {
        super(Component.translatable("gui.abyssorigins.presence"));
        this.caster = caster;
        this.expires = opened + ticks * 50L;
    }

    @Override public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}
    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean shouldCloseOnEsc()  { return false; }

    private int kneelX()  { return width / 2 - BW - 8; }
    private int resistX() { return width / 2 + 8; }
    private int buttonY() { return height / 2 + 34; }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        long now = Util.getMillis();
        float fade = easeOut((now - opened) / 350f);
        int cx = width / 2, cy = height / 2;

        g.fillGradient(0, 0, width, height, alpha(0xB0050308, fade), alpha(0xE0140C02, fade));
        motes(g, width, height, GOLD_C, 30, fade);
        vignette(g, width, height, 0x000000, Math.max(30, height / 4), 0.9f * fade);

        // The crown bears down
        float crownY = cy - 74;
        glow(g, cx, crownY + 8, 46 + 6 * pulse(2.2f), Eminence.COLOR, 0.75f * fade);
        scaledText(g, font, Component.literal("♛"), cx - font.width("♛") * 2.6f / 2f, crownY - 4, 2.6f, alpha(GOLD_C, fade), true);

        centered(g, font, Component.translatable("gui.abyssorigins.presence.eyebrow"), cx, cy - 40, alpha(TEXT_FAINT, fade));
        Component title = Component.translatable("gui.abyssorigins.presence.title", caster);
        float ts = 1.4f;
        scaledText(g, font, title, cx - font.width(title) * ts / 2f, cy - 26, ts, alpha(lighten(GOLD_C, 0.25f), fade), true);
        wrappedCentered(g, Component.translatable("gui.abyssorigins.presence.body"), cx, cy - 6, 300, alpha(TEXT_DIM, fade));

        if (status == null) {
            button(g, kneelX(), buttonY(), Component.translatable("gui.abyssorigins.presence.kneel"), "1", GOLD_C, mx, my, fade);
            button(g, resistX(), buttonY(), Component.translatable("gui.abyssorigins.presence.resist"), "2", DEFY, mx, my, fade);
            float left = Math.max(0f, (expires - now) / (float) (expires - opened));
            bar(g, cx - 120, buttonY() + BH + 12, 240, 3, left, GOLD_C, false);
            if (left <= 0f)
                centered(g, font, Component.translatable("gui.abyssorigins.presence.silence"), cx, buttonY() + BH + 20, alpha(TEXT_FAINT, fade));
        } else {
            centered(g, font, status, cx, buttonY() + 10, alpha(TEXT, 0.6f + 0.4f * pulse(1.4f)));
        }
    }

    private void button(GuiGraphics g, int x, int y, Component label, String key, int accent, int mx, int my, float fade) {
        boolean hover = inside(mx, my, x, y, BW, BH);
        if (hover) glow(g, x + BW / 2f, y + BH / 2f, BW * 0.7f, accent & 0xFFFFFF, 0.5f);
        g.fillGradient(x, y, x + BW, y + BH, alpha(hover ? 0xF0221A10 : 0xE0141008, fade), alpha(0xE0080604, fade));
        border(g, x, y, BW, BH, alpha(hover ? accent : mix(EDGE, accent, 0.5f), fade));
        centered(g, font, label, x + BW / 2, y + 8, alpha(hover ? lighten(accent, 0.3f) : accent, fade));
        centered(g, font, "[" + key + "]", x + BW / 2, y + BH - 10, alpha(TEXT_FAINT, fade));
    }

    private void wrappedCentered(GuiGraphics g, Component text, int cx, int y, int w, int color) {
        for (var line : font.split(text, w)) {
            g.drawString(font, line, cx - font.width(line) / 2, y, color, false);
            y += 10;
        }
    }

    private void choose(boolean kneel) {
        if (status != null) return;
        status = Component.translatable(kneel ? "gui.abyssorigins.presence.kneeling" : "gui.abyssorigins.presence.steeling");
        PacketDistributor.sendToServer(new PresenceChoicePacket(kneel));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (status == null && button == 0) {
            if (inside(mx, my, kneelX(), buttonY(), BW, BH))  { choose(true);  return true; }
            if (inside(mx, my, resistX(), buttonY(), BW, BH)) { choose(false); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_1 || key == GLFW.GLFW_KEY_KP_1) { choose(true);  return true; }
        if (key == GLFW.GLFW_KEY_2 || key == GLFW.GLFW_KEY_KP_2) { choose(false); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
