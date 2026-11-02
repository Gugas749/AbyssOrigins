package com.gugas749.abyssorigins.power;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Server-side particle and sound choreography. Every shape is sent to all
 * nearby clients via ServerLevel.sendParticles, so abilities read the same
 * for the caster and for bystanders.
 */
public final class Fx {

    private Fx() {}

    // ---- Particle options ---------------------------------------------------

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(vec(rgb), size);
    }

    public static DustColorTransitionOptions fade(int fromRgb, int toRgb, float size) {
        return new DustColorTransitionOptions(vec(fromRgb), vec(toRgb), size);
    }

    private static Vector3f vec(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
    }

    // ---- Shapes -------------------------------------------------------------

    /** Scattered puff. */
    public static void burst(ServerLevel l, ParticleOptions p, Vec3 c, int count, double spread, double speed) {
        l.sendParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    /** Horizontal ring. With outward > 0 particles fly outward (count=0 → velocity mode). */
    public static void ring(ServerLevel l, ParticleOptions p, Vec3 c, double radius, int points, double outward) {
        for (int i = 0; i < points; i++) {
            double a = Mth.TWO_PI * i / points;
            double dx = Math.cos(a), dz = Math.sin(a);
            if (outward > 0) l.sendParticles(p, c.x + dx * radius, c.y, c.z + dz * radius, 0, dx, 0.02, dz, outward);
            else             l.sendParticles(p, c.x + dx * radius, c.y, c.z + dz * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Hollow sphere shell. */
    public static void sphere(ServerLevel l, ParticleOptions p, Vec3 c, double radius, int points) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double t = golden * i;
            l.sendParticles(p, c.x + Math.cos(t) * r * radius, c.y + y * radius, c.z + Math.sin(t) * r * radius,
                1, 0, 0, 0, 0);
        }
    }

    /** Straight line between two points. */
    public static void line(ServerLevel l, ParticleOptions p, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-3) return;
        Vec3 dir = d.scale(1 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3 q = a.add(dir.scale(t));
            l.sendParticles(p, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Rising helix around a point — used for empowerment / transformation. */
    public static void helix(ServerLevel l, ParticleOptions p, Vec3 base, double radius, double height, int points, double phase) {
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double a = phase + t * Mth.TWO_PI * 2;
            l.sendParticles(p, base.x + Math.cos(a) * radius, base.y + t * height, base.z + Math.sin(a) * radius,
                1, 0, 0, 0, 0);
            l.sendParticles(p, base.x + Math.cos(a + Math.PI) * radius, base.y + t * height,
                base.z + Math.sin(a + Math.PI) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Vertical pillar. */
    public static void column(ServerLevel l, ParticleOptions p, Vec3 base, double height, int points) {
        for (int i = 0; i < points; i++) {
            double y = base.y + height * i / points;
            l.sendParticles(p, base.x, y, base.z, 1, 0.08, 0.05, 0.08, 0);
        }
    }

    /** Particles drawn from a ring into the center — vortex / gathering. */
    public static void implode(ServerLevel l, ParticleOptions p, Vec3 c, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = Mth.TWO_PI * i / points;
            double dx = -Math.cos(a), dz = -Math.sin(a);
            l.sendParticles(p, c.x - dx * radius, c.y, c.z - dz * radius, 0, dx, 0, dz, radius * 0.12);
        }
    }

    /** A pair of wings behind the entity, following its yaw. */
    public static void wings(ServerLevel l, ParticleOptions p, Entity e, double span) {
        double yaw = Math.toRadians(e.getYRot());
        Vec3 back  = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.35);
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 root  = e.position().add(0, e.getBbHeight() * 0.72, 0).add(back);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 1; i <= 8; i++) {
                double t = i / 8.0;
                double lift = Math.sin(t * Math.PI) * 0.45 + t * 0.25;
                Vec3 q = root.add(right.scale(side * t * span)).add(0, lift, 0);
                l.sendParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- Sound --------------------------------------------------------------

    public static void sound(Level l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    public static void sound(Level l, Vec3 at, Holder<SoundEvent> s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    public static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.55, 0);
    }

    /** Roughly where the caster's right hand is — origin for cast beams. */
    public static Vec3 hand(Entity e) {
        Vec3 look = e.getLookAngle();
        double yaw = Math.toRadians(e.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return e.getEyePosition().add(0, -0.4, 0).add(look.scale(0.6)).add(right.scale(0.35));
    }
}
