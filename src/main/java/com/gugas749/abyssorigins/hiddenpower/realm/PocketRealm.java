package com.gugas749.abyssorigins.hiddenpower.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalInt;

/**
 * The pocket realm: an always-night void dimension cut into one private plot per player.
 *
 * Plots sit {@link #SPACING} blocks apart, far beyond render distance, so no one ever sees
 * another plot; {@link RealmGuard} returns anyone who strays more than {@link #RADIUS} blocks
 * from their own plot (or falls into the void) to their platform. The power moves the caster
 * alone; the only way anyone else gets in is through a portal the owner opens from their own plot,
 * and only while it stays open.
 */
public final class PocketRealm {

    public static final int SPACING = 4096, ROW = 100, RADIUS = 160, FLOOR_Y = 64, HALF = 4;

    private PocketRealm() {}

    public static BlockPos center(int plot) {
        return new BlockPos((plot % ROW) * SPACING, FLOOR_Y, (plot / ROW) * SPACING);
    }

    /** The plot whose area (within {@link #RADIUS} of its centre) contains {@code pos}, if any. */
    public static OptionalInt plotAt(Vec3 pos) {
        int col = (int) Math.round(pos.x / SPACING), row = (int) Math.round(pos.z / SPACING);
        if (col < 0 || col >= ROW || row < 0) return OptionalInt.empty();
        BlockPos c = center(row * ROW + col);
        double dx = pos.x - (c.getX() + 0.5), dz = pos.z - (c.getZ() + 0.5);
        return dx * dx + dz * dz <= (double) RADIUS * RADIUS ? OptionalInt.of(row * ROW + col) : OptionalInt.empty();
    }

    /** Where the owner stands on arrival: on top of the platform's centre. */
    public static Vec3 spawn(int plot) {
        return Vec3.atBottomCenterOf(center(plot).above());
    }

    /** Takes {@code p} to their own plot, building its platform on the first visit. */
    public static void enter(ServerPlayer p, ServerLevel pocket) {
        RealmData data = RealmData.get(p.server);
        if (!Realms.inRealm(p)) data.setPocketReturn(p.getUUID(), Location.of(p));
        int plot = data.plot(p.getUUID());
        ensurePlatform(pocket, data, plot);
        Realms.travel(p, pocket, spawn(plot), p.getYRot(), 0f);
    }

    /** Returns {@code p} to where they entered from (world spawn if that is unknown). */
    public static void leave(ServerPlayer p) {
        Realms.travel(p, RealmData.get(p.server).takePocketReturn(p.getUUID()));
    }

    /** Back onto the platform - used when someone strays from or falls off their plot. */
    public static void rescue(ServerPlayer p, ServerLevel pocket, int plot) {
        ensurePlatform(pocket, RealmData.get(p.server), plot);
        Realms.travel(p, pocket, spawn(plot), p.getYRot(), 0f);
    }

    /**
     * A 9x9 obsidian-rimmed slab of deepslate with four soul lanterns. Built once per plot; after
     * that only the centre block is restored if missing, so the owner can reshape the rest freely.
     */
    private static void ensurePlatform(ServerLevel level, RealmData data, int plot) {
        BlockPos c = center(plot);
        if (data.isBuilt(plot)) {
            if (level.getBlockState(c).isAir()) level.setBlock(c, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            return;
        }
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState(), rim = Blocks.OBSIDIAN.defaultBlockState();
        for (int dx = -HALF; dx <= HALF; dx++)
            for (int dz = -HALF; dz <= HALF; dz++) {
                boolean edge = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                level.setBlock(c.offset(dx, 0, dz), edge ? rim : floor, 3);
            }
        level.setBlock(c, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2)
                level.setBlock(c.offset(sx * HALF, 1, sz * HALF), lantern, 3);
        data.markBuilt(plot);
    }
}
