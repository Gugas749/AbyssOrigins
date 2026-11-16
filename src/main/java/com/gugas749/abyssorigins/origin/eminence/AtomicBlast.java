package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.config.AbyssOriginsServerConfig;
import com.gugas749.abyssorigins.hiddenpower.realm.Realms;
import com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Hold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Clearable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server side of I Am Atomic that runs over many ticks:
 *
 *  - Hold: from the cast until the crater is finished the caster stays where they cast it, in mid-air once
 *    the ground is gone ({@link Hold}). On release they get Slow Falling.
 *  - Crater: from the explosion on, every block inside the blast is removed, column by column from the centre
 *    outward, at most {@code atomicBlocksPerTick} blocks per tick. The shape is a hemisphere of
 *    {@link Atomic#RADIUS} above the caster's feet plus a bowl {@link #DEPTH} x radius deep at the centre below.
 *    Unbreakable blocks (bedrock...) stay; containers are emptied first so nothing drops. No crater in the
 *    mod's realm dimensions or when {@code atomicDestroysBlocks} is off.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class AtomicBlast {

    /** Depth of the bowl below the caster's feet at the centre, as a fraction of the radius. */
    private static final double DEPTH = 0.2;
    /** Longest the caster is held after the explosion, whatever happens to the crater. */
    private static final int MAX_HOLD_AFTER = 600;
    private static final int SLOW_FALL_TICKS = 260;
    /** Block flags: tell clients, skip neighbour updates (no sand cascades or water spreading mid-blast). */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final ResourceLocation HOLD = Hold.id("atomic_hold");

    private static final class Cast {
        final UUID caster;
        final ServerLevel level;
        final Vec3 anchor;
        int age;
        boolean released;
        Crater crater;

        Cast(ServerPlayer p) {
            caster = p.getUUID();
            level = p.serverLevel();
            anchor = p.position();
        }
    }

    private static final List<Cast> CASTS = new ArrayList<>();
    /** Columns of a disc of {@link Atomic#RADIUS}, as packed (dx, dz), nearest first. Built once. */
    private static int[] columns;

    private AtomicBlast() {}

    // ---- Entry points (from EminenceAbilities.Atomic) ------------------------------------------

    /** True while {@code p} is casting (held in place for the cast or the crater). */
    public static boolean casting(ServerPlayer p) {
        for (Cast c : CASTS) if (!c.released && c.caster.equals(p.getUUID())) return true;
        return false;
    }

    /** Starts holding the caster in place for the cast. */
    public static void begin(ServerPlayer p) {
        CASTS.removeIf(c -> c.caster.equals(p.getUUID()));
        CASTS.add(new Cast(p));
        hold(p, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
    }

    /** The explosion: starts digging the crater (if allowed here). */
    public static void detonate(ServerPlayer p, Vec3 center) {
        for (Cast c : CASTS) {
            if (!c.caster.equals(p.getUUID())) continue;
            if (AbyssOriginsServerConfig.ATOMIC_DESTROYS_BLOCKS.get() && !Realms.isRealm(c.level.dimension()))
                c.crater = new Crater(c.level, BlockPos.containing(center));
        }
    }

    // ---- Tick ---------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CASTS.isEmpty()) return;
        int budget = AbyssOriginsServerConfig.ATOMIC_BLOCKS_PER_TICK.get();
        Iterator<Cast> it = CASTS.iterator();
        while (it.hasNext()) {
            Cast c = it.next();
            c.age++;
            boolean craterDone = c.crater == null || c.crater.done();
            if (!craterDone) budget -= c.crater.dig(Math.max(500, budget));
            if (c.released) {
                if (craterDone) it.remove();
                continue;
            }
            ServerPlayer p = c.level.getServer().getPlayerList().getPlayer(c.caster);
            if (p == null || !p.isAlive() || p.level() != c.level) {
                if (p != null) hold(p, false);
                c.released = true;
                continue;
            }
            boolean blastOver = c.age > Atomic.DETONATE + 10 && craterDone;
            if (blastOver || c.age > Atomic.DETONATE + MAX_HOLD_AFTER) {
                release(p);
                c.released = true;
                continue;
            }
            // Stay put: no velocity, and snap back if anything moved them
            p.setDeltaMovement(Vec3.ZERO);
            if (p.position().distanceToSqr(c.anchor) > 0.04) p.teleportTo(c.anchor.x, c.anchor.y, c.anchor.z);
            p.fallDistance = 0;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CASTS.clear();
    }

    private static void release(ServerPlayer p) {
        hold(p, false);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, true));
        p.fallDistance = 0;
    }

    private static void hold(ServerPlayer p, boolean on) {
        Hold.set(p, HOLD, on);
    }

    // ---- Crater -------------------------------------------------------------------------------

    private static int[] columns() {
        if (columns != null) return columns;
        int r = (int) Atomic.RADIUS;
        List<int[]> list = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (dx * dx + dz * dz <= r * r) list.add(new int[]{ dx, dz, dx * dx + dz * dz });
        list.sort((a, b) -> Integer.compare(a[2], b[2]));
        int[] packed = new int[list.size() * 2];
        for (int i = 0; i < list.size(); i++) { packed[i * 2] = list.get(i)[0]; packed[i * 2 + 1] = list.get(i)[1]; }
        return columns = packed;
    }

    /** One blast's crater, dug a budget at a time, nearest columns first. */
    private static final class Crater {
        private final ServerLevel level;
        private final BlockPos center;
        private final int[] cols = columns();
        private int next;
        private int lastSoundRadius;

        Crater(ServerLevel level, BlockPos center) {
            this.level = level;
            this.center = center;
        }

        boolean done() { return next >= cols.length / 2; }

        /** Digs whole columns until about {@code budget} blocks were removed. @return blocks removed. */
        int dig(int budget) {
            double r = Atomic.RADIUS;
            int removed = 0, checked = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            BlockState air = Blocks.AIR.defaultBlockState();
            while (!done() && removed < budget && checked < budget * 6) {
                int dx = cols[next * 2], dz = cols[next * 2 + 1];
                next++;
                int x = center.getX() + dx, z = center.getZ() + dz;
                if (!level.hasChunkAt(pos.set(x, center.getY(), z))) continue;
                double d = Math.sqrt(dx * dx + dz * dz);
                int domeTop = center.getY() + (int) Math.floor(Math.sqrt(Math.max(0, r * r - d * d)));
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                int bottom = Math.max(level.getMinBuildHeight() + 1,
                    center.getY() - (int) Math.round(r * DEPTH * (1 - (d / r) * (d / r))));
                for (int y = Math.min(domeTop, surface); y >= bottom; y--) {
                    checked++;
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
                    Clearable.tryClear(level.getBlockEntity(pos));
                    level.setBlock(pos, air, FLAGS);
                    removed++;
                }
            }
            // Rumble along the advancing edge
            if (!done()) {
                int dx = cols[next * 2], dz = cols[next * 2 + 1];
                int edge = (int) Math.sqrt(dx * dx + dz * dz);
                if (edge - lastSoundRadius >= 12) {
                    lastSoundRadius = edge;
                    Vec3 at = Vec3.atCenterOf(center.offset(dx, 0, dz));
                    Fx.sound(level, at, SoundEvents.GENERIC_EXPLODE, 4f, 0.5f);
                    level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, 2, 1, 2, 0);
                }
            }
            return removed;
        }
    }
}
