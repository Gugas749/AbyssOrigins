package com.gugas749.abyssorigins.vfx;

import com.gugas749.abyssorigins.network.packet.VfxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side entry point for 3D ability visuals.
 *
 * <pre>
 * Vfx.of(VfxType.SHOCKWAVE).at(pos).scale(5).colors(0xFF6A1F, 0xFFE08A).duration(18).send(level);
 * </pre>
 * The packet is tiny (one spec); all geometry is built and animated on each client.
 */
public final class Vfx {

    /** Hemisphere instead of a full sphere (DOME). */
    public static final int HALF     = 1;
    /** Dark "void" material instead of light (WINGS, AURA, DOME, BURST). */
    public static final int VOID     = 1 << 1;
    /** Upright orientation (SLASH). */
    public static final int VERTICAL = 1 << 2;
    /** Mirror / reverse direction (SLASH spin, HELIX). */
    public static final int REVERSE  = 1 << 3;
    /** Not an effect: removes active effects of this type attached to {@code follow}. */
    public static final int STOP     = 1 << 30;

    private static final double RANGE = 96;

    private Vfx() {}

    public static Builder of(VfxType type) { return new Builder(type); }

    // ---- One-line shorthands for the most common shapes ------------------------------

    public static void burst(ServerLevel level, Vec3 at, float size, int c1, int c2) {
        of(VfxType.BURST).at(at).scale(size).colors(c1, c2).duration(12).send(level);
    }

    public static void sparks(ServerLevel level, Vec3 at, float amount, int c1, int c2) {
        of(VfxType.SPARKS).at(at).scale(amount).colors(c1, c2).send(level);
    }

    public static void shockwave(ServerLevel level, Vec3 at, float radius, int c1, int c2, int duration) {
        of(VfxType.SHOCKWAVE).at(at).scale(radius).colors(c1, c2).duration(duration).send(level);
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, float width, int c1, int c2, int duration) {
        of(VfxType.BEAM).at(from).to(to).extra(width).colors(c1, c2).duration(duration).send(level);
    }

    public static void helix(ServerLevel level, Entity e, float height, int c1, int c2, int duration) {
        of(VfxType.HELIX).follow(e).scale(height).colors(c1, c2).duration(duration).send(level);
    }

    /** Ends a lingering attached effect early (e.g. wings when the form is cancelled). */
    public static void stop(ServerLevel level, Entity entity, VfxType type) {
        of(type).follow(entity).flags(STOP).send(level);
    }

    /** Like {@link #stop(ServerLevel, Entity, VfxType)} but only effects whose primary colour matches. */
    public static void stop(ServerLevel level, Entity entity, VfxType type, int color) {
        of(type).follow(entity).color(color).extra(1).flags(STOP).send(level);
    }

    public static final class Builder {
        private final VfxType type;
        private Vec3 pos = Vec3.ZERO, dir = Vec3.ZERO;
        private float scale = 1f, extra = 0f;
        private int color = 0xFFFFFF, color2 = -1, duration = 20, follow = -1, flags = 0;

        private Builder(VfxType type) { this.type = type; }

        public Builder at(Vec3 p)            { this.pos = p; return this; }
        /** Position at the entity's feet. */
        public Builder at(Entity e)          { this.pos = e.position(); return this; }
        /** Absolute target point (BEAM, BOLT) or facing vector (SLASH, RIFT, SPARKS). */
        public Builder dir(Vec3 d)           { this.dir = d; return this; }
        public Builder to(Vec3 target)       { this.dir = target; return this; }
        public Builder scale(float s)        { this.scale = s; return this; }
        public Builder extra(float e)        { this.extra = e; return this; }
        public Builder color(int rgb)        { this.color = rgb & 0xFFFFFF; return this; }
        public Builder colors(int a, int b)  { this.color = a & 0xFFFFFF; this.color2 = b & 0xFFFFFF; return this; }
        public Builder duration(int ticks)   { this.duration = Math.max(1, ticks); return this; }
        public Builder follow(Entity e)      { this.follow = e.getId(); if (pos == Vec3.ZERO) pos = e.position(); return this; }
        public Builder flags(int f)          { this.flags |= f; return this; }

        public VfxSpec build() {
            int seed = (int) (Double.doubleToLongBits(pos.x * 31 + pos.z * 17 + pos.y) ^ System.nanoTime());
            return new VfxSpec(type, pos, dir, scale, extra, color, color2 < 0 ? color : color2, duration, follow, flags, seed);
        }

        /** Everyone within 96 blocks sees it. */
        public void send(ServerLevel level) {
            PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, RANGE, new VfxPacket(build()));
        }

        /** Only this player sees it (personal marks, private sight). */
        public void sendTo(ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new VfxPacket(build()));
        }
    }
}
