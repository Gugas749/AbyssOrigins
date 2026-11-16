package com.gugas749.abyssorigins.hiddenpower.realm;

import com.gugas749.abyssorigins.block.ModBlocks;
import com.gugas749.abyssorigins.block.PunishCrossBlock;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmData.Session;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * The punishment realm: a dark dimension with ash and dead trees, cut into one cell per
 * active punishment so two punishments never meet.
 *
 * The target is bound to the cross at the centre of their cell for {@link #DURATION}
 * ticks: they are pinned to the spot every tick (any movement, knockback, teleport or flight is undone,
 * and their own client holds them and their view still too - {@link CrossPin}),
 * cannot use powers, attack, use or break anything ({@link RealmGuard}), and the timer only runs
 * while they are online, so logging out doesn't shorten it. Their punisher may step in and out to
 * watch - and strike - at will. When time runs out, or the target dies, everyone is returned.
 */
public final class PunishRealm {

    public static final int DURATION = 60 * 20;
    public static final int SPACING = 1024, FLOOR_Y = 64, RADIUS = 18, OBSERVER_RADIUS = 40;
    /** The cross faces south (yaw 0): the target looks at the watcher. */
    private static final float FACING_YAW = 0f;
    /** Drift tolerated before the target is put back (squared blocks): 2 cm. */
    private static final double PIN_TOLERANCE_SQR = 0.02 * 0.02;

    private PunishRealm() {}

    // ---- Layout -------------------------------------------------------------------------------

    public static BlockPos center(int cell) { return new BlockPos(cell * SPACING, FLOOR_Y, 0); }

    /** Where the target stands: on the cross's footrest, facing south (towards the watcher). */
    public static Vec3 crossSpot(int cell) { return Vec3.atBottomCenterOf(center(cell).offset(0, 2, 1)); }

    /** Where the punisher arrives: a few steps south, facing the cross. */
    public static Vec3 observerSpot(int cell) { return Vec3.atBottomCenterOf(center(cell).offset(0, 1, 7)); }

    // ---- Queries ------------------------------------------------------------------------------

    public static boolean isPunished(ServerPlayer p) { return RealmData.get(p.server).session(p.getUUID()) != null; }

    // ---- Lifecycle ----------------------------------------------------------------------------

    /** Binds {@code target} to a fresh cell. The caller has already validated the target. */
    public static void punish(ServerPlayer caster, ServerPlayer target, ServerLevel realm) {
        RealmData data = RealmData.get(caster.server);
        int cell = data.freeCell();
        build(realm, cell);
        data.startSession(target.getUUID(), new Session(caster.getUUID(), cell, Location.of(target), DURATION));
        bind(target, realm, cell);
        Fx.sound(realm, crossSpot(cell), SoundEvents.CHAIN_PLACE, 1.2f, 0.5f);
        Fx.sound(realm, crossSpot(cell), SoundEvents.WITHER_SPAWN, 0.4f, 1.6f);
        target.sendSystemMessage(Component.translatable("abyssorigins.message.punish_bound", DURATION / 20));
    }

    /** The punisher steps in to watch, or back out to where they came from. */
    public static void toggleObserve(ServerPlayer caster, Session s, ServerLevel realm) {
        RealmData data = RealmData.get(caster.server);
        if (data.isObserving(caster.getUUID()) && caster.level().dimension() == Realms.PUNISHMENT) {
            Realms.travel(caster, data.takeObserverReturn(caster.getUUID()));
            return;
        }
        if (!Realms.inRealm(caster)) data.setObserving(caster.getUUID(), Location.of(caster));
        else if (!data.isObserving(caster.getUUID())) data.setObserving(caster.getUUID(), data.pocketReturn(caster.getUUID()));
        Realms.travel(caster, realm, observerSpot(s.cell), 180f, 10f);
    }

    /** Ends a punishment: the target goes home (unless dead), and so does a watching punisher. */
    public static void release(MinecraftServer server, UUID targetId, boolean returnTarget) {
        RealmData data = RealmData.get(server);
        Session s = data.endSession(targetId);
        if (s == null) return;
        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        if (target != null) {
            target.removeEffect(ModEffects.CRUCIFIED);
            if (returnTarget && target.isAlive()) {
                Realms.travel(target, s.targetReturn);
                target.sendSystemMessage(Component.translatable("abyssorigins.message.punish_released"));
            }
        }
        ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
        if (caster != null && data.isObserving(s.caster) && caster.level().dimension() == Realms.PUNISHMENT)
            Realms.travel(caster, data.takeObserverReturn(s.caster));
        if (caster != null)
            caster.displayClientMessage(Component.translatable("abyssorigins.message.punish_ended"), true);
    }

    /** Server tick: counts online targets down and frees those whose time is up. */
    public static void tick(MinecraftServer server) {
        RealmData data = RealmData.get(server);
        if (data.sessions().isEmpty()) return;
        UUID done = null;
        for (Map.Entry<UUID, Session> e : data.sessions().entrySet()) {
            ServerPlayer target = server.getPlayerList().getPlayer(e.getKey());
            if (target == null) continue;                                   // paused while offline
            if (--e.getValue().remaining <= 0 && done == null) done = e.getKey();
        }
        data.touch();
        if (done != null) release(server, done, true);
    }

    /** Player tick for a punished target: keeps them on the cross. */
    public static void restrain(ServerPlayer target, Session s) {
        ServerLevel realm = target.server.getLevel(Realms.PUNISHMENT);
        if (realm == null) return;
        Vec3 spot = crossSpot(s.cell);
        // Any drift at all (a shove, lag, a client that didn't pin itself) is undone at once
        if (target.level() != realm || target.position().distanceToSqr(spot) > PIN_TOLERANCE_SQR) bind(target, realm, s.cell);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = false;                                         // no stray velocity sent to their client
        CrossPin.face(target, FACING_YAW);
        target.resetFallDistance();
        if (target.getAbilities().flying) {
            target.getAbilities().flying = false;
            target.onUpdateAbilities();
        }
        if (target.tickCount % 20 == 0) {
            target.addEffect(new MobEffectInstance(ModEffects.CRUCIFIED, 40, 0, false, false, true));
            realm.sendParticles(ParticleTypes.ASH, spot.x, spot.y + 1, spot.z, 6, 0.4, 0.8, 0.4, 0);
            if (s.remaining % 200 == 0)
                target.displayClientMessage(Component.translatable("abyssorigins.message.punish_remaining", s.remaining / 20), true);
        }
    }

    private static void bind(ServerPlayer target, ServerLevel realm, int cell) {
        Vec3 spot = crossSpot(cell);
        if (target.isPassenger()) target.stopRiding();
        target.teleportTo(realm, spot.x, spot.y, spot.z, FACING_YAW, 0f);  // facing south, towards the watcher
        target.setDeltaMovement(Vec3.ZERO);
        target.addEffect(new MobEffectInstance(ModEffects.CRUCIFIED, 40, 0, false, false, true));
    }

    // ---- Building -----------------------------------------------------------------------------

    /**
     * (Re)builds a cell - always, so damage from an earlier punishment never carries over:
     * a scorched floor, the main cross, a ring of smaller crosses (some topped with
     * skulls), dead trees and a few soul lanterns. Seeded by cell index, so a
     * cell always looks the same.
     */
    private static void build(ServerLevel level, int cell) {
        BlockPos c = center(cell);
        RandomSource rand = RandomSource.create(cell * 341873128712L + 132897987541L);

        // Clear the air above, lay the floor
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
                for (int dy = 1; dy <= 9; dy++) level.setBlock(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(c.offset(dx, -1, dz), Blocks.BLACKSTONE.defaultBlockState(), 2);
                level.setBlock(c.offset(dx, 0, dz), floor(rand), 2);
            }

        // The main cross (a GeckoLib block): the target stands on its footrest block, a block above
        // the ground, chained by the wrists, waist and ankles and facing south towards the watcher
        level.setBlock(c.offset(0, 1, 1), ModBlocks.PUNISH_CROSS.get().defaultBlockState()
            .setValue(PunishCrossBlock.FACING, Direction.SOUTH), 3);

        // Smaller crosses in a ring, facing the centre
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.3;
            BlockPos base = c.offset((int) Math.round(Math.cos(a) * 10), 0, (int) Math.round(Math.sin(a) * 10));
            crucifix(level, base, Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? Direction.Axis.Z : Direction.Axis.X, rand);
        }

        // Dead trees scattered between them
        for (int i = 0; i < 9; i++) {
            double a = rand.nextDouble() * Math.PI * 2, r = 5 + rand.nextDouble() * (RADIUS - 7);
            BlockPos base = c.offset((int) (Math.cos(a) * r), 0, (int) (Math.sin(a) * r));
            if (Math.abs(base.getX() - c.getX()) <= 2 && Math.abs(base.getZ() - c.getZ()) <= 8) continue; // keep the view clear
            deadTree(level, base, rand);
        }

        // Soul lanterns so the cross is visible (besides its own candles)
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        for (int dx : new int[]{-3, 3}) {
            level.setBlock(c.offset(dx, 1, 0), Blocks.BLACKSTONE_WALL.defaultBlockState(), 3);
            level.setBlock(c.offset(dx, 2, 0), lantern, 3);
        }
        level.setBlock(c.offset(-4, 1, 3), Blocks.SOUL_CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(c.offset(4, 1, 3), Blocks.SOUL_CAMPFIRE.defaultBlockState(), 3);
    }

    private static BlockState floor(RandomSource rand) {
        int r = rand.nextInt(10);
        return r < 4 ? Blocks.SOUL_SOIL.defaultBlockState()
             : r < 7 ? Blocks.COARSE_DIRT.defaultBlockState()
             : r < 9 ? Blocks.BLACKSTONE.defaultBlockState()
             : Blocks.BONE_BLOCK.defaultBlockState();
    }

    private static BlockState log(Direction.Axis axis) {
        return Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** A 4-tall cross with a 3-wide beam along {@code beamAxis}, sometimes crowned with a skull. */
    private static void crucifix(ServerLevel level, BlockPos base, Direction.Axis beamAxis, RandomSource rand) {
        BlockState post = log(Direction.Axis.Y), beam = log(beamAxis);
        for (int dy = 1; dy <= 4; dy++) level.setBlock(base.above(dy), post, 3);
        BlockPos arm = base.above(3);
        level.setBlock(beamAxis == Direction.Axis.X ? arm.east() : arm.south(), beam, 3);
        level.setBlock(beamAxis == Direction.Axis.X ? arm.west() : arm.north(), beam, 3);
        if (rand.nextBoolean())
            level.setBlock(base.above(5), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rand.nextInt(16)), 3);
    }

    /** A leafless trunk with a few bare branches. */
    private static void deadTree(ServerLevel level, BlockPos base, RandomSource rand) {
        BlockState trunk = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        int h = 4 + rand.nextInt(4);
        for (int dy = 1; dy <= h; dy++) level.setBlock(base.above(dy), trunk, 3);
        int branches = 2 + rand.nextInt(3);
        for (int b = 0; b < branches; b++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
            BlockPos p = base.above(h - rand.nextInt(3));
            int len = 1 + rand.nextInt(3);
            for (int i = 0; i < len; i++) {
                p = p.relative(dir);
                if (rand.nextInt(3) == 0) p = p.above();
                level.setBlock(p, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, dir.getAxis()), 3);
            }
        }
    }
}
