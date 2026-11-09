package com.gugas749.abyssorigins.client.vfx.effects;

import com.gugas749.abyssorigins.client.vfx.VfxCanvas;
import com.gugas749.abyssorigins.client.vfx.VfxInstance;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxSpec;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.gugas749.abyssorigins.client.vfx.VfxCanvas.UP;

/** Effects made of light-eating matter: vortexes, tendrils and rifts. */
public final class VoidEffects {

    private VoidEffects() {}

    // ---- Vortex -------------------------------------------------------------------------

    /**
     * A collapsing rift: spiral accretion arms drawn inward, tilted lensing rings and a
     * black core with a burning event horizon that always faces the viewer.
     */
    public static final class Vortex extends VfxInstance {
        public Vortex(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void onTick() {
            double r = spec.scale();
            for (int i = 0; i < 3; i++) {
                double ang = rand.nextDouble() * Mth.TWO_PI;
                Vec3 at = spec.pos().add(Math.cos(ang) * r, (rand.nextDouble() - 0.5) * 1.2, Math.sin(ang) * r);
                Vec3 in = spec.pos().subtract(at).scale(0.09);
                spawnSparks(at, 1, 0.0, in, color(), color2(), 14, 0.0);
            }
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 ctr = spec.pos();
            float env = envelope(t, Math.min(0.3f, 8f / lifetime), Math.min(0.3f, 5f / lifetime));
            float sec = seconds(pt);
            double rot = sec * 2.6;
            double big = spec.scale();

            // Accretion arms — dark backing then glowing filament
            for (int arm = 0; arm < 3; arm++) {
                int n = 40;
                Vec3[] pts = new Vec3[n];
                float[] wDark = new float[n], wGlow = new float[n], a = new float[n];
                for (int i = 0; i < n; i++) {
                    float f = i / (float) (n - 1);
                    double r = (big * (1 - f) + 0.6) * env;
                    double ang = arm * Mth.TWO_PI / 3 + f * Math.PI * 3 + rot;
                    pts[i] = ctr.add(Math.cos(ang) * r, Math.sin(f * Math.PI * 2 + rot) * 0.08, Math.sin(ang) * r);
                    wDark[i] = (0.25f + 0.65f * f) * env;
                    wGlow[i] = (0.1f + 0.3f * f) * env;
                    a[i] = env * (0.2f + 0.8f * f);
                }
                c.flatRibbon(c.voidMatter, pts, wDark, a, UP, color2(), (float) rot, 0.2f);
                c.flatRibbon(c.band, pts, wGlow, a, UP, color(), -sec * 3, 0.2f);
            }

            for (int k = 0; k < 3; k++) {
                double tilt = rot * 0.35 + k * 2.1;
                Vec3 n = new Vec3(Math.sin(tilt) * 0.6, 1, Math.cos(tilt) * 0.6);
                c.ring(c.band, ctr, n, (1.3 + 0.4 * k) * env, 0.07, 48, color2(), env * 0.55f, sec * 2);
            }

            float pulse = 1 + 0.07f * Mth.sin(sec * 9);
            double core = 0.95 * env * pulse;
            c.disc(c.voidMatter, ctr, c.camLeft, c.camUp, core * 1.25, core * 1.25, 36, color(), env, sec);
            c.ring(c.band, ctr, c.camLook, core * 1.18, 0.16, 48, color(), env, sec * 3);
            c.glowDisc(ctr, big * 0.55, color(), env * 0.35f);
        }
    }

    // ---- Tendrils -----------------------------------------------------------------------

    /** Shadow tendrils that burst from the ground, curl inward around the target, then sink. */
    public static final class Tendrils extends VfxInstance {
        private final double[] offsets;

        public Tendrils(VfxSpec s) {
            super(s, s.duration());
            int count = s.extra() > 0 ? (int) s.extra() : 5;
            offsets = new double[count];
            for (int i = 0; i < count; i++) offsets[i] = rand.nextDouble() * 0.6;
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 base = anchor(pt);
            float grow = easeOut(t / 0.28f);
            float sink = t > 0.65f ? smooth((1 - t) / 0.35f) : 1f;
            float len = grow * sink;
            float sec = seconds(pt);
            double h = spec.scale();
            int count = offsets.length;

            c.ring(c.voidMatter, base.add(0, 0.04, 0), UP, 0.95, 1.4, 40, color(), 0.85f * sink, sec);

            for (int k = 0; k < count; k++) {
                double rootAng = Mth.TWO_PI * k / count + offsets[k] * 3;
                Vec3 root = base.add(Math.cos(rootAng) * 1.05, 0, Math.sin(rootAng) * 1.05);
                int n = 14;
                Vec3[] pts = new Vec3[n];
                float[] w = new float[n], wc = new float[n], a = new float[n];
                for (int i = 0; i < n; i++) {
                    float f = i / (float) (n - 1) * len;
                    double inward = 0.75 * Math.sin(f * Math.PI * 0.6);
                    double curl = Math.sin(f * Math.PI * 1.6 + sec * 3 + k) * 0.28 * f;
                    double ang = rootAng + curl;
                    double r = 1.05 * (1 - inward);
                    pts[i] = base.add(Math.cos(ang) * r, h * f * (0.9 + offsets[k] * 0.3), Math.sin(ang) * r);
                    float fi = i / (float) (n - 1);
                    w[i] = 0.36f * (1 - fi) + 0.02f;
                    wc[i] = w[i] * 0.22f;
                    a[i] = 0.95f * sink;
                }
                c.ribbon(c.voidMatter, pts, w, a, color(), sec + k, 0.25f);
                c.ribbon(c.band, pts, wc, a, color2(), -sec * 2 + k, 0.25f);
            }
        }
    }

    // ---- Rift ---------------------------------------------------------------------------

    /**
     * A vertical portal slit — tears open, swallows light, and seals itself. With {@link Vfx#SUSTAIN}
     * it tears open just as fast but stays open for the whole duration (extra = 1: already open),
     * sealing over the last {@link #SEAL} ticks, as soon as it is stopped, or when the server's
     * re-broadcasts stop arriving for {@link #ORPHANED} ticks (it was closed out of earshot).
     */
    public static final class Rift extends VfxInstance {
        /** Opening / sealing time of a sustained rift: the same speed as the 40-tick one-shot. */
        private static final float TEAR = 0.22f * 40, SEAL = 0.45f * 40;
        private static final int ORPHANED = 60;
        private final boolean sustain, preOpened;
        private int sealing = -1, lastHeard = 0;

        public Rift(VfxSpec s) {
            super(s, s.duration());
            sustain = s.has(Vfx.SUSTAIN);
            preOpened = sustain && s.extra() == 1f;
            if (!preOpened) spawnSparks(s.pos(), 14, 0.2, Vec3.ZERO, s.color(), s.color2(), 16, 0.0);
        }

        @Override
        public boolean release() {
            if (!sustain) return false;
            if (sealing < 0) sealing = age;
            return true;
        }

        @Override
        public void refresh() { lastHeard = age; }

        @Override
        public boolean tick() {
            if (sustain && sealing < 0 && age - lastHeard > ORPHANED) sealing = age;
            if (sealing >= 0 && age - sealing >= SEAL && age < lifetime) age = lifetime - 1;
            return super.tick();
        }

        private float openness(float t, float pt) {
            if (!sustain) return envelope(t, 0.22f, 0.45f);
            float a = age + pt;
            float in = preOpened ? 1f : smooth(a / TEAR);
            float out = sealing >= 0 ? smooth(1f - (a - sealing) / SEAL) : smooth((lifetime - a) / SEAL);
            return Math.min(in, out);
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 ctr = spec.pos();
            Vec3 facing = VfxCanvas.horizontal(spec.dir());
            Vec3 right = new Vec3(-facing.z, 0, facing.x);
            float open = openness(t, pt);
            float sec = seconds(pt);
            double h = (spec.scale() > 0 ? spec.scale() : 2.2) / 2;
            double rx = 0.55 * open, ry = h * (0.45 + 0.55 * open);

            c.disc(c.voidMatter, ctr, right, UP, rx, ry, 36, color(), open, sec);
            int n = 48;
            Vec3[] rim = VfxCanvas.ellipse(ctr, right, UP, rx, ry, 0, Mth.TWO_PI, n);
            c.flatRibbon(c.band, rim, VfxCanvas.fill(n, 0.14f), VfxCanvas.fill(n, open), facing, color(), sec * 3, 8f / n);
            Vec3[] outer = VfxCanvas.ellipse(ctr, right, UP, rx * 1.5 + 0.15, ry * 1.15, 0, Mth.TWO_PI, n);
            c.flatRibbon(c.band, outer, VfxCanvas.fill(n, 0.06f), VfxCanvas.fill(n, open * 0.5f), facing, color2(), -sec * 2, 8f / n);
            c.glowDisc(ctr, ry * 1.2, color(), open * 0.35f);
        }
    }
}
