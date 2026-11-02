package com.gugas749.abyssorigins.client.gui;

import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * The Abyss visual language: deep violet-black void, drifting mist, faint rising motes,
 * thin luminous edges and diamond motifs. Every screen and HUD element draws through
 * these helpers so the mod reads as one cohesive interface.
 */
public final class AbyssTheme {

    // ---- Palette ------------------------------------------------------------

    public static final int VOID       = 0xFF06050C;
    public static final int ABYSS      = 0xFF0E0B1A;
    public static final int PANEL      = 0xE00F0C1D;
    public static final int PANEL_HI   = 0xE0171230;
    public static final int EDGE       = 0xFF2E2650;
    public static final int EDGE_SOFT  = 0x802E2650;
    public static final int TEXT       = 0xFFE6E1F5;
    public static final int TEXT_DIM   = 0xFF9690B4;
    public static final int TEXT_FAINT = 0xFF5E5880;
    public static final int GOLD       = 0xFFE8C872;
    public static final int GOOD       = 0xFF6FD8A0;
    public static final int BAD        = 0xFFE0566B;
    public static final int MIST       = 0xFF8E7BD8;

    private AbyssTheme() {}

    // ---- Time & colour maths ----------------------------------------------------

    /** Seconds since an arbitrary epoch, for animation. */
    public static float time() { return (Util.getMillis() % 3_600_000L) / 1000f; }

    /** 0..1 sine pulse with the given period in seconds. */
    public static float pulse(float period) { return 0.5f + 0.5f * Mth.sin(time() * Mth.TWO_PI / period); }

    public static float easeOut(float t) { t = Mth.clamp(t, 0f, 1f); float u = 1 - t; return 1 - u * u * u; }

    public static int alpha(int argb, float a) {
        int base = (argb >>> 24) & 0xFF;
        return (Mth.clamp((int) (base * a), 0, 255) << 24) | (argb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, int a) { return (Mth.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF); }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int darken(int argb, float f)  { return mix(argb, argb & 0xFF000000, 1 - f); }
    public static int lighten(int argb, float f) { return mix(argb, argb | 0x00FFFFFF, f); }

    // ---- Primitive shapes -------------------------------------------------------

    public static void gradientH(GuiGraphics g, int x0, int y0, int x1, int y1, int left, int right) {
        int w = x1 - x0;
        if (w <= 0) return;
        int steps = Math.min(w, 24);
        for (int i = 0; i < steps; i++) {
            int sx = x0 + w * i / steps, ex = x0 + w * (i + 1) / steps;
            g.fill(sx, y0, ex, y1, mix(left, right, (i + 0.5f) / steps));
        }
    }

    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Filled diamond (square rotated 45°) centred on (cx, cy); {@code r} = centre-to-tip distance. */
    public static void diamond(GuiGraphics g, float cx, float cy, float r, int color) {
        if (r <= 0) return;
        float s = r / Mth.SQRT_OF_TWO;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45));
        g.pose().scale(s, s, 1);
        g.fill(-1, -1, 1, 1, color);
        g.pose().popPose();
    }

    /** Diamond ring of the given thickness drawn over {@code inner}. */
    public static void diamondRing(GuiGraphics g, float cx, float cy, float r, float thickness, int color, int inner) {
        diamond(g, cx, cy, r, color);
        diamond(g, cx, cy, r - thickness * Mth.SQRT_OF_TWO, inner);
    }

    /** Soft glow: stacked translucent diamonds. */
    public static void glow(GuiGraphics g, float cx, float cy, float r, int rgb, float intensity) {
        int steps = 6;
        for (int i = steps; i >= 1; i--) {
            float f = i / (float) steps;
            int a = (int) (intensity * 46 * (1 - f) * (1 - f) + intensity * 8);
            diamond(g, cx, cy, r * f, withAlpha(rgb, a));
        }
    }

    /** Line of arbitrary angle and thickness. */
    public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        g.pose().translate(0, -thickness / 2f, 0);
        g.pose().scale(len, thickness, 1);
        g.fill(0, 0, 1, 1, color);
        g.pose().popPose();
    }

    public static void dashedLine(GuiGraphics g, float x1, float y1, float x2, float y2, float thickness, float dash, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        float ux = dx / len, uy = dy / len;
        for (float t = 0; t < len; t += dash * 2) {
            float e = Math.min(len, t + dash);
            line(g, x1 + ux * t, y1 + uy * t, x1 + ux * e, y1 + uy * e, thickness, color);
        }
    }

    // ---- Atmosphere -------------------------------------------------------------

    /** Full-screen abyss backdrop: gradient void, tinted mist, rising motes, vignette. */
    public static void backdrop(GuiGraphics g, int w, int h, int tint, float fade) {
        g.fillGradient(0, 0, w, h, alpha(0xF205040B, fade), alpha(0xF5100C1E, fade));
        mist(g, w, h, tint, 0.9f * fade);
        motes(g, w, h, tint, 46, fade);
        vignette(g, w, h, 0xFF000000, Math.max(24, h / 5), 0.75f * fade);
    }

    /** Slow drifting fog banks. */
    public static void mist(GuiGraphics g, int w, int h, int tint, float strength) {
        float t = time();
        for (int i = 0; i < 6; i++) {
            float seed = i * 1.7f;
            float cx = w * (0.15f + 0.7f * (0.5f + 0.5f * Mth.sin(t * 0.045f + seed * 2.1f)));
            float cy = h * (0.2f + 0.65f * (0.5f + 0.5f * Mth.cos(t * 0.032f + seed * 1.3f)));
            float rx = w * (0.22f + 0.08f * Mth.sin(seed));
            float ry = h * (0.14f + 0.05f * Mth.cos(seed * 1.9f));
            int color = i % 2 == 0 ? tint : MIST;
            softEllipse(g, cx, cy, rx, ry, color, strength * 0.55f);
        }
    }

    /** Approximate soft ellipse with layered scanlines (brighter toward the middle). */
    public static void softEllipse(GuiGraphics g, float cx, float cy, float rx, float ry, int rgb, float strength) {
        if (strength <= 0.01f) return;
        for (int layer = 0; layer < 3; layer++) {
            float scale = 1f - layer * 0.3f;
            float lrx = rx * scale, lry = ry * scale;
            int a = (int) (9 * strength);
            if (a <= 0) continue;
            int col = withAlpha(rgb, a);
            int step = 4;
            for (float dy = -lry; dy < lry; dy += step) {
                float k = dy / lry;
                float half = lrx * Mth.sqrt(Math.max(0, 1 - k * k));
                g.fill((int) (cx - half), (int) (cy + dy), (int) (cx + half), (int) (cy + dy + step), col);
            }
        }
    }

    /** Tiny glimmering particles rising through the dark. */
    public static void motes(GuiGraphics g, int w, int h, int tint, int count, float fade) {
        float t = time();
        for (int i = 0; i < count; i++) {
            float sx = hash(i * 12.9898f) * w;
            float speed = 4 + hash(i * 3.17f) * 10;
            float y = (hash(i * 78.233f) * (h + 40) - t * speed) % (h + 40);
            if (y < 0) y += h + 40;
            y -= 20;
            float x = sx + Mth.sin(t * 0.6f + i) * 6;
            float flicker = 0.35f + 0.65f * (0.5f + 0.5f * Mth.sin(t * (1.3f + hash(i) * 2f) + i * 4f));
            int a = (int) (110 * flicker * fade);
            int col = withAlpha(i % 3 == 0 ? 0xFFFFFF : tint, a);
            int s = hash(i * 9.1f) > 0.85f ? 2 : 1;
            g.fill((int) x, (int) y, (int) x + s, (int) y + s, col);
        }
    }

    public static void vignette(GuiGraphics g, int w, int h, int rgb, int depth, float strength) {
        int a = (int) (200 * strength);
        g.fillGradient(0, 0, w, depth, withAlpha(rgb, a), withAlpha(rgb, 0));
        g.fillGradient(0, h - depth, w, h, withAlpha(rgb, 0), withAlpha(rgb, a));
        gradientH(g, 0, 0, depth, h, withAlpha(rgb, a), withAlpha(rgb, 0));
        gradientH(g, w - depth, 0, w, h, withAlpha(rgb, 0), withAlpha(rgb, a));
    }

    private static float hash(float n) {
        float s = Mth.sin(n) * 43758.5453f;
        return s - (float) Math.floor(s);
    }

    // ---- Composite widgets ------------------------------------------------------

    /** Panel with gradient body, hairline edge, glowing accent crest and corner ticks. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent, float fade) {
        g.fillGradient(x, y, x + w, y + h, alpha(PANEL_HI, fade), alpha(PANEL, fade));
        border(g, x, y, w, h, alpha(EDGE, fade));
        int mid = x + w / 2;
        gradientH(g, x + 1, y, mid, y + 1, alpha(accent & 0x00FFFFFF, fade), alpha(accent, fade));
        gradientH(g, mid, y, x + w - 1, y + 1, alpha(accent, fade), alpha(accent & 0x00FFFFFF, fade));
        int tick = 4, c = alpha(accent, 0.9f * fade);
        g.fill(x, y, x + tick, y + 1, c);           g.fill(x, y, x + 1, y + tick, c);
        g.fill(x + w - tick, y, x + w, y + 1, c);   g.fill(x + w - 1, y, x + w, y + tick, c);
        g.fill(x, y + h - 1, x + tick, y + h, c);   g.fill(x, y + h - tick, x + 1, y + h, c);
        g.fill(x + w - tick, y + h - 1, x + w, y + h, c); g.fill(x + w - 1, y + h - tick, x + w, y + h, c);
    }

    /** "◆ TITLE ———" section header. Returns the height used. */
    public static int header(GuiGraphics g, Font font, int x, int y, int w, Component text, int accent, float fade) {
        diamond(g, x + 3, y + 4, 3, alpha(accent, fade));
        g.drawString(font, text, x + 10, y, alpha(TEXT, fade), false);
        int lx = x + 14 + font.width(text);
        if (lx < x + w) gradientH(g, lx, y + 4, x + w, y + 5, alpha(accent, 0.6f * fade), alpha(accent & 0x00FFFFFF, fade));
        return 12;
    }

    /** Resource / progress bar with gradient fill, highlight and travelling shimmer. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float pct, int color, boolean shimmer) {
        g.fill(x, y, x + w, y + h, 0xFF09070F);
        border(g, x - 1, y - 1, w + 2, h + 2, EDGE_SOFT);
        int fw = (int) (w * Mth.clamp(pct, 0f, 1f));
        if (fw <= 0) return;
        gradientH(g, x, y, x + fw, y + h, darken(color, 0.55f), color);
        g.fill(x, y, x + fw, y + 1, alpha(lighten(color, 0.45f), 0.85f));
        if (shimmer && fw > 6) {
            float t = (time() * 0.35f) % 1.4f - 0.2f;
            int sx = x + (int) (t * w);
            if (sx >= x && sx < x + fw - 4) g.fill(sx, y, Math.min(x + fw, sx + 4), y + h, 0x30FFFFFF);
        }
    }

    public static void centered(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, true);
    }

    public static void centered(GuiGraphics g, Font font, String text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, true);
    }

    /** Draw text scaled about its top-left corner. */
    public static void scaledText(GuiGraphics g, Font font, Component text, float x, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, shadow);
        g.pose().popPose();
    }

    /** Wrapped text; returns the y after the last line. Stops (with "…") after maxLines. */
    public static int wrapped(GuiGraphics g, Font font, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(10, width));
        int n = Math.min(lines.size(), maxLines);
        for (int i = 0; i < n; i++) {
            g.drawString(font, lines.get(i), x, y, color, false);
            y += 10;
        }
        if (lines.size() > maxLines) g.drawString(font, "…", x + width - 6, y - 10, color, false);
        return y;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
