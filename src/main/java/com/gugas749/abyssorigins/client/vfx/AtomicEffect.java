package com.gugas749.abyssorigins.client.vfx;

import com.gugas749.abysscore.api.vfx.client.VfxCanvas;
import com.gugas749.abysscore.api.vfx.client.VfxInstance;
import com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic;
import com.gugas749.abysscore.api.vfx.VfxSpec;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.gugas749.abysscore.api.vfx.client.VfxCanvas.UP;

/**
 * 3D effect of the Eminence's I Am Atomic, on the ability's timeline ({@link Atomic}):
 *
 *  - gather (0..CUT): a rune circle on the ground, energy streams spiralling in from far away into an atom
 *    above the caster (core + three electron orbits), and a thin beam from the raised scythe to the atom;
 *  - cut (CUT..DETONATE): a vertical slash of light through the atom, which splits into two halves;
 *  - explosion (DETONATE..end): white flash and rays, a hemisphere growing to the full radius, a ground
 *    wave, a rising column with a cap on top, and embers drifting up while it fades;
 *  - the whole time: a bubble over the blast area (a translucent hemisphere of the full radius with a ring
 *    on the ground), its surface crossed by meridian lines with light running up and down them.
 *
 * spec: pos = caster's feet, scale = blast radius, extra = atom height, color / color2 = violet / highlight.
 */
public final class AtomicEffect extends VfxInstance {

    private static final int STREAMS = 16;
    private static final int BUBBLE_LINES = 40;
    /** Highest point of the bubble's lines, as a fraction of a quarter turn (they stop short of the top). */
    private static final double LINE_TOP = 0.94;
    private static final int WHITE = 0xFFFFFF;

    private final double[] streamAngle = new double[STREAMS], streamRadius = new double[STREAMS],
        streamHeight = new double[STREAMS], streamPhase = new double[STREAMS];
    private final double[] linePhase = new double[BUBBLE_LINES];

    public AtomicEffect(VfxSpec s) {
        super(s, s.duration());
        for (int i = 0; i < STREAMS; i++) {
            streamAngle[i] = Mth.TWO_PI * i / STREAMS + rand.nextDouble() * 0.3;
            streamRadius[i] = s.scale() * (0.35 + rand.nextDouble() * 0.25);
            streamHeight[i] = rand.nextDouble() * 8 - 1;
            streamPhase[i] = rand.nextDouble();
        }
        for (int i = 0; i < BUBBLE_LINES; i++) linePhase[i] = rand.nextDouble();
    }

    /** Visible from anywhere around the bubble, not just within the default 128 blocks. */
    @Override
    public double cullDistance() { return Math.max(128, spec.scale() * 2.5); }

    private double radius() { return spec.scale(); }

    private Vec3 atom() { return spec.pos().add(0, spec.extra(), 0); }

    // ---- Particles ----------------------------------------------------------------------------

    @Override
    protected void onTick() {
        Vec3 atom = atom();
        double r = radius();
        if (age < Atomic.CUT) {
            // Sparks drawn in toward the atom (drag 0.88/tick: a velocity of 0.12 x distance arrives)
            for (int i = 0; i < 3; i++) {
                Vec3 dir = new Vec3(rand.nextGaussian(), rand.nextGaussian() * 0.5, rand.nextGaussian()).normalize();
                Vec3 at = atom.add(dir.scale(r * (0.15 + rand.nextDouble() * 0.3)));
                spawnSparks(at, 1, 0.01, atom.subtract(at).scale(0.12), color(), color2(), 20, 0);
            }
        } else if (age == Atomic.DETONATE) {
            spawnSparks(atom, 160, r * 0.045, Vec3.ZERO, color2(), WHITE, 40, 0.01);
        } else if (age > Atomic.DETONATE && age < Atomic.DETONATE + 70) {
            // Embers rising through the fading blast
            for (int i = 0; i < 10; i++) {
                double ang = rand.nextDouble() * Mth.TWO_PI, d = Math.sqrt(rand.nextDouble()) * r * 0.85;
                Vec3 at = spec.pos().add(Math.cos(ang) * d, rand.nextDouble() * r * 0.25, Math.sin(ang) * d);
                spawnSparks(at, 1, 0.03, new Vec3(0, 0.08 + rand.nextDouble() * 0.1, 0), color(), color2(), 40, -0.002);
            }
        }
    }

    // ---- Drawing ------------------------------------------------------------------------------

    @Override
    protected void draw(VfxCanvas c, float t, float pt) {
        float a = age + pt;
        float ph = seconds(pt);
        float life = Atomic.TOTAL - Atomic.DETONATE;
        float bubble = smooth(a / 16f) * (1 - smooth((a - Atomic.DETONATE - 40) / (life - 40)));
        drawBubble(c, a, ph, bubble);
        if (a < Atomic.DETONATE + 4) drawGather(c, a, ph);
        if (a >= Atomic.DETONATE) drawExplosion(c, a - Atomic.DETONATE, ph);
    }

    /** The bubble over the blast area, with light running up and down its meridian lines. */
    private void drawBubble(VfxCanvas c, float a, float ph, float alpha) {
        if (alpha <= 0.004f) return;
        Vec3 base = spec.pos();
        double rr = radius() * (0.92 + 0.08 * easeOut(a / 16f));
        boolean inside = c.cam.distanceTo(base) < rr;
        c.sphere(c.field, base, rr, 14, 48, color(), alpha * (inside ? 0.3f : 0.45f), 2.4f, true, ph * 0.3f);
        c.ring(c.band, base.add(0, 0.15, 0), UP, rr, 0.6 + rr * 0.008, 160, color2(), alpha, ph);

        float w = (float) (rr * 0.007);
        int n = 14, m = 6;
        for (int i = 0; i < BUBBLE_LINES; i++) {
            double phi = Mth.TWO_PI * i / BUBBLE_LINES;
            Vec3[] line = new Vec3[n];
            for (int j = 0; j < n; j++) line[j] = onBubble(base, rr, phi, j / (double) (n - 1));
            c.ribbon(c.band, line, VfxCanvas.fill(n, w * 0.6f), VfxCanvas.fill(n, alpha * 0.22f), color(), ph + i, 0.5f);
            // Two pulses per line: one runs up while the other runs down
            for (int k = 0; k < 2; k++) {
                double speed = 0.07 + 0.04 * linePhase[i];
                double s = 0.5 + 0.5 * Math.sin(a * speed + linePhase[i] * Mth.TWO_PI + k * Math.PI);
                Vec3[] seg = new Vec3[m];
                float[] sw = new float[m], sa = new float[m];
                for (int j = 0; j < m; j++) {
                    double f = j / (double) (m - 1);
                    seg[j] = onBubble(base, rr, phi, Mth.clamp(s + (f - 0.5) * 0.16, 0, 1));
                    float bell = (float) Math.sin(Math.PI * f);
                    sw[j] = w * (0.5f + 1.3f * bell);
                    sa[j] = alpha * 0.95f * bell;
                }
                c.ribbon(c.band, seg, sw, sa, k == 0 ? color2() : WHITE, ph * 4 + i, 0.5f);
            }
        }
    }

    /** A point on the bubble at longitude {@code phi}, {@code up} = 0 on the ground .. 1 at the lines' top. */
    private static Vec3 onBubble(Vec3 base, double r, double phi, double up) {
        double el = up * LINE_TOP * Math.PI / 2;
        return base.add(Math.cos(phi) * Math.cos(el) * r, Math.sin(el) * r, Math.sin(phi) * Math.cos(el) * r);
    }

    private void drawGather(VfxCanvas c, float a, float ph) {
        Vec3 base = spec.pos(), atom = atom();
        double r = radius();
        float charge = smooth(a / Atomic.SWING);
        float fadeIn = smooth(a / 10f);
        float gone = 1 - smooth((a - Atomic.DETONATE) / 4f);       // everything gives way to the flash

        // Rune circle under the caster
        Vec3 g = base.add(0, 0.06, 0);
        double sr = Math.min(r * 0.16, 9) * easeOut(a / 20f);
        float sa = 0.85f * fadeIn * gone;
        c.ring(c.band, g, UP, sr, 0.3, 96, color(), sa, ph * 2);
        c.ring(c.band, g, UP, sr * 0.72, 0.16, 80, color2(), sa * 0.8f, -ph * 3);
        c.ring(c.band, g, UP, sr * 1.9 * charge, 0.12, 128, color(), sa * 0.5f, ph);
        int points = 7;
        double rot = ph * 0.35;
        for (int k = 0; k < points; k++) {
            double a0 = rot + Mth.TWO_PI * k / points, a1 = rot + Mth.TWO_PI * ((k + 3) % points) / points;
            c.stroke(c.band, g.add(Math.cos(a0) * sr * 0.72, 0, Math.sin(a0) * sr * 0.72),
                g.add(Math.cos(a1) * sr * 0.72, 0, Math.sin(a1) * sr * 0.72), 0.12, UP, color2(), sa * 0.7f, 0, 2);
        }
        c.ring(c.glow, g, UP, sr * 0.4, sr, 48, color(), 0.15f * sa, 0);

        // Energy streams spiralling in
        if (a < Atomic.CUT + 2) {
            float sAlpha = fadeIn * (1 - smooth((a - Atomic.CUT + 4) / 6f));
            for (int i = 0; i < STREAMS; i++) drawStream(c, i, a, atom, sAlpha, ph);
        }

        // The atom
        float pulse = 1 + 0.08f * Mth.sin(a * 0.9f);
        double size = (0.25 + 1.25 * charge) * pulse * (a > Atomic.SWING ? 1 + 0.3 * smooth((a - Atomic.SWING) / 6f) : 1);
        float split = smooth((a - Atomic.CUT) / (float) (Atomic.DETONATE - Atomic.CUT));
        float coreA = fadeIn * gone;
        if (split <= 0) {
            c.glowDisc(atom, size * 3.2, color(), 0.55f * coreA);
            c.glowDisc(atom, size * 1.6, color2(), 0.9f * coreA);
            c.disc(c.band, atom, c.camLeft, c.camUp, size * 0.8, size * 0.8, 28, color2(), coreA, ph * 2);
            c.sphere(c.field, atom, size * 1.1, 8, 18, color(), 0.8f * coreA, 1.6f, false, ph);
        } else {
            // Cut in two: the halves fly apart sideways on screen
            for (int side = -1; side <= 1; side += 2) {
                Vec3 half = atom.add(c.camLeft.scale(side * size * (0.25 + 2.2 * split)));
                c.glowDisc(half, size * 2.2, color(), 0.6f * coreA);
                c.disc(c.band, half, c.camLeft, c.camUp, size * 0.45, size * 0.8, 20, color2(), coreA, ph * 2);
            }
            c.rays(atom, size * (4 + 10 * split), size * 0.12, 12, ph, WHITE, 0.8f * coreA * split);
        }
        // Electron orbits
        float orbitA = coreA * (1 - split);
        for (int k = 0; k < 3; k++) {
            double tilt = k * Math.PI / 3 + ph * 0.6;
            Vec3 n = new Vec3(Math.cos(tilt), 0.6 + 0.4 * Math.sin(ph * 0.3 + k), Math.sin(tilt)).normalize();
            double orbit = size * 2.4;
            c.ring(c.band, atom, n, orbit, 0.05 + 0.05 * charge, 56, color(), 0.9f * orbitA, ph * 3);
            Vec3 e = atom.add(VfxCanvas.onCircle(VfxCanvas.basis(n), ph * 5 + k * 2.1).scale(orbit));
            c.glowDisc(e, size * 0.5, color2(), orbitA);
            c.diamond(c.band, e, size * 0.18, WHITE, orbitA);
        }

        // Link from the raised scythe to the atom
        Vec3 from = base.add(0, 2.6, 0);
        if (atom.y > from.y) {
            float la = 0.6f * charge * coreA * (1 - split);
            c.ribbon(c.band, new Vec3[]{ from, from.lerp(atom, 0.5), atom }, new float[]{ 0.05f, 0.18f, 0.1f },
                new float[]{ 0f, la, la }, color2(), ph * 4, 1);
        }

        // The cut: a vertical slash of light through the atom, drawn top to bottom
        float cut = (a - (Atomic.CUT - 2)) / 3f;
        if (cut > 0) {
            float ca = 1 - smooth((a - Atomic.CUT) / (Atomic.DETONATE - Atomic.CUT + 2f));
            Vec3 top = atom.add(0, size * 5, 0), bottom = base.add(0, 0.3, 0);
            Vec3 end = top.lerp(bottom, Math.min(1, cut));
            Vec3 mid = top.lerp(end, 0.5);
            c.ribbon(c.band, new Vec3[]{ top, mid, end }, new float[]{ 0.1f, 0.7f, 0.25f },
                new float[]{ ca * 0.6f, ca, ca }, WHITE, ph * 6, 1);
            c.ribbon(c.glow, new Vec3[]{ top, mid, end }, new float[]{ 0.4f, 2.2f, 0.8f },
                new float[]{ 0f, ca * 0.5f, ca * 0.3f }, color2(), 0, 1);
        }
    }

    /** One stream: a ribbon segment travelling along a spiral from far away into the atom, on a loop. */
    private void drawStream(VfxCanvas c, int i, float a, Vec3 atom, float alpha, float ph) {
        double head = (a / 28.0 + streamPhase[i]) % 1.0;
        int n = 10;
        Vec3[] pts = new Vec3[n];
        float[] w = new float[n], al = new float[n];
        for (int j = 0; j < n; j++) {
            double f = j / (double) (n - 1);
            double s = Math.max(0, head - 0.3 * (1 - f));          // tail ... head
            double reach = Math.pow(1 - s, 1.4);
            double ang = streamAngle[i] + s * 2.6;
            pts[j] = atom.add(Math.cos(ang) * streamRadius[i] * reach,
                (spec.pos().y + streamHeight[i] - atom.y) * reach, Math.sin(ang) * streamRadius[i] * reach);
            w[j] = (float) (0.08 + 0.45 * f * (0.4 + reach));
            al[j] = (float) (alpha * f * (1 - Math.pow(s, 6)));
        }
        c.ribbon(c.band, pts, w, al, i % 3 == 0 ? color2() : color(), ph * 3 + i, 0.6f);
    }

    private void drawExplosion(VfxCanvas c, float d, float ph) {
        Vec3 base = spec.pos(), atom = atom();
        double r = radius();
        float life = Atomic.TOTAL - Atomic.DETONATE;
        float fade = 1 - smooth((d - 40) / (life - 40));

        // Flash and rays
        float flash = 1 - d / 14f;
        if (flash > 0) {
            c.glowDisc(atom, r * 0.6 * easeOut(d / 5f), WHITE, 0.75f * flash);
            c.glowDisc(atom, r * 0.25, color2(), flash);
            c.rays(atom, r * 1.1, r * 0.04, 18, ph * 0.5, WHITE, 0.8f * flash);
        }

        // Hemisphere growing to the full radius
        double dome = r * easeOut(Math.min(1f, d / 22f));
        boolean inside = c.cam.distanceTo(base) < dome;
        float domeA = fade * (inside ? 0.28f : 0.9f);
        c.sphere(c.field, base, dome, 12, 36, color(), domeA, 1.8f, true, ph * 0.5f);
        c.sphere(c.band, base, dome * 0.55, 8, 28, color2(), domeA * 0.6f * (1 - smooth(d / 50f)), 0f, true, ph);
        c.ring(c.band, base.add(0, 0.12, 0), UP, dome, 0.6 + r * 0.02, 128, color2(), fade, ph * 2);

        // Ground wave, running ahead of the dome
        float wk = Math.min(1f, d / 18f);
        double wave = r * 1.02 * easeOut(wk);
        float waveA = (1 - wk) * 0.9f;
        c.ring(c.band, base.add(0, 0.1, 0), UP, wave, 0.5 + r * 0.04 * (1 - wk), 160, WHITE, waveA, ph);
        c.wall(c.band, base, wave, r * 0.1 * (1 - wk), 128, color(), waveA * 0.6f, 0f, ph);

        // Column and cap
        double height = r * 0.8 * easeOut(Math.min(1f, d / 40f));
        int n = 12;
        Vec3[] pts = new Vec3[n];
        float[] w = new float[n], al = new float[n], wc = new float[n], ac = new float[n];
        for (int i = 0; i < n; i++) {
            float f = i / (float) (n - 1);
            pts[i] = base.add(0, height * f, 0);
            w[i] = (float) (r * (0.12 - 0.05 * f));
            al[i] = fade * (0.2f + 0.45f * f);          // fainter at the base, so the caster stays visible
            wc[i] = (float) (r * 0.03);
            ac[i] = fade * (0.25f + 0.5f * f);
        }
        double camDist = Math.hypot(c.cam.x - base.x, c.cam.z - base.z);
        if (camDist > r * 0.1) c.ribbon(c.band, pts, w, al, color(), ph * 2, 0.5f);
        c.ribbon(c.band, pts, wc, ac, color2(), -ph * 3, 0.5f);

        Vec3 top = base.add(0, height, 0);
        double cap = r * 0.3 * easeOut(Math.min(1f, d / 45f));
        for (int k = 0; k < 4; k++)
            c.ring(c.band, top.add(0, k * r * 0.03 - r * 0.04, 0), UP, cap * (1 - k * 0.14), r * 0.06, 72,
                k % 2 == 0 ? color() : color2(), fade * 0.8f, ph * (k % 2 == 0 ? 1 : -1));
        c.sphere(c.field, top, cap * 0.75, 8, 24, color(), fade * 0.7f, 1.4f, false, ph);
        c.glowDisc(top, cap * 1.4, color(), fade * 0.35f);
    }
}
