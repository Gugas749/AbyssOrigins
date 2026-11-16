package com.gugas749.abyssorigins.client.vfx;

import com.gugas749.abyssorigins.api.Ascension;
import com.gugas749.abyssorigins.api.Ascension.Motif;
import com.gugas749.abyssorigins.api.Ascension.Timeline;
import com.gugas749.abysscore.api.vfx.client.VfxCanvas;
import com.gugas749.abysscore.api.vfx.client.VfxInstance;
import com.gugas749.abysscore.api.vfx.VfxSpec;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static com.gugas749.abysscore.api.vfx.client.VfxCanvas.UP;

/**
 * 3D effect of an origin ascension ceremony, on the stage's {@link Timeline}:
 *
 *  - gather (0..burst): a glow and an energy shell around the player, and the motif's own gathering:
 *    RADIANT light streams and a rising column; BLOOD a crimson whirlwind; HELLFIRE a ring of flames and
 *    ground cracks; DIVINE beams of light from the sky; VOID dark streams and lensing rings; SHADOW smoke
 *    crawling in over the ground and winding up the body; SOVEREIGN three orbits like an atom. Just before
 *    the burst everything contracts into the player;
 *  - burst: flash, rays, an expanding shell (dark for VOID and SHADOW), two ground rings and sparks;
 *  - afterwards: a halo over the head (Noble) or a crown (Lord) until the ceremony ends, and rising embers.
 *
 * spec: follow = the player, pos = their feet at the start (the ground), scale = stage, extra = motif ordinal.
 */
public final class AscensionEffect extends VfxInstance {

    private static final int WHITE = 0xFFFFFF;

    private final Motif motif;
    private final Timeline tl;
    private final boolean lord;
    private final int streams;
    private final double[] angle, phase;

    public AscensionEffect(VfxSpec s) {
        super(s, s.duration());
        Motif[] all = Motif.values();
        motif = all[Mth.clamp(Math.round(s.extra()), 0, all.length - 1)];
        int stage = Math.round(s.scale());
        tl = Ascension.timeline(stage);
        lord = stage >= Ascension.LORD;
        streams = lord ? 18 : 12;
        angle = new double[streams];
        phase = new double[streams];
        for (int i = 0; i < streams; i++) {
            angle[i] = Mth.TWO_PI * i / streams + rand.nextDouble() * 0.4;
            phase[i] = rand.nextDouble();
        }
    }

    @Override
    public double cullDistance() { return 160; }

    private double k() { return lord ? 1.45 : 1.0; }

    private boolean dark() { return motif == Motif.VOID || motif == Motif.SHADOW; }

    private Vec3 feet(float pt) { return anchor(pt); }

    private Vec3 chest(float pt) { return anchor(pt).add(0, 1.0, 0); }

    private Vec3 head(float pt) {
        Entity e = followed();
        return anchor(pt).add(0, (e != null ? e.getBbHeight() : 1.8) + 0.32, 0);
    }

    // ---- Particles ----------------------------------------------------------------------------

    @Override
    protected void onTick() {
        Vec3 chest = chest(0);
        double k = k();
        int burst = tl.burst();
        if (age < burst) {
            int n = lord ? 3 : 2;
            for (int i = 0; i < n; i++) {
                Vec3 at = switch (motif) {
                    case HELLFIRE -> spec.pos().add(rand.nextGaussian() * 2.5 * k, 0.2, rand.nextGaussian() * 2.5 * k);
                    case DIVINE   -> chest.add(rand.nextGaussian() * 2, 5 + rand.nextDouble() * 5, rand.nextGaussian() * 2);
                    default       -> chest.add(new Vec3(rand.nextGaussian(), rand.nextGaussian() * 0.6, rand.nextGaussian())
                                        .normalize().scale((3 + rand.nextDouble() * 3) * k));
                };
                spawnSparks(at, 1, 0.01, chest.subtract(at).scale(0.12), color(), color2(), 20, 0);
            }
        } else if (age == burst) {
            spawnSparks(chest, lord ? 220 : 140, 0.5 * k, Vec3.ZERO, color2(), WHITE, 36, 0.01);
        } else if (age < burst + 60) {
            for (int i = 0; i < 4; i++) {
                double ang = rand.nextDouble() * Mth.TWO_PI, d = Math.sqrt(rand.nextDouble()) * 6 * k;
                Vec3 at = spec.pos().add(Math.cos(ang) * d, rand.nextDouble() * 2, Math.sin(ang) * d);
                spawnSparks(at, 1, 0.02, new Vec3(0, 0.06 + rand.nextDouble() * 0.08, 0), color(), color2(), 36, -0.002);
            }
        }
    }

    // ---- Drawing ------------------------------------------------------------------------------

    @Override
    protected void draw(VfxCanvas c, float t, float pt) {
        float a = age + pt;
        if (a < tl.burst() + 6) drawGather(c, a, pt);
        if (a >= tl.burst()) drawBurst(c, a - tl.burst(), pt);
        drawMark(c, a, pt);
    }

    private void drawGather(VfxCanvas c, float a, float pt) {
        int burst = tl.burst();
        float g = smooth(a / burst);
        float fade = smooth(a / 12f) * (1 - smooth((a - burst) / 6f));
        if (fade <= 0.004f) return;
        float ph = seconds(pt);
        double k = k();
        Vec3 chest = chest(pt);
        boolean fp = isLocalFirstPerson();

        if (!fp) {
            c.glowDisc(chest, (0.8 + 1.4 * g) * k, color(), fade * (0.25f + 0.55f * g) * nearFade(c, chest, 3));
            c.sphere(dark() ? c.voidMatter : c.field, chest, (1.15 + 0.08 * Mth.sin(ph * 6)) * k, 10, 20, color(),
                fade * (dark() ? 0.35f : 0.2f) * g, 2.6f, false, ph);
        }

        switch (motif) {
            case RADIANT -> {
                converge(c, c.band, chest, 7 * k, 4, a, fade * g, color2());
                c.wall(c.band, spec.pos(), 1.1 * k, (1.5 + 4.5 * g) * k, 32, color(), fade * 0.5f * g, 0, ph);
                c.ring(c.band, spec.pos().add(0, 0.06, 0), UP, 1.7 * k, 0.1, 48, color2(), fade * g, ph);
            }
            case BLOOD -> whirlwind(c, a, pt, fade, g);
            case HELLFIRE -> {
                flames(c, ph, fade, g);
                cracks(c, ph, fade, g);
                converge(c, c.band, chest, 5 * k, -1, a, fade * g * 0.8f, color2());
            }
            case DIVINE -> {
                beams(c, chest, ph, fade, g);
                if (!fp) c.ring(c.band, head(pt), UP, 0.42 * g, 0.06, 32, color2(), fade * g, ph);
            }
            case VOID -> {
                converge(c, c.voidMatter, chest, 7 * k, 3, a, fade * g, color());
                for (int i = 0; i < 3; i++) {
                    Vec3 n = new Vec3(Math.sin(ph * 0.7 + i * 2.1), 1.4, Math.cos(ph * 0.7 + i * 2.1)).normalize();
                    c.ring(c.band, chest, n, (1.6 + i * 0.55) * k * (1.25 - 0.35 * g), 0.06, 56, color2(), fade * 0.75f * g, ph * 2 + i);
                }
            }
            case SHADOW -> smoke(c, a, pt, fade, g);
            case SOVEREIGN -> {
                converge(c, c.band, chest, 7 * k, 3, a, fade * g * 0.7f, color());
                for (int i = 0; i < 3; i++) {
                    double tilt = Math.PI / 3 * i + ph * 0.25;
                    Vec3 n = new Vec3(Math.cos(tilt) * 0.85, 0.55, Math.sin(tilt) * 0.85).normalize();
                    double r = (0.9 + 0.9 * g) * k;
                    c.ring(c.band, chest, n, r, 0.06, 64, color2(), fade * g, ph * 2);
                    Vec3 e = chest.add(VfxCanvas.onCircle(VfxCanvas.basis(n), ph * (3 + i)).scale(r));
                    c.glowDisc(e, 0.25 * k, WHITE, fade * g);
                }
            }
        }

        // The last moments before the burst: rings collapse into the player
        float pre = smooth((a - (burst - 12)) / 12f) * (1 - smooth((a - burst) / 3f));
        if (pre > 0.004f) {
            for (int i = 0; i < 3; i++) {
                double r = (4.2 * (1 - pre) + 0.25) * k * (1 + i * 0.35);
                c.ring(c.band, chest, c.camLook, r, 0.12, 48, i == 0 ? WHITE : color2(), pre * 0.9f, ph * 3);
            }
            if (!fp) c.glowDisc(chest, 2.6 * k * pre, WHITE, pre * nearFade(c, chest, 3));
        }
    }

    /** Streams spiralling in from {@code r0} away to {@code target}, each with a pulse running inward. */
    private void converge(VfxCanvas c, VertexConsumer vc, Vec3 target, double r0, double h0, float a, float alpha, int pulse) {
        if (alpha <= 0.004f) return;
        int n = 16, m = 6;
        Vec3[] pts = new Vec3[n];
        float[] w = new float[n], al = new float[n];
        for (int i = 0; i < streams; i++) {
            double h = h0 < 0 ? -1.0 : h0 * (phase[i] - 0.35);
            for (int j = 0; j < n; j++) {
                double f = j / (double) (n - 1);
                pts[j] = streamPoint(target, r0, h, i, f, a);
                w[j] = (float) (0.05 + 0.12 * f);
                al[j] = alpha * (float) (0.15 + 0.35 * f);
            }
            c.ribbon(vc, pts, w, al, color(), seconds(0) + i, 0.4f);
            double s = (a * 0.03 + phase[i]) % 1.0;
            Vec3[] seg = new Vec3[m];
            float[] sw = new float[m], sa = new float[m];
            for (int j = 0; j < m; j++) {
                double f = j / (double) (m - 1);
                seg[j] = streamPoint(target, r0, h, i, Mth.clamp(s + (f - 0.5) * 0.2, 0, 1), a);
                float bell = (float) Math.sin(Math.PI * f);
                sw[j] = 0.06f + 0.18f * bell;
                sa[j] = alpha * bell;
            }
            c.ribbon(c.band, seg, sw, sa, pulse, a * 0.2f + i, 0.5f);
        }
    }

    private Vec3 streamPoint(Vec3 target, double r0, double h, int i, double f, float a) {
        double ang = angle[i] + f * 2.4 + a * 0.015;
        double rad = r0 * Math.pow(1 - f, 1.15);
        return target.add(Math.cos(ang) * rad, h * (1 - f), Math.sin(ang) * rad);
    }

    /** BLOOD: crimson ribbons whirling from the ground up around the floating body. */
    private void whirlwind(VfxCanvas c, float a, float pt, float fade, float g) {
        double k = k();
        Vec3 ground = spec.pos(), top = head(pt).add(0, 0.6, 0);
        int n = 20;
        for (int i = 0; i < streams; i++) {
            Vec3[] pts = new Vec3[n];
            float[] w = new float[n], wc = new float[n], al = new float[n];
            for (int j = 0; j < n; j++) {
                float f = j / (float) (n - 1);
                double ang = angle[i] + f * Math.PI * 3 + a * (0.1 + 0.05 * phase[i]);
                double rad = (0.8 + 1.6 * (1 - f)) * k * (0.75 + 0.25 * g);
                double y = Mth.lerp(f, ground.y, top.y);
                pts[j] = new Vec3(ground.x + Math.cos(ang) * rad, y, ground.z + Math.sin(ang) * rad);
                w[j] = (float) (0.2 * k * (1 - f * 0.5));
                wc[j] = w[j] * 0.3f;
                al[j] = fade * (0.25f + 0.7f * g) * Mth.sin((float) Math.PI * f);
            }
            c.ribbon(c.band, pts, w, al, color(), -seconds(pt) * 3 + i, 0.3f);
            c.ribbon(c.band, pts, wc, al, color2(), -seconds(pt) * 4 + i, 0.3f);
        }
        c.ring(c.band, ground.add(0, 0.06, 0), UP, 2.4 * k, 0.5, 48, color(), fade * g, seconds(pt));
    }

    /** HELLFIRE: a ring of flames rising from the ground around the player. */
    private void flames(VfxCanvas c, float ph, float fade, float g) {
        double k = k(), r = 3.2 * k;
        int count = lord ? 22 : 16, n = 7;
        for (int i = 0; i < count; i++) {
            double ang = Mth.TWO_PI * i / count + ph * 0.3;
            Vec3 base = spec.pos().add(Math.cos(ang) * r, 0.05, Math.sin(ang) * r);
            double flick = 0.7 + 0.3 * Math.sin(ph * 11 + i * 1.7);
            double h = (0.6 + 2.2 * g) * flick * k;
            Vec3[] pts = new Vec3[n];
            float[] w = new float[n], wc = new float[n], al = new float[n];
            for (int j = 0; j < n; j++) {
                float f = j / (float) (n - 1);
                double sway = Math.sin(ph * 7 + i + f * 3) * 0.15 * f;
                pts[j] = base.add(sway, h * f, sway * 0.6);
                w[j] = (float) (0.55 * k * Math.pow(1 - f, 0.8)) + 0.02f;
                wc[j] = w[j] * 0.35f;
                al[j] = fade * (1 - f * 0.6f);
            }
            c.ribbon(c.band, pts, w, al, color(), -ph * 3 + i, 0.3f);
            c.ribbon(c.band, pts, wc, al, color2(), -ph * 4 + i, 0.3f);
        }
        c.ring(c.band, spec.pos().add(0, 0.05, 0), UP, r, 0.5 * k, 64, color(), fade * 0.8f, ph);
    }

    /** HELLFIRE: glowing cracks running out over the ground. */
    private void cracks(VfxCanvas c, float ph, float fade, float g) {
        double len = 6 * k() * g;
        if (len < 0.2) return;
        int count = lord ? 11 : 8, n = 7;
        for (int i = 0; i < count; i++) {
            double ang = angle[i % streams] * 1.7 + i;
            Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang)), side = new Vec3(-dir.z, 0, dir.x);
            Vec3[] pts = new Vec3[n];
            float[] w = new float[n], al = new float[n];
            for (int j = 0; j < n; j++) {
                float f = j / (float) (n - 1);
                double zig = (j % 2 == 0 ? 1 : -1) * 0.25 * f * (0.5 + phase[i % streams]);
                pts[j] = spec.pos().add(dir.scale(0.6 + len * f)).add(side.scale(zig)).add(0, 0.04, 0);
                w[j] = 0.16f * (1 - f) + 0.03f;
                al[j] = fade * (0.9f - 0.5f * f);
            }
            c.flatRibbon(c.band, pts, w, al, UP, color2(), ph * 2, 0.4f);
        }
    }

    /** DIVINE: beams of light falling from the sky onto the player, drawing in as the charge grows. */
    private void beams(VfxCanvas c, Vec3 chest, float ph, float fade, float g) {
        double k = k();
        int count = lord ? 8 : 6;
        for (int i = 0; i < count; i++) {
            double ang = Mth.TWO_PI * i / count + ph * 0.4;
            double rad = 2.8 * k * (1 - g * 0.8);
            Vec3 top = chest.add(Math.cos(ang) * rad, 24, Math.sin(ang) * rad);
            Vec3 mid = chest.add(Math.cos(ang) * rad * 0.7, 6, Math.sin(ang) * rad * 0.7);
            Vec3 bottom = chest.add(Math.cos(ang) * 0.3, 0, Math.sin(ang) * 0.3);
            c.ribbon(c.band, new Vec3[]{ top, mid, bottom }, new float[]{ 0.05f, (float) (0.24 * k), 0.1f },
                new float[]{ 0f, fade * 0.8f, fade * 0.6f }, color2(), -ph * 2 + i, 0.3f);
        }
    }

    /** SHADOW: smoke crawling in over the ground, then winding up around the body. */
    private void smoke(VfxCanvas c, float a, float pt, float fade, float g) {
        double k = k();
        Vec3 ground = spec.pos().add(0, 0.05, 0), feet = feet(pt);
        int n = 14;
        for (int i = 0; i < streams; i++) {
            Vec3[] pts = new Vec3[n];
            float[] w = new float[n], al = new float[n];
            for (int j = 0; j < n; j++) {
                float f = j / (float) (n - 1);
                double ang = angle[i] + f * 3.0 + a * 0.02;
                double rad = 7 * k * (1 - f) + 0.4;
                pts[j] = ground.add(Math.cos(ang) * rad, 0, Math.sin(ang) * rad);
                w[j] = (float) (0.5 * k * (0.4 + f));
                al[j] = fade * (0.3f + 0.6f * f) * g;
            }
            c.flatRibbon(c.voidMatter, pts, w, al, UP, color(), seconds(pt) + i, 0.3f);
            if (i % 2 == 1) continue;
            Vec3[] up = new Vec3[n];
            float[] uw = new float[n], ua = new float[n];
            for (int j = 0; j < n; j++) {
                float f = j / (float) (n - 1);
                double ang = angle[i] - f * Math.PI * 2 - a * 0.08;
                double y = Mth.lerp(f, ground.y, feet.y + 2.3);
                up[j] = new Vec3(feet.x + Math.cos(ang) * 0.7 * k, y, feet.z + Math.sin(ang) * 0.7 * k);
                uw[j] = (float) (0.3 * k * (1 - f * 0.7));
                ua[j] = fade * g * Mth.sin((float) Math.PI * f);
            }
            c.ribbon(c.voidMatter, up, uw, ua, color(), -seconds(pt) * 2 + i, 0.3f);
        }
    }

    private void drawBurst(VfxCanvas c, float d, float pt) {
        if (d > 50) return;
        double k = k();
        float ph = seconds(pt);
        Vec3 chest = chest(pt), ground = spec.pos();
        float flash = 1 - smooth(d / 10f);
        c.glowDisc(chest, (3 + 6 * easeOut(d / 6f)) * k, WHITE, flash * nearFade(c, chest, 6));
        c.rays(chest, (6 + 10 * easeOut(d / 8f)) * k, 0.25 * k, 16, ph * 0.5, color2(), 1 - smooth(d / 18f));
        float shell = 1 - smooth(d / 30f);
        c.sphere(dark() ? c.voidMatter : c.field, chest, 11 * k * easeOut(d / 22f), 14, 28, color(), shell * 0.7f, 2f, false, ph);
        c.ring(c.band, ground.add(0, 0.1, 0), UP, 14 * k * easeOut(d / 26f), 0.9 * (1 - d / 50f), 96, color2(), 1 - smooth(d / 34f), ph);
        if (d > 4)
            c.ring(dark() ? c.voidMatter : c.band, ground.add(0, 0.12, 0), UP, 10 * k * easeOut((d - 4) / 26f), 1.4 * (1 - d / 50f),
                96, color(), 1 - smooth((d - 4) / 30f), ph);
    }

    /** The halo (Noble) or crown (Lord) over the head, from the burst to the end. */
    private void drawMark(VfxCanvas c, float a, float pt) {
        float m = smooth((a - tl.burst() + 2) / 10f) * (1 - smooth((a - (lifetime - 15)) / 15f));
        if (m <= 0.004f || isLocalFirstPerson()) return;
        float ph = seconds(pt);
        Vec3 head = head(pt);
        c.glowDisc(head, 0.6, color(), m * 0.4f);
        if (!lord) {
            c.ring(c.band, head, UP, 0.42, 0.07, 32, color2(), m, ph);
            return;
        }
        c.ring(c.band, head, UP, 0.36, 0.08, 32, color2(), m, ph);
        for (int i = 0; i < 8; i++) {
            double ang = ph * 0.6 + Mth.TWO_PI * i / 8;
            Vec3 base = head.add(Math.cos(ang) * 0.36, 0, Math.sin(ang) * 0.36);
            Vec3 tip = base.add(Math.cos(ang) * 0.05, i % 2 == 0 ? 0.34 : 0.2, Math.sin(ang) * 0.05);
            c.ribbon(c.band, new Vec3[]{ base, tip }, new float[]{ 0.1f, 0.01f }, new float[]{ m, m }, color2(), ph, 0.5f);
            c.diamond(c.glow, tip, 0.05, WHITE, m);
        }
    }
}
