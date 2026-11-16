package com.gugas749.abyssorigins.power;

import net.minecraft.core.Holder;
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

    private static Vector3f vec(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
    }

    // ---- Shapes -------------------------------------------------------------

    /** Scattered puff. */
    public static void burst(ServerLevel l, ParticleOptions p, Vec3 c, int count, double spread, double speed) {
        l.sendParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    /** Horizontal ring. With outward > 0 particles fly outward (count=0 -> velocity mode). */
    public static void ring(ServerLevel l, ParticleOptions p, Vec3 c, double radius, int points, double outward) {
        for (int i = 0; i < points; i++) {
            double a = Mth.TWO_PI * i / points;
            double dx = Math.cos(a), dz = Math.sin(a);
            if (outward > 0) l.sendParticles(p, c.x + dx * radius, c.y, c.z + dz * radius, 0, dx, 0.02, dz, outward);
            else             l.sendParticles(p, c.x + dx * radius, c.y, c.z + dz * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Particles drawn from a ring into the center - vortex / gathering. */
    public static void implode(ServerLevel l, ParticleOptions p, Vec3 c, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = Mth.TWO_PI * i / points;
            double dx = -Math.cos(a), dz = -Math.sin(a);
            l.sendParticles(p, c.x - dx * radius, c.y, c.z - dz * radius, 0, dx, 0, dz, radius * 0.12);
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

    /** Roughly where the caster's right hand is - origin for cast beams. */
    public static Vec3 hand(Entity e) {
        Vec3 look = e.getLookAngle();
        double yaw = Math.toRadians(e.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return e.getEyePosition().add(0, -0.4, 0).add(look.scale(0.6)).add(right.scale(0.35));
    }
}
