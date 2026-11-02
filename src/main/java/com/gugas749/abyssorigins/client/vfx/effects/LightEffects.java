package com.gugas749.abyssorigins.client.vfx.effects;

import com.gugas749.abyssorigins.client.vfx.VfxCanvas;
import com.gugas749.abyssorigins.client.vfx.VfxInstance;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxSpec;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.gugas749.abyssorigins.client.vfx.VfxCanvas.UP;

/** Radiant effects: shockwaves, domes, pillars, bursts, sigils, helixes, auras. */
public final class LightEffects {

    private LightEffects() {}

    // ---- Shockwave ----------------------------------------------------------------------

    /** A burning ring races outward along the ground, trailing a fading wall of light. */
    public static final class Shockwave extends VfxInstance {
        public Shockwave(VfxSpec s) {
            super(s, s.duration());
            int n = (int) (14 + s.scale() * 4);
            for (int i = 0; i < n; i++) {
                double ang = rand.nextDouble() * Mth.TWO_PI;
                Vec3 out = new Vec3(Math.cos(ang), 0.25 + rand.nextDouble() * 0.35, Math.sin(ang));
                spawnSparks(s.pos().add(0, 0.3, 0), 1, 0.05, out.scale(0.25 + s.scale() * 0.06), s.color(), s.color2(), 22, 0.012);
            }
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 ctr = anchor(pt).add(0, 0.07, 0);
            float big = spec.scale();
            float e = easeOut(t);
            float a = (float) Math.pow(1 - t, 1.3);
            double r = big * e;
            float ph = seconds(pt) * 1.5f;

            c.ring(c.band, ctr, UP, r, 0.35 + big * 0.14 * (1 - t), 72, color(), a, ph);
            c.ring(c.band, ctr, UP, r * 0.74, 0.16 + big * 0.05 * (1 - t), 64, color2(), a * 0.75f, -ph);
            c.wall(c.band, ctr, r, big * 0.4 * (1 - e * 0.55), 56, color(), a * 0.55f, 0f, ph);
            c.ring(c.glow, ctr.add(0, 0.02, 0), UP, r * 0.55, r * 0.9, 48, color(), a * 0.18f, 0);

            float flash = 1 - t * 3.5f;
            if (flash > 0) {
                c.glowDisc(ctr.add(0, 0.6, 0), big * 0.75, color2(), flash * 0.85f);
                c.disc(c.band, ctr.add(0, 0.05, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), big * 0.35 * (1 + t * 2), big * 0.35 * (1 + t * 2), 32, color2(), flash, ph);
            }
        }
    }

    // ---- Dome ------------------------------------------------------------------------------

    /** A fresnel energy shell with a lattice and scan-lines; grows in with a slight overshoot. */
    public static final class Dome extends VfxInstance {
        public Dome(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            float age = this.age + pt;
            float grow = age < 7 ? easeOutBack(age / 7f) : 1f;
            float env = envelope(t, Math.min(0.3f, 4f / lifetime), Math.min(0.35f, 8f / lifetime));
            boolean half = spec.has(Vfx.HALF);
            Vec3 base = anchor(pt);
            Vec3 ctr = half ? base.add(0, 0.05, 0) : (spec.follow() >= 0 ? base.add(0, 1.0, 0) : base);
            double r = spec.scale() * grow;
            float ph = seconds(pt) * 0.4f;
            // Seen from inside (e.g. your own halo), the shell would tint the whole screen
            if (c.cam.distanceTo(ctr) < r) env *= half ? 0.6f : 0.25f;

            if (spec.has(Vfx.VOID)) {
                c.sphere(c.voidMatter, ctr, r, 12, 24, color(), 0.7f * env, 0f, half, ph);
                c.sphere(c.field, ctr, r * 1.02, 12, 24, color2(), 0.6f * env, 3f, half, ph);
                return;
            }
            c.sphere(c.field, ctr, r, 14, 30, color(), 0.9f * env, 2.2f, half, ph);
            // Two slowly precessing great circles give the shell a sense of structure
            for (int k = 0; k < 2; k++) {
                double tilt = seconds(pt) * 0.7 + k * Math.PI / 2;
                Vec3 n = new Vec3(Math.cos(tilt), half ? 0 : 0.3, Math.sin(tilt));
                if (!half) c.ring(c.band, ctr, n, r, 0.06, 64, color2(), 0.5f * env, ph * 3);
            }
            c.ring(c.band, base.add(0, 0.06, 0), UP, r, 0.28, 72, color2(), 0.85f * env, ph * 4);
            c.ring(c.glow, base.add(0, 0.05, 0), UP, r * 0.5, r, 48, color(), 0.12f * env, 0);
        }

        private static float easeOutBack(float t) {
            float c1 = 1.70158f, c3 = c1 + 1;
            float u = t - 1;
            return 1 + c3 * u * u * u + c1 * u * u;
        }
    }

    // ---- Pillar ----------------------------------------------------------------------------

    /** A column of light: wide plasma sheath, white core, spiralling strands, ground halo. */
    public static final class Pillar extends VfxInstance {
        public Pillar(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void onTick() {
            if (age % 2 == 0) {
                double r = radius();
                double ang = rand.nextDouble() * Mth.TWO_PI;
                Vec3 at = spec.pos().add(Math.cos(ang) * r, rand.nextDouble() * 0.5, Math.sin(ang) * r);
                spawnSparks(at, 1, 0.02, new Vec3(0, 0.22 + rand.nextDouble() * 0.2, 0), color(), color2(), 26, -0.004);
            }
        }

        private double radius() { return spec.extra() > 0 ? spec.extra() : 1.0; }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            float env = envelope(t, 0.12f, 0.4f);
            Vec3 base = anchor(pt);
            double h = spec.scale() * (0.35 + 0.65 * easeOut(t / 0.2f));
            double r = radius();
            float ph = seconds(pt);

            int n = 10;
            Vec3[] pts = new Vec3[n];
            float[] w = new float[n], a = new float[n], wc = new float[n], ac = new float[n];
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                pts[i] = base.add(0, h * f, 0);
                w[i] = (float) (r * 2 * (1.0 - f * 0.25)) * env;
                a[i] = env * (1 - f) * 0.9f;
                wc[i] = (float) (r * 0.45) * env;
                ac[i] = env * (1 - f * f);
            }
            // Standing inside the column: the camera-facing sheath would cover the screen, so
            // only the strands and ground halo are drawn.
            double camDist = Math.hypot(c.cam.x - base.x, c.cam.z - base.z);
            if (camDist > r * 1.3) {
                c.ribbon(c.band, pts, w, a, color(), ph * -2, 0.8f);
                c.ribbon(c.band, pts, wc, ac, color2(), ph * -3, 0.8f);
            }

            for (int s = 0; s < 3; s++) {
                int m = 24;
                Vec3[] sp = new Vec3[m];
                float[] sw = new float[m], sa = new float[m];
                for (int i = 0; i < m; i++) {
                    float f = i / (float) (m - 1);
                    double ang = s * Mth.TWO_PI / 3 + f * 5 + ph * 3;
                    double rr = r * 0.9 * (1 - f * 0.4);
                    sp[i] = base.add(Math.cos(ang) * rr, h * f, Math.sin(ang) * rr);
                    sw[i] = 0.14f * (1 - f * 0.7f);
                    sa[i] = env * (1 - f);
                }
                c.ribbon(c.band, sp, sw, sa, color2(), ph * 2, 0.35f);
            }

            c.ring(c.band, base.add(0, 0.06, 0), UP, r * (1.3 + t * 1.6), 0.22, 56, color(), env * (1 - t), ph);
            c.glowDisc(base.add(0, 0.4, 0), r * 2.2, color(), 0.5f * env * nearFade(c, base.add(0, 0.4, 0), r * 2));
        }
    }

    // ---- Burst -----------------------------------------------------------------------------

    /** Impact: flash, rotating star rays, an expanding shell and a ring flung toward the viewer. */
    public static final class Burst extends VfxInstance {
        public Burst(VfxSpec s) {
            super(s, s.duration());
            spawnSparks(s.pos(), (int) (10 + 14 * s.scale()), 0.28 * Math.max(0.6, s.scale()), Vec3.ZERO,
                s.color(), s.color2(), 18, 0.01);
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 p = anchor(pt);
            float s = spec.scale();
            float e = easeOut(t);
            float a = (float) Math.pow(1 - t, 1.6) * nearFade(c, p, s * 2.0);
            float ph = seconds(pt);

            if (spec.has(Vfx.VOID)) {
                c.disc(c.voidMatter, p, c.camLeft, c.camUp, s * (0.5 + e), s * (0.5 + e), 28, color(), a, ph);
                c.ring(c.band, p, c.camLook, s * (0.6 + e * 1.2), s * 0.18 * (1 - t), 40, color2(), a, ph);
                return;
            }
            c.glowDisc(p, s * (1.3 + 0.7 * e), color2(), a * 0.9f);
            c.rays(p, s * 3.2 * (0.4 + 0.6 * e), s * 0.1, 12, ph * 1.4, color2(), a);
            c.sphere(c.field, p, s * 1.5 * e, 10, 20, color(), a * 0.7f, 1.8f, false, ph);
            c.ring(c.band, p, c.camLook, s * 1.7 * e, s * 0.32 * (1 - t), 48, color(), a, ph * 2);
        }
    }

    // ---- Sigil -----------------------------------------------------------------------------

    /** A rune circle inscribes itself on the ground: twin rings, a rotating star, rune ticks. */
    public static final class Sigil extends VfxInstance {
        public Sigil(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            float big = spec.scale();
            int points = spec.extra() > 0 ? (int) spec.extra() : 6;
            Vec3 ctr = anchor(pt).add(0, 0.06, 0);
            float draw = easeOut(t / 0.3f);
            float star = easeOut((t - 0.12f) / 0.3f);
            float fade = t > 0.7f ? smooth((1 - t) / 0.3f) : 1f;
            double rot = seconds(pt) * 0.7;
            double w = Math.max(0.06, big * 0.045);
            Vec3 ex = new Vec3(1, 0, 0), ez = new Vec3(0, 0, 1);

            circle(c, c.band, ctr, big, w, rot, Mth.TWO_PI * draw, color(), fade);
            circle(c, c.band, ctr, big * 0.8, w * 0.6, -rot * 1.3, Mth.TWO_PI * draw, color2(), fade * 0.9f);
            circle(c, c.band, ctr, big * 0.3, w * 0.5, rot * 2, Mth.TWO_PI * star, color(), fade * 0.8f);

            if (star > 0) {
                int step = points % 2 == 0 ? points / 2 - 1 : 2;
                Vec3[] v = new Vec3[points];
                for (int k = 0; k < points; k++) {
                    double an = -rot * 0.8 + Mth.TWO_PI * k / points;
                    v[k] = ctr.add(Math.cos(an) * big * 0.8, 0, Math.sin(an) * big * 0.8);
                }
                for (int k = 0; k < points; k++) {
                    Vec3 a = v[k], b = v[(k + Math.max(1, step)) % points];
                    c.stroke(c.band, a, a.lerp(b, star), w * 0.7, UP, color2(), fade * star, 0, 2);
                }
                for (int k = 0; k < 24; k++) {
                    double an = rot * 1.6 + Mth.TWO_PI * k / 24;
                    Vec3 d = new Vec3(Math.cos(an), 0, Math.sin(an));
                    double len = k % 3 == 0 ? 0.16 : 0.08;
                    c.stroke(c.band, ctr.add(d.scale(big * (0.87 - len))), ctr.add(d.scale(big * 0.87)),
                        w * 0.5, UP, color(), fade * star * 0.8f, 0, 1);
                }
                for (int k = 0; k < 4; k++) {
                    double an = rot * 2.4 + k * Math.PI / 2;
                    c.glowDisc(ctr.add(Math.cos(an) * big, 0.05, Math.sin(an) * big), 0.25, color2(), fade * star);
                }
            }
            c.disc(c.glow, ctr, ex, ez, big, big, 32, color(), 0.08f * fade, 0);
        }

        private static void circle(VfxCanvas c, VertexConsumer vc, Vec3 ctr, double r, double w,
                                   double start, double sweep, int rgb, float a) {
            if (sweep <= 0.01) return;
            int n = Math.max(3, (int) (72 * sweep / Mth.TWO_PI));
            Vec3[] pts = VfxCanvas.ellipse(ctr, new Vec3(1, 0, 0), new Vec3(0, 0, 1), r, r, start, sweep, n);
            c.flatRibbon(vc, pts, VfxCanvas.fill(n, (float) w), VfxCanvas.fill(n, a), UP, rgb, 0, 8f / 72);
        }
    }

    // ---- Helix -----------------------------------------------------------------------------

    /** Twin ribbons spiralling up around a body — empowerment, healing, ascension. */
    public static final class Helix extends VfxInstance {
        public Helix(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void onTick() {
            if (age % 2 == 0 && age < lifetime - 4) {
                Vec3 base = anchor(1f);
                double ang = rand.nextDouble() * Mth.TWO_PI;
                spawnSparks(base.add(Math.cos(ang) * 0.7, rand.nextDouble() * spec.scale(), Math.sin(ang) * 0.7),
                    1, 0.01, new Vec3(0, 0.08, 0), color(), color2(), 16, -0.002);
            }
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 base = anchor(pt);
            float env = envelope(t, 0.06f, 0.4f);
            float rise = easeOut(t * 1.7f);
            double h = spec.scale();
            float dir = spec.has(Vfx.REVERSE) ? -1 : 1;
            float ph = seconds(pt) * 7 * dir;

            for (int s = 0; s < 2; s++) {
                int n = 30;
                Vec3[] pts = new Vec3[n];
                float[] w = new float[n], a = new float[n];
                for (int i = 0; i < n; i++) {
                    float f = i / (float) (n - 1);
                    double ang = ph + s * Math.PI + f * Math.PI * 4 * dir;
                    double r = 0.8 * (1 - 0.4 * f);
                    pts[i] = base.add(Math.cos(ang) * r, 0.1 + h * f * rise, Math.sin(ang) * r);
                    w[i] = 0.26f * (1 - f * 0.65f);
                    a[i] = env * (float) Math.pow(1 - f, 0.7);
                }
                c.ribbon(c.band, pts, w, a, s == 0 ? color() : color2(), -seconds(pt) * 3, 0.3f);
            }
            c.ring(c.band, base.add(0, 0.06, 0), UP, 0.9 + t * 0.6, 0.12, 40, color(), env * (1 - t), ph * 0.1f);
        }
    }

    // ---- Aura ------------------------------------------------------------------------------

    /** Lingering presence: rotating ground ring, orbiting arc segments and rising wisps. */
    public static final class Aura extends VfxInstance {
        public Aura(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 base = anchor(pt);
            float env = envelope(t, Math.min(0.3f, 6f / lifetime), Math.min(0.3f, 8f / lifetime));
            double r = spec.scale();
            float sec = seconds(pt);
            boolean dark = spec.has(Vfx.VOID);
            VertexConsumer body = dark ? c.voidMatter : c.band;
            Vec3 g = base.add(0, 0.05, 0);

            c.ring(body, g, UP, r, dark ? 0.5 : 0.12, 56, color(), (dark ? 0.75f : 0.6f) * env, sec);
            Vec3[] b = VfxCanvas.basis(UP);
            for (int k = 0; k < 6; k++)
                c.arc(c.band, g, b, r * 0.78, 0.12, -sec * 1.2 + k * Math.PI / 3, Math.PI / 5, 8, color2(), 0.75f * env, sec);

            // Rising wisps would drift straight through the camera of the aura's owner
            for (int k = 0; k < (isLocalFirstPerson() ? 0 : 4); k++) {
                float p = (sec * 0.45f + k / 4f) % 1f;
                int n = 8;
                Vec3[] pts = new Vec3[n];
                float[] w = new float[n], a = new float[n];
                for (int i = 0; i < n; i++) {
                    float f = i / (float) (n - 1);
                    float q = Math.max(0, p - f * 0.18f);
                    double ang = k * Math.PI / 2 + q * 5 + sec * 0.8;
                    double rr = r * (1 - q * 0.55);
                    pts[i] = base.add(Math.cos(ang) * rr, q * 2.3, Math.sin(ang) * rr);
                    w[i] = (dark ? 0.35f : 0.13f) * (1 - f * 0.6f);
                    a[i] = env * (1 - p) * (1 - f) * (dark ? 0.85f : 0.8f);
                }
                c.ribbon(body, pts, w, a, dark ? color() : color2(), sec * 2, 0.4f);
            }
            if (!dark) c.ring(c.glow, g, UP, r * 0.4, r, 40, color(), 0.1f * env, 0);
        }
    }
}
