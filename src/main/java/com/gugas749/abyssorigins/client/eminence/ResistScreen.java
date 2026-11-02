package com.gugas749.abyssorigins.client.eminence;

import com.gugas749.abyssorigins.network.packet.ResistResultPacket;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * Resisting the Presence: a timing skill check. After a random pause a warning chime sounds,
 * a gold arc appears on the dial and the needle sweeps from the top — press SPACE (or click)
 * while it crosses the arc. Three successes break the hold; pressing outside the arc or letting
 * the needle pass it is failure (presses between checks are ignored). Each check is faster and narrower.
 */
public class ResistScreen extends Screen {

    private static final int NEEDED = 3;
    private static final float[] ZONE_DEG = {40f, 33f, 27f};
    private static final float[] REV_MS   = {1100f, 960f, 860f};
    private static final int GOLD_C = 0xFF000000 | Eminence.COLOR;
    private static final int FAIL_C = 0xFFE0566B;

    private enum State { WAIT, ACTIVE, WON, LOST }

    private final RandomSource rng;
    private final long opened = Util.getMillis(), deadline;
    private State state;
    private long stateStart, waitMs, flashAt;
    private float zoneStart;
    private int successes, flashColor;
    private boolean sent;

    public ResistScreen(long seed, int ticks) {
        super(Component.translatable("gui.abyssorigins.resist"));
        this.rng = RandomSource.create(seed);
        this.deadline = opened + ticks * 50L - 400L;
        nextCheck(opened + 500L);
    }

    @Override public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}
    @Override public boolean isPauseScreen()    { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    // ---- Logic ---------------------------------------------------------------------------------

    private void nextCheck(long now) {
        state = State.WAIT;
        stateStart = now;
        waitMs = 600 + rng.nextInt(1000);
        float w = ZONE_DEG[Math.min(successes, ZONE_DEG.length - 1)];
        zoneStart = 115f + rng.nextFloat() * (300f - 115f - w);
    }

    private float sweep(long now) {
        return (now - stateStart) * 360f / REV_MS[Math.min(successes, REV_MS.length - 1)];
    }

    private float zoneWidth() { return ZONE_DEG[Math.min(successes, ZONE_DEG.length - 1)]; }

    private void update(long now) {
        if (state == State.WAIT && now - stateStart >= waitMs) {
            state = State.ACTIVE;
            stateStart = now;
            sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5f);
        }
        if (state == State.ACTIVE && sweep(now) > zoneStart + zoneWidth() + 3f) lose(now);     // let it pass
        if (!sent && now > deadline) lose(now);
        if ((state == State.WON || state == State.LOST) && now - stateStart > 1100) onClose();
    }

    private void press() {
        long now = Util.getMillis();
        update(now);
        if (state != State.ACTIVE) return;              // pauses between checks forgive stray presses
        float a = sweep(now);
        if (a >= zoneStart && a <= zoneStart + zoneWidth()) {
            successes++;
            flash(now, GOLD_C);
            sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f + successes * 0.25f);
            if (successes >= NEEDED) win(now);
            else nextCheck(now);
        } else {
            lose(now);
        }
    }

    private void win(long now) {
        state = State.WON; stateStart = now;
        send(true);
        sound(SoundEvents.TOTEM_USE, 1.3f);
    }

    private void lose(long now) {
        if (state == State.LOST || state == State.WON) return;
        state = State.LOST; stateStart = now;
        flash(now, FAIL_C);
        send(false);
        sound(SoundEvents.GLASS_BREAK, 0.6f);
    }

    private void send(boolean ok) {
        if (sent) return;
        sent = true;
        PacketDistributor.sendToServer(new ResistResultPacket(ok));
    }

    private void flash(long now, int color) { flashAt = now; flashColor = color; }

    private static void sound(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    // ---- Drawing -----------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        long now = Util.getMillis();
        update(now);
        float fade = easeOut((now - opened) / 300f);
        int cx = width / 2, cy = height / 2 + 6;
        int r = 46;

        g.fillGradient(0, 0, width, height, alpha(0xD0040306, fade), alpha(0xF0100804, fade));
        vignette(g, width, height, 0x000000, Math.max(30, height / 4), 0.95f * fade);

        Component title = Component.translatable("gui.abyssorigins.resist.title");
        scaledText(g, font, title, cx - font.width(title) * 1.5f / 2f, cy - r - 52, 1.5f, alpha(lighten(GOLD_C, 0.2f), fade), true);
        centered(g, font, Component.translatable("gui.abyssorigins.resist.hint"), cx, cy - r - 32, alpha(TEXT_DIM, fade));

        // Progress pips
        for (int i = 0; i < NEEDED; i++) {
            float px = cx + (i - 1) * 16;
            diamondRing(g, px, cy - r - 16, 5, 1.2f, alpha(GOLD_C, fade), 0xFF0B0816);
            if (i < successes) diamond(g, px, cy - r - 16, 3.2f, alpha(lighten(GOLD_C, 0.3f), fade));
        }

        // Dial
        glow(g, cx, cy, r + 26, state == State.LOST ? 0xE0566B : Eminence.COLOR, (state == State.ACTIVE ? 0.55f : 0.25f) * fade);
        ring(g, cx, cy, r, 0f, 360f, 2f, alpha(0xFF3A3150, fade));
        ring(g, cx, cy, r - 7, 0f, 360f, 1f, alpha(0x802E2650, fade));
        if (state == State.ACTIVE) {
            ring(g, cx, cy, r, zoneStart, zoneStart + zoneWidth(), 7f, alpha(GOLD_C, fade));
            ring(g, cx, cy, r, zoneStart, zoneStart + Math.min(6f, zoneWidth()), 7f, alpha(0xFFFFF4D6, fade));
            float a = Math.min(sweep(now), 360f);
            float rad = a * Mth.DEG_TO_RAD;
            line(g, cx, cy, cx + Mth.sin(rad) * (r + 6), cy - Mth.cos(rad) * (r + 6), 2f, 0xFFFF4A5A);
            diamond(g, cx + Mth.sin(rad) * (r + 6), cy - Mth.cos(rad) * (r + 6), 2.5f, 0xFFFFFFFF);
        } else if (state == State.WAIT) {
            ring(g, cx, cy, r, 0f, 360f, 2f, alpha(withAlpha(Eminence.COLOR, 120), 0.4f + 0.6f * pulse(0.8f)));
        }
        centered(g, font, "♛", cx, cy - 4, alpha(state == State.LOST ? FAIL_C : GOLD_C, fade));

        // Overall time
        float left = Mth.clamp((deadline - now) / (float) (deadline - opened), 0f, 1f);
        bar(g, cx - 80, (int) (cy + r + 18), 160, 3, left, GOLD_C, false);

        if (state == State.WON)
            centered(g, font, Component.translatable("gui.abyssorigins.resist.won"), cx, (int) (cy + r + 28), GOOD);
        if (state == State.LOST)
            centered(g, font, Component.translatable("gui.abyssorigins.resist.lost"), cx, (int) (cy + r + 28), FAIL_C);

        float f = 1f - (now - flashAt) / 260f;
        if (f > 0 && flashAt > 0) g.fill(0, 0, width, height, withAlpha(flashColor & 0xFFFFFF, (int) (70 * f)));
    }

    /** Arc from {@code fromDeg} to {@code toDeg}, clockwise from the top. */
    private static void ring(GuiGraphics g, float cx, float cy, float r, float fromDeg, float toDeg, float thickness, int color) {
        float step = 4f;
        for (float a = fromDeg; a < toDeg; a += step) {
            float b = Math.min(toDeg, a + step);
            float ar = a * Mth.DEG_TO_RAD, br = b * Mth.DEG_TO_RAD;
            line(g, cx + Mth.sin(ar) * r, cy - Mth.cos(ar) * r, cx + Mth.sin(br) * r, cy - Mth.cos(br) * r, thickness, color);
        }
    }

    // ---- Input -----------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_E) { press(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        press();
        return true;
    }
}
