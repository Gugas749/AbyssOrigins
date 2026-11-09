package com.gugas749.abyssorigins.client.vfx;

import com.gugas749.abyssorigins.vfx.VfxSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One live effect. Subclasses draw geometry for normalised time t (0 → 1). */
public abstract class VfxInstance {

    protected final VfxSpec spec;
    protected final Random rand;
    protected final int lifetime;
    protected int age;
    protected final List<Spark> sparks = new ArrayList<>();

    protected VfxInstance(VfxSpec spec, int lifetime) {
        this.spec = spec;
        this.rand = new Random(spec.seed());
        this.lifetime = Math.max(1, lifetime);
    }

    public VfxSpec spec() { return spec; }

    /** @return false when the effect (and all its sparks) are finished. */
    public boolean tick() {
        age++;
        sparks.removeIf(s -> !s.tick());
        if (spec.follow() >= 0 && age < lifetime && followed() == null) age = lifetime; // owner gone
        if (age < lifetime) onTick();
        return age < lifetime || !sparks.isEmpty();
    }

    protected void onTick() {}

    /**
     * Asked to stop (a STOP spec matched it). Return true to play out a closing animation and end on
     * its own; false (the default) removes it at once.
     */
    public boolean release() { return false; }

    /** The server re-sent this (SUSTAIN) effect while it is showing — it is still wanted. */
    public void refresh() {}

    public final void render(VfxCanvas c, float pt) {
        float t = (age + pt) / lifetime;
        if (t <= 1f) draw(c, t, pt);
        for (Spark s : sparks) s.render(c, pt);
    }

    protected abstract void draw(VfxCanvas c, float t, float pt);

    // ---- Helpers ---------------------------------------------------------------

    protected Entity followed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || spec.follow() < 0) return null;
        Entity e = mc.level.getEntity(spec.follow());
        return e == null || e.isRemoved() ? null : e;
    }

    /** Feet position of the followed entity (interpolated), or the spec position. */
    protected Vec3 anchor(float pt) {
        Entity e = followed();
        return e != null ? e.getPosition(pt) : spec.pos();
    }

    protected float bodyYaw(float pt) {
        Entity e = followed();
        if (e instanceof LivingEntity le) return Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot);
        return e != null ? e.getYRot() : 0f;
    }

    protected float seconds(float pt) { return (age + pt) / 20f; }

    /** True when this effect follows the entity we are currently seeing the world through, in first person. */
    protected boolean isLocalFirstPerson() {
        Minecraft mc = Minecraft.getInstance();
        Entity e = followed();
        return e != null && e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /**
     * 1 when the camera is comfortably far from {@code p}, fading towards 0.15 as it gets
     * inside {@code radius} — stops caster-centred effects from washing out the screen.
     */
    protected static float nearFade(VfxCanvas c, Vec3 p, double radius) {
        double d = c.cam.distanceTo(p);
        return (float) Mth.clamp((d - radius * 0.3) / Math.max(0.01, radius * 1.2), 0.15, 1.0);
    }

    protected int color()  { return spec.color(); }
    protected int color2() { return spec.color2(); }

    // ---- Easing ------------------------------------------------------------------

    public static float easeOut(float t)  { t = Mth.clamp(t, 0f, 1f); float u = 1 - t; return 1 - u * u * u; }
    public static float smooth(float t)   { t = Mth.clamp(t, 0f, 1f); return t * t * (3 - 2 * t); }
    /** 0 → 1 → 0 envelope with configurable attack/release fractions. */
    public static float envelope(float t, float in, float out) {
        if (t < in) return smooth(t / in);
        if (t > 1 - out) return smooth((1 - t) / out);
        return 1f;
    }

    // ---- Sparks ------------------------------------------------------------------

    protected void spawnSparks(Vec3 at, int count, double speed, Vec3 bias, int rgb, int rgb2, int life, double gravity) {
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
            Vec3 vel = dir.scale(speed * (0.4 + rand.nextDouble() * 0.8)).add(bias);
            sparks.add(new Spark(at, vel, life / 2 + rand.nextInt(Math.max(1, life / 2)),
                rand.nextBoolean() ? rgb : rgb2, 0.04 + rand.nextDouble() * 0.05, gravity));
        }
    }

    /** A glowing streak with velocity, drag and gravity — the engine's own "particle". */
    public static final class Spark {
        private Vec3 pos, prev, vel;
        private int life;
        private final int max, rgb;
        private final double size, gravity;

        Spark(Vec3 pos, Vec3 vel, int life, int rgb, double size, double gravity) {
            this.pos = pos; this.prev = pos; this.vel = vel;
            this.life = life; this.max = life; this.rgb = rgb; this.size = size; this.gravity = gravity;
        }

        boolean tick() {
            prev = pos;
            pos = pos.add(vel);
            vel = vel.scale(0.88).add(0, -gravity, 0);
            return --life > 0;
        }

        void render(VfxCanvas c, float pt) {
            float f = (life - pt) / max;
            if (f <= 0) return;
            Vec3 p = prev.lerp(pos, pt);
            double len = Math.max(0.05, vel.length() * 2.2);
            c.streak(p, vel.lengthSqr() > 1.0E-6 ? vel : new Vec3(0, 1, 0), len, size * 2.2, rgb, f);
            c.diamond(c.glow, p, size * (0.6 + f), rgb, f);
        }
    }
}
