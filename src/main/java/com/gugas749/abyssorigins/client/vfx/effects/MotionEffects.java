package com.gugas749.abyssorigins.client.vfx.effects;

import com.gugas749.abyssorigins.client.vfx.VfxCanvas;
import com.gugas749.abyssorigins.client.vfx.VfxInstance;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxSpec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.gugas749.abyssorigins.client.vfx.VfxCanvas.UP;

/** Effects defined by movement: beams, bolts, slashes, trails, sparks, marks and wings. */
public final class MotionEffects {

    private MotionEffects() {}

    // ---- Beam ---------------------------------------------------------------------------

    /** An energy lance that races from source to target, wavering, then dissolves. */
    public static final class Beam extends VfxInstance {
        private boolean landed;

        public Beam(VfxSpec s) { super(s, s.duration()); }

        private float travel(float t) { return easeOut(t / 0.3f); }

        @Override
        protected void onTick() {
            if (!landed && travel(age / (float) lifetime) >= 0.98f) {
                landed = true;
                spawnSparks(spec.dir(), 10, 0.18, Vec3.ZERO, color(), color2(), 14, 0.01);
            }
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 from = spec.pos(), to = spec.dir();
            float travel = travel(t);
            float fade = t > 0.45f ? smooth((1 - t) / 0.55f) : 1f;
            float width = spec.extra() > 0 ? spec.extra() : 0.3f;
            Vec3 head = from.lerp(to, travel);
            Vec3[] b = VfxCanvas.basis(to.subtract(from).lengthSqr() < 1.0E-6 ? UP : to.subtract(from));
            float sec = seconds(pt);

            int n = 18;
            Vec3[] pts = new Vec3[n];
            float[] w = new float[n], a = new float[n], wc = new float[n];
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                double wob = Math.sin(f * Math.PI) * 0.12 * Math.sin(sec * 22 + f * 9);
                pts[i] = from.lerp(head, f).add(b[0].scale(wob)).add(b[1].scale(wob * 0.6));
                float taper = (float) Math.min(1, Math.min(f * 6, (1 - f) * 6 + 0.35));
                w[i] = width * taper;
                wc[i] = width * 0.32f * taper;
                a[i] = fade * (0.45f + 0.55f * f) * nearFade(c, pts[i], 1.2);
            }
            if (spec.has(Vfx.VOID)) c.ribbon(c.voidMatter, pts, scale(w, 1.6f), a, color(), -sec * 3, 0.5f);
            else c.ribbon(c.band, pts, w, a, color(), -sec * 3, 0.5f);
            c.ribbon(c.band, pts, wc, a, color2(), -sec * 5, 0.5f);
            c.glowDisc(head, width * 2.6, color2(), fade * nearFade(c, head, 1.5));
            c.glowDisc(from, width * 1.4, color(), fade * 0.6f * nearFade(c, from, 1.5));
        }

        private static float[] scale(float[] w, float k) {
            float[] o = new float[w.length];
            for (int i = 0; i < w.length; i++) o[i] = w[i] * k;
            return o;
        }
    }

    // ---- Bolt ---------------------------------------------------------------------------

    /** Forked lightning, re-struck every couple of ticks so it crackles. */
    public static final class Bolt extends VfxInstance {
        private final List<Vec3[]> branches = new ArrayList<>();

        public Bolt(VfxSpec s) {
            super(s, s.duration());
            regenerate();
            spawnSparks(s.dir(), 18, 0.25, new Vec3(0, 0.1, 0), s.color(), s.color2(), 16, 0.02);
        }

        @Override
        protected void onTick() { if (age % 2 == 0) regenerate(); }

        private void regenerate() {
            branches.clear();
            Vec3[] main = jag(spec.pos(), spec.dir(), 5, 0.13);
            branches.add(main);
            for (int k = 0; k < 2; k++) {
                Vec3 start = main[2 + rand.nextInt(Math.max(1, main.length - 4))];
                Vec3 off = new Vec3(rand.nextGaussian(), -0.4, rand.nextGaussian()).normalize()
                    .scale(spec.pos().distanceTo(spec.dir()) * 0.22);
                branches.add(jag(start, start.add(off), 3, 0.18));
            }
        }

        private Vec3[] jag(Vec3 a, Vec3 b, int depth, double rough) {
            List<Vec3> pts = new ArrayList<>(List.of(a, b));
            double len = a.distanceTo(b);
            for (int d = 0; d < depth; d++) {
                List<Vec3> next = new ArrayList<>();
                for (int i = 0; i < pts.size() - 1; i++) {
                    Vec3 p = pts.get(i), q = pts.get(i + 1);
                    Vec3 mid = p.lerp(q, 0.5).add(new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian())
                        .scale(len * rough / (d + 1)));
                    next.add(p);
                    next.add(mid);
                }
                next.add(pts.get(pts.size() - 1));
                pts = next;
            }
            return pts.toArray(new Vec3[0]);
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            float flicker = (age % 3 == 0 ? 0.55f : 1f) * (float) Math.pow(1 - t, 0.6);
            for (int i = 0; i < branches.size(); i++) {
                Vec3[] pts = branches.get(i);
                float k = i == 0 ? 1f : 0.55f;
                int n = pts.length;
                c.ribbon(c.glow, pts, VfxCanvas.fill(n, 0.9f * k), VfxCanvas.fill(n, 0.3f * flicker), color(), 0, 0);
                c.ribbon(c.band, pts, VfxCanvas.fill(n, 0.22f * k), VfxCanvas.fill(n, flicker), color2(), seconds(pt), 0.2f);
            }
            c.glowDisc(spec.dir(), 1.6, color(), flicker);
            c.glowDisc(spec.dir(), 0.6, 0xFFFFFF, flicker);
        }
    }

    // ---- Slash --------------------------------------------------------------------------

    /** A crescent blade sweep with an afterimage — horizontal by default, upright with VERTICAL. */
    public static final class Slash extends VfxInstance {
        public Slash(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Vec3 ctr = spec.follow() >= 0 ? anchor(pt).add(0, 1.0, 0) : spec.pos();
            Vec3 facing = VfxCanvas.horizontal(spec.dir());
            Vec3 right = new Vec3(-facing.z, 0, facing.x);
            double sweep = Math.toRadians(spec.extra() > 0 ? spec.extra() : 160);
            float dir = spec.has(Vfx.REVERSE) ? -1 : 1;
            Vec3[] b;
            double start;
            if (spec.has(Vfx.VERTICAL)) {
                double tilt = Math.toRadians(35) * dir;
                Vec3 e1 = right.scale(Math.cos(tilt)).add(UP.scale(Math.sin(tilt)));
                Vec3 e2 = UP.scale(Math.cos(tilt)).subtract(right.scale(Math.sin(tilt)));
                b = new Vec3[]{ e1, e2 };
                start = Math.PI / 2 + sweep / 2;
                sweep = -sweep;
            } else {
                b = new Vec3[]{ facing, right.scale(dir) };
                start = -sweep / 2;
            }

            float r = spec.scale();
            float prog = easeOut(t / 0.45f);
            float fade = t > 0.4f ? smooth((1 - t) / 0.6f) : 1f;
            double head = start + sweep * prog;
            double tail = Math.min(Math.abs(sweep * prog), 1.9) * Math.signum(sweep);
            float sec = seconds(pt);

            c.arc(c.band, ctr, b, r, r * 0.32, head - tail, tail, 28, color(), fade, sec * 3);
            c.arc(c.band, ctr, b, r * 0.8, r * 0.12, head - tail * 0.8, tail * 0.8, 24, color2(), fade, sec * 4);
            c.arc(c.glow, ctr, b, r * 0.92, r * 0.6, head - tail * 1.4, tail * 1.4, 24, color(), fade * 0.25f, 0);
            Vec3 tip = ctr.add(VfxCanvas.onCircle(b, head).scale(r));
            c.glowDisc(tip, r * 0.35, color2(), fade);
        }
    }

    // ---- Trail --------------------------------------------------------------------------

    /** A comet tail that follows an entity (dashes, spectral arrows). */
    public static final class Trail extends VfxInstance {
        private static final int LENGTH = 12;
        private final Deque<Vec3> history = new ArrayDeque<>();

        public Trail(VfxSpec s) { super(s, s.duration()); }

        private Vec3 point(Entity e, float pt) { return e.getPosition(pt).add(0, e.getBbHeight() * 0.5, 0); }

        @Override
        protected void onTick() {
            Entity e = followed();
            if (e == null) return;
            history.addLast(point(e, 1f));
            while (history.size() > LENGTH) history.removeFirst();
            if (age % 3 == 0) spawnSparks(point(e, 1f), 1, 0.05, Vec3.ZERO, color(), color2(), 12, 0.0);
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Entity e = followed();
            if (e == null || history.size() < 2) return;
            List<Vec3> pts = new ArrayList<>(history);
            pts.add(point(e, pt));
            int n = pts.size();
            float env = envelope(t, 0.05f, 0.3f);
            float wid = spec.scale();
            float[] w = new float[n], a = new float[n], wc = new float[n];
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                w[i] = wid * f;
                wc[i] = wid * 0.3f * f;
                a[i] = env * f;
            }
            Vec3[] arr = pts.toArray(new Vec3[0]);
            c.ribbon(c.band, arr, w, a, color(), seconds(pt) * 4, 0.3f);
            c.ribbon(c.band, arr, wc, a, color2(), seconds(pt) * 5, 0.3f);
            if (!isLocalFirstPerson()) c.glowDisc(arr[n - 1], wid * 1.4, color2(), env);
        }
    }

    // ---- Sparks -------------------------------------------------------------------------

    /** Just a spray of the engine's glowing streaks. */
    public static final class SparkBurst extends VfxInstance {
        public SparkBurst(VfxSpec s) {
            super(s, 2);
            spawnSparks(s.pos(), Math.max(3, (int) (12 * s.scale())), 0.32, s.dir().scale(0.3), s.color(), s.color2(), 18, 0.015);
        }

        @Override protected void draw(VfxCanvas c, float t, float pt) {}
    }

    // ---- Mark ---------------------------------------------------------------------------

    /** A rotating reticle hovering over a marked creature. */
    public static final class Mark extends VfxInstance {
        public Mark(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Entity e = followed();
            if (e == null) return;
            float env = envelope(t, Math.min(0.3f, 5f / lifetime), Math.min(0.3f, 8f / lifetime));
            float sec = seconds(pt);
            Vec3 p = e.getPosition(pt).add(0, e.getBbHeight() + 0.65 + Math.sin(sec * 3) * 0.06, 0);
            float pop = easeOut((age + pt) / 6f);

            c.ring(c.band, p, c.camLook, 0.42 * pop, 0.08, 36, color(), env * 0.95f, sec * 2);
            for (int k = 0; k < 3; k++) {
                double ang = sec * 1.6 + k * Mth.TWO_PI / 3;
                Vec3 d = c.camLeft.scale(Math.cos(ang)).add(c.camUp.scale(Math.sin(ang)));
                Vec3 perp = c.camLeft.scale(-Math.sin(ang)).add(c.camUp.scale(Math.cos(ang)));
                Vec3[] chev = {
                    p.add(d.scale(0.8 * pop)).add(perp.scale(0.14)),
                    p.add(d.scale(0.58 * pop)),
                    p.add(d.scale(0.8 * pop)).subtract(perp.scale(0.14))
                };
                c.ribbon(c.band, chev, VfxCanvas.fill(3, 0.08f), VfxCanvas.fill(3, env), color2(), sec, 0.5f);
            }
            c.diamond(c.glow, p.subtract(c.camUp.scale(0.62)), 0.12, color2(), env);
            c.glowDisc(p, 0.55, color(), env * 0.35f);
        }
    }

    // ---- Wings --------------------------------------------------------------------------

    /**
     * Wings of light attached to the follow entity: seven plasma feathers per side that open,
     * flap and fold. VOID renders membranous bat wings of dark matter with burning edges.
     */
    public static final class Wings extends VfxInstance {
        private static final int FEATHERS = 7, SEGMENTS = 7;

        public Wings(VfxSpec s) { super(s, s.duration()); }

        @Override
        protected void onTick() {
            Entity e = followed();
            if (e != null && !spec.has(Vfx.VOID) && age % 3 == 0) {
                double yaw = Math.toRadians(e.getYRot());
                Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
                spawnSparks(e.position().add(0, e.getBbHeight() * 0.7, 0).add(back.scale(0.6)), 1, 0.05,
                    new Vec3(0, -0.05, 0), color(), color2(), 24, 0.002);
            }
        }

        @Override
        protected void draw(VfxCanvas c, float t, float pt) {
            Entity e = followed();
            // Your own wings are behind the camera; near the shoulders they would only clip into view.
            if (e == null || isLocalFirstPerson()) return;
            float age = this.age + pt;
            float open = Math.min(easeOut(age / 8f), smooth((lifetime - age) / 8f));
            if (open <= 0.01f) return;

            boolean bat = spec.has(Vfx.VOID);
            float sec = seconds(pt);
            double yaw = Math.toRadians(bodyYaw(pt));
            Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            Vec3 back = forward.scale(-1);
            Vec3 shoulders = e.getPosition(pt).add(0, e.getBbHeight() * 0.74, 0).add(back.scale(0.22));
            float scale = spec.scale() > 0 ? spec.scale() : 1f;
            double flapSpeed = e.onGround() ? 2.2 : 5.5;
            double flap = Math.sin(sec * flapSpeed) * (e.onGround() ? 8 : 18);

            for (int side = -1; side <= 1; side += 2) {
                Vec3 root = shoulders.add(right.scale(side * 0.12));
                for (int k = 0; k < FEATHERS; k++) {
                    float kf = k / (float) (FEATHERS - 1);
                    double elev = Math.toRadians(-25 + kf * 95 + flap * (0.35 + kf * 0.65));
                    double len = (1.0 + 1.1 * Math.sin(Math.PI * (k + 0.6) / FEATHERS)) * scale * open;
                    Vec3 dir = right.scale(side * Math.cos(elev)).add(UP.scale(Math.sin(elev))).add(back.scale(0.45)).normalize();

                    Vec3[] pts = new Vec3[SEGMENTS];
                    float[] w = new float[SEGMENTS], a = new float[SEGMENTS], wc = new float[SEGMENTS];
                    for (int i = 0; i < SEGMENTS; i++) {
                        float f = i / (float) (SEGMENTS - 1);
                        pts[i] = root.add(dir.scale(len * f)).add(back.scale(0.25 * f * f)).add(0, -0.18 * f * f, 0);
                        float width = bat ? 0.42f * (1 - f * 0.55f) : 0.24f * (1 - f) + 0.03f;
                        w[i] = width * scale;
                        wc[i] = w[i] * 0.3f;
                        a[i] = open * (bat ? 0.95f : 0.95f - 0.45f * f);
                    }
                    if (bat) {
                        c.ribbon(c.voidMatter, pts, w, a, color(), sec, 0.4f);
                        if (k % 2 == 0) c.ribbon(c.band, pts, wc, a, color2(), sec * 2, 0.4f);
                    } else {
                        c.ribbon(c.band, pts, w, a, color(), -sec * 2 + k, 0.45f);
                        c.ribbon(c.band, pts, wc, a, color2(), -sec * 3 + k, 0.45f);
                    }
                }
            }
            c.glowDisc(shoulders, 0.9 * scale, color(), open * (bat ? 0.25f : 0.55f));
        }
    }
}
