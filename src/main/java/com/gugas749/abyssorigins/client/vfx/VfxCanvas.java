package com.gugas749.abyssorigins.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Immediate-mode 3D geometry for the VFX engine. All coordinates are world-space;
 * vertices are emitted camera-relative (the model-view stack already holds the camera
 * rotation at the AFTER_PARTICLES stage).
 *
 * UV convention for BAND/VOID: u = distance along the shape (period 8 for closed loops),
 * v = 0..1 across it. FIELD uses u = longitude, v = latitude.
 */
public final class VfxCanvas {

    public static final Vec3 UP = new Vec3(0, 1, 0);

    public final VertexConsumer band, field, glow, voidMatter;
    public final Vec3 cam, camLeft, camUp, camLook;
    public final float partialTick;
    private final Matrix4f identity = new Matrix4f();

    public VfxCanvas(VertexConsumer band, VertexConsumer field, VertexConsumer glow, VertexConsumer voidMatter,
                     Vec3 cam, Vector3f left, Vector3f up, Vector3f look, float partialTick) {
        this.band = band; this.field = field; this.glow = glow; this.voidMatter = voidMatter;
        this.cam = cam;
        this.camLeft = new Vec3(left.x(), left.y(), left.z());
        this.camUp = new Vec3(up.x(), up.y(), up.z());
        this.camLook = new Vec3(look.x(), look.y(), look.z());
        this.partialTick = partialTick;
    }

    // ---- Vertex -----------------------------------------------------------------

    public void v(VertexConsumer vc, Vec3 p, float u, float vv, int rgb, float a) {
        vc.addVertex(identity, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
          .setUv(u, vv)
          .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int) (Mth.clamp(a, 0f, 1f) * 255));
    }

    // ---- Maths helpers ------------------------------------------------------------

    /** Two unit vectors spanning the plane perpendicular to {@code n}. */
    public static Vec3[] basis(Vec3 n) {
        n = n.normalize();
        Vec3 ref = Math.abs(n.y) < 0.95 ? UP : new Vec3(1, 0, 0);
        Vec3 e1 = ref.cross(n).normalize();
        Vec3 e2 = n.cross(e1).normalize();
        return new Vec3[]{ e1, e2 };
    }

    public static Vec3 onCircle(Vec3[] b, double angle) {
        return b[0].scale(Math.cos(angle)).add(b[1].scale(Math.sin(angle)));
    }

    public static Vec3 rotateY(Vec3 v, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    public static Vec3 horizontal(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : h.normalize();
    }

    public static int lerpColor(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }

    // ---- Primitives ------------------------------------------------------------------

    /** Flat annulus band of radius {@code r} and width {@code w} around {@code c}, perpendicular to {@code n}. */
    public void ring(VertexConsumer vc, Vec3 c, Vec3 n, double r, double w, int segs, int rgb, float a, float uPhase) {
        if (a <= 0.004f || r <= 0) return;
        Vec3[] b = basis(n);
        double ri = Math.max(0, r - w / 2), ro = r + w / 2;
        for (int i = 0; i < segs; i++) {
            double a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            Vec3 d0 = onCircle(b, a0), d1 = onCircle(b, a1);
            float u0 = 8f * i / segs + uPhase, u1 = 8f * (i + 1) / segs + uPhase;
            v(vc, c.add(d0.scale(ri)), u0, 0, rgb, a);
            v(vc, c.add(d0.scale(ro)), u0, 1, rgb, a);
            v(vc, c.add(d1.scale(ro)), u1, 1, rgb, a);
            v(vc, c.add(d1.scale(ri)), u1, 0, rgb, a);
        }
    }

    /**
     * Partial ring — a crescent. Width swells to {@code w} mid-arc and tapers to points;
     * alpha rises from the tail (t=0) to the head (t=1).
     */
    public void arc(VertexConsumer vc, Vec3 c, Vec3[] b, double r, double w, double start, double sweep,
                    int segs, int rgb, float a, float uPhase) {
        if (a <= 0.004f) return;
        for (int i = 0; i < segs; i++) {
            float t0 = i / (float) segs, t1 = (i + 1) / (float) segs;
            double w0 = w * Math.pow(Math.sin(Math.PI * t0), 0.7), w1 = w * Math.pow(Math.sin(Math.PI * t1), 0.7);
            double an0 = start + sweep * t0, an1 = start + sweep * t1;
            Vec3 d0 = onCircle(b, an0), d1 = onCircle(b, an1);
            float al0 = a * (0.15f + 0.85f * t0), al1 = a * (0.15f + 0.85f * t1);
            float u0 = t0 * 4f + uPhase, u1 = t1 * 4f + uPhase;
            v(vc, c.add(d0.scale(r - w0 / 2)), u0, 0, rgb, al0);
            v(vc, c.add(d0.scale(r + w0 / 2)), u0, 1, rgb, al0);
            v(vc, c.add(d1.scale(r + w1 / 2)), u1, 1, rgb, al1);
            v(vc, c.add(d1.scale(r - w1 / 2)), u1, 0, rgb, al1);
        }
    }

    /** Vertical cylinder wall from c.y to c.y + h; alpha fades bottom → top. */
    public void wall(VertexConsumer vc, Vec3 c, double r, double h, int segs, int rgb, float aBottom, float aTop, float uPhase) {
        if (aBottom <= 0.004f || r <= 0 || h <= 0) return;
        for (int i = 0; i < segs; i++) {
            double a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            Vec3 p0 = c.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), p1 = c.add(Math.cos(a1) * r, 0, Math.sin(a1) * r);
            float u0 = 8f * i / segs + uPhase, u1 = 8f * (i + 1) / segs + uPhase;
            v(vc, p0, u0, 0, rgb, aBottom);
            v(vc, p0.add(0, h, 0), u0, 1, rgb, aTop);
            v(vc, p1.add(0, h, 0), u1, 1, rgb, aTop);
            v(vc, p1, u1, 0, rgb, aBottom);
        }
    }

    /** Camera-facing ribbon through {@code pts} — reads as a glowing tube from every angle. */
    public void ribbon(VertexConsumer vc, Vec3[] pts, float[] widths, float[] alphas, int rgb, float uStart, float uStep) {
        int n = pts.length;
        if (n < 2) return;
        Vec3[] side = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 tan = pts[Math.min(n - 1, i + 1)].subtract(pts[Math.max(0, i - 1)]);
            Vec3 toCam = cam.subtract(pts[i]);
            Vec3 s = tan.cross(toCam);
            side[i] = s.lengthSqr() < 1.0E-8 ? camLeft : s.normalize();
        }
        for (int i = 0; i < n - 1; i++) {
            if (alphas[i] <= 0.004f && alphas[i + 1] <= 0.004f) continue;
            float u0 = uStart + uStep * i, u1 = uStart + uStep * (i + 1);
            Vec3 a0 = side[i].scale(widths[i] / 2), a1 = side[i + 1].scale(widths[i + 1] / 2);
            v(vc, pts[i].subtract(a0), u0, 0, rgb, alphas[i]);
            v(vc, pts[i].add(a0), u0, 1, rgb, alphas[i]);
            v(vc, pts[i + 1].add(a1), u1, 1, rgb, alphas[i + 1]);
            v(vc, pts[i + 1].subtract(a1), u1, 0, rgb, alphas[i + 1]);
        }
    }

    /** Ribbon lying in the plane with normal {@code n} (ground spirals, sigil strokes). */
    public void flatRibbon(VertexConsumer vc, Vec3[] pts, float[] widths, float[] alphas, Vec3 n, int rgb, float uStart, float uStep) {
        int count = pts.length;
        if (count < 2) return;
        for (int i = 0; i < count - 1; i++) {
            Vec3 t0 = pts[Math.min(count - 1, i + 1)].subtract(pts[Math.max(0, i - 1)]);
            Vec3 t1 = pts[Math.min(count - 1, i + 2)].subtract(pts[i]);
            Vec3 s0 = t0.cross(n).normalize().scale(widths[i] / 2), s1 = t1.cross(n).normalize().scale(widths[i + 1] / 2);
            float u0 = uStart + uStep * i, u1 = uStart + uStep * (i + 1);
            v(vc, pts[i].subtract(s0), u0, 0, rgb, alphas[i]);
            v(vc, pts[i].add(s0), u0, 1, rgb, alphas[i]);
            v(vc, pts[i + 1].add(s1), u1, 1, rgb, alphas[i + 1]);
            v(vc, pts[i + 1].subtract(s1), u1, 0, rgb, alphas[i + 1]);
        }
    }

    /** Straight flat stroke from a to b in plane {@code n}. */
    public void stroke(VertexConsumer vc, Vec3 a, Vec3 b, double w, Vec3 n, int rgb, float alpha, float u0, float u1) {
        Vec3 s = b.subtract(a).cross(n);
        if (s.lengthSqr() < 1.0E-8) return;
        s = s.normalize().scale(w / 2);
        v(vc, a.subtract(s), u0, 0, rgb, alpha);
        v(vc, a.add(s), u0, 1, rgb, alpha);
        v(vc, b.add(s), u1, 1, rgb, alpha);
        v(vc, b.subtract(s), u1, 0, rgb, alpha);
    }

    /**
     * UV sphere with a fresnel rim: faces turned away from the camera's line of sight glow,
     * faces looking straight at it go nearly transparent — reads as a solid energy shell.
     */
    public void sphere(VertexConsumer vc, Vec3 c, double r, int latN, int lonN, int rgb, float a,
                       float rimPower, boolean half, float uPhase) {
        if (a <= 0.004f || r <= 0) return;
        double maxTheta = half ? Math.PI / 2 : Math.PI;
        for (int i = 0; i < latN; i++) {
            double th0 = maxTheta * i / latN, th1 = maxTheta * (i + 1) / latN;
            for (int j = 0; j < lonN; j++) {
                double ph0 = Mth.TWO_PI * j / lonN, ph1 = Mth.TWO_PI * (j + 1) / lonN;
                sphereVertex(vc, c, r, th0, ph0, 8f * j / lonN + uPhase, (float) (th0 / Math.PI), rgb, a, rimPower);
                sphereVertex(vc, c, r, th1, ph0, 8f * j / lonN + uPhase, (float) (th1 / Math.PI), rgb, a, rimPower);
                sphereVertex(vc, c, r, th1, ph1, 8f * (j + 1) / lonN + uPhase, (float) (th1 / Math.PI), rgb, a, rimPower);
                sphereVertex(vc, c, r, th0, ph1, 8f * (j + 1) / lonN + uPhase, (float) (th0 / Math.PI), rgb, a, rimPower);
            }
        }
    }

    private void sphereVertex(VertexConsumer vc, Vec3 c, double r, double th, double ph, float u, float vv,
                              int rgb, float a, float rimPower) {
        Vec3 n = new Vec3(Math.sin(th) * Math.cos(ph), Math.cos(th), Math.sin(th) * Math.sin(ph));
        Vec3 p = c.add(n.scale(r));
        Vec3 view = cam.subtract(p);
        double len = view.length();
        if (rimPower <= 0f) { v(vc, p, u, vv, rgb, a); return; }
        float facing = len < 1.0E-4 ? 0f : (float) Math.abs(n.dot(view.scale(1 / len)));
        float rim = (float) Math.pow(1f - facing, rimPower);
        v(vc, p, u, vv, rgb, a * (0.12f + 0.88f * rim));
    }

    /**
     * Elliptical disc in the plane (e1, e2): v = 0.5 at the centre and 0 at the edge, so the
     * BAND material gives a white-hot core with flame edges and the VOID material gives a
     * pitch-black core with a burning rim. Pass camLeft/camUp for a camera-facing orb.
     */
    public void disc(VertexConsumer vc, Vec3 c, Vec3 e1, Vec3 e2, double rx, double ry, int segs,
                     int rgb, float a, float uPhase) {
        if (a <= 0.004f || rx <= 0 || ry <= 0) return;
        for (int i = 0; i < segs; i++) {
            double a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            Vec3 p0 = c.add(e1.scale(Math.cos(a0) * rx)).add(e2.scale(Math.sin(a0) * ry));
            Vec3 p1 = c.add(e1.scale(Math.cos(a1) * rx)).add(e2.scale(Math.sin(a1) * ry));
            float u0 = 8f * i / segs + uPhase, u1 = 8f * (i + 1) / segs + uPhase;
            v(vc, c, u0, 0.5f, rgb, a);
            v(vc, p0, u0, 0f, rgb, a);
            v(vc, p1, u1, 0f, rgb, a);
            v(vc, c, u1, 0.5f, rgb, a);
        }
    }

    /** Points on an ellipse in plane (e1, e2), optionally a partial sweep. */
    public static Vec3[] ellipse(Vec3 c, Vec3 e1, Vec3 e2, double rx, double ry, double start, double sweep, int n) {
        Vec3[] pts = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double an = start + sweep * i / (n - 1);
            pts[i] = c.add(e1.scale(Math.cos(an) * rx)).add(e2.scale(Math.sin(an) * ry));
        }
        return pts;
    }

    public static float[] fill(int n, float value) {
        float[] f = new float[n];
        java.util.Arrays.fill(f, value);
        return f;
    }

    /** Soft round glow facing the camera: bright centre fading to nothing (GLOW consumer). */
    public void glowDisc(Vec3 c, double r, int rgb, float a) {
        if (a <= 0.004f || r <= 0) return;
        int segs = 18;
        for (int i = 0; i < segs; i++) {
            double a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            Vec3 e0 = camLeft.scale(Math.cos(a0) * r).add(camUp.scale(Math.sin(a0) * r));
            Vec3 e1 = camLeft.scale(Math.cos(a1) * r).add(camUp.scale(Math.sin(a1) * r));
            v(glow, c, 0, 0, rgb, a);
            v(glow, c.add(e0), 0, 0, rgb, 0);
            v(glow, c.add(e1), 0, 0, rgb, 0);
            v(glow, c, 0, 0, rgb, a);
        }
    }

    /** Star-burst rays facing the camera (GLOW consumer). */
    public void rays(Vec3 c, double len, double width, int count, double rot, int rgb, float a) {
        if (a <= 0.004f) return;
        for (int k = 0; k < count; k++) {
            double ang = rot + Mth.TWO_PI * k / count;
            Vec3 dir = camLeft.scale(Math.cos(ang)).add(camUp.scale(Math.sin(ang)));
            Vec3 perp = camLeft.scale(-Math.sin(ang)).add(camUp.scale(Math.cos(ang))).scale(width);
            double l = len * (k % 2 == 0 ? 1.0 : 0.55);
            v(glow, c.add(perp), 0, 0, rgb, a);
            v(glow, c.add(dir.scale(l)), 0, 0, rgb, 0);
            v(glow, c.add(dir.scale(l)), 0, 0, rgb, 0);
            v(glow, c.subtract(perp), 0, 0, rgb, a);
        }
    }

    /** Glowing streak from {@code head} trailing back along {@code -dir} (GLOW consumer). */
    public void streak(Vec3 head, Vec3 dir, double len, double width, int rgb, float a) {
        if (a <= 0.004f || dir.lengthSqr() < 1.0E-8) return;
        Vec3 tail = head.subtract(dir.normalize().scale(len));
        ribbon(glow, new Vec3[]{ tail, head }, new float[]{ (float) width * 0.2f, (float) width },
            new float[]{ 0f, a }, rgb, 0, 1);
    }

    /** Flat diamond (rhombus) facing the camera. */
    public void diamond(VertexConsumer vc, Vec3 c, double r, int rgb, float a) {
        Vec3 l = camLeft.scale(r * 0.6), u = camUp.scale(r);
        v(vc, c.add(u), 0.5f, 0, rgb, a);
        v(vc, c.add(l), 0, 0.5f, rgb, a);
        v(vc, c.subtract(u), 0.5f, 1, rgb, a);
        v(vc, c.subtract(l), 1, 0.5f, rgb, a);
    }
}
