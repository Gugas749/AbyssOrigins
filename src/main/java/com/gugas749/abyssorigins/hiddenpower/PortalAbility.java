package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.api.HiddenAbility;
import com.gugas749.abyssorigins.hiddenpower.Portals.Endpoint;
import com.gugas749.abyssorigins.hiddenpower.realm.PocketRealm;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmData;
import com.gugas749.abyssorigins.hiddenpower.realm.Realms;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.power.Targeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.OptionalInt;

/**
 * Portal (hidden power). Two presses build a portal:
 *
 *   1. aim at a spot within {@link #RANGE} blocks and use - the first endpoint is placed (a faint ring
 *      only you can see marks it; sneak + use cancels it);
 *   2. aim at a second spot within {@link #RANGE} blocks and use - the second endpoint is placed and
 *      the two are linked for {@link Portals#LIFETIME} ticks. Players step through either end to
 *      reach the other. The cooldown starts here.
 *
 * Use again while the portal is open to close it early. The endpoints can be in different
 * dimensions - place the first, travel, place the second - so a portal can link your own
 * pocket-realm plot with the outside world and let others in. Never in the punishment realm.
 *
 * An endpoint must be a spot a player can stand in (the same no-collision rule other teleports use),
 * on the block you aim at or the floor just below it, and is checked against the range when placed.
 */
public final class PortalAbility extends HiddenAbility {

    public static final double RANGE = 128;
    /** Endpoints in the same dimension closer than this would overlap. */
    private static final double MIN_GAP = 3;

    PortalAbility() {
        super("hidden_portal", "abyssorigins.ability.hidden_portal.name", "abyssorigins.ability.hidden_portal.desc", 30);
        icon("◎");
        color(0xFF9B4DFF);
    }

    /** Close an open portal, cancel or place the first endpoint. The second endpoint is placed by activate(). */
    @Override
    public boolean recast(ServerPlayer player) {
        if (Portals.close(player.server, player.getUUID(), true)) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.portal_closed"), true);
            return true;
        }
        if (player.isShiftKeyDown()) {
            if (Portals.cancelPending(player.getUUID()))
                player.displayClientMessage(Component.translatable("abyssorigins.message.portal_pending_cancelled"), true);
            return true;
        }
        if (Portals.pending(player.getUUID()) != null) return false;           // second press: activate() links
        if (Loadouts.data(player).cooldown(getId()) > 0) return false;          // let the controller report the cooldown

        Endpoint first = select(player, null);
        if (first == null) return true;
        Portals.setPending(player, first);
        Fx.ring(player.serverLevel(), ParticleTypes.REVERSE_PORTAL, first.base().add(0, 0.1, 0), Portals.HALF_WIDTH, 16, 0);
        player.displayClientMessage(Component.translatable("abyssorigins.message.portal_first_placed",
            (int) Math.round(player.position().distanceTo(first.base())), (int) RANGE), true);
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        Endpoint first = Portals.pending(player.getUUID());
        if (first == null) return false;
        Endpoint second = select(player, first);
        if (second == null) return false;                                       // no cooldown for a bad aim
        Portals.link(player, first, second);
        player.displayClientMessage(Component.translatable("abyssorigins.message.portal_linked", clock(Portals.LIFETIME)), true);
        return true;
    }

    /** "m:ss" for a tick count. */
    public static String clock(int ticks) {
        int secs = Math.max(0, (ticks + 19) / 20);
        return secs / 60 + ":" + String.format("%02d", secs % 60);
    }

    // ---- Endpoint selection --------------------------------------------------------------------

    /** The endpoint the player is aiming at, or null (after telling them why) if it isn't valid. */
    private static Endpoint select(ServerPlayer p, Endpoint first) {
        ServerLevel level = p.serverLevel();
        if (level.dimension() == Realms.PUNISHMENT) return fail(p, "abyssorigins.message.portal_realm");

        // Look twice as far as allowed, so a spot just past the limit gets "too far" rather than "nothing there"
        BlockHitResult hit = Targeting.lookBlock(p, RANGE * 2);
        if (hit.getType() == HitResult.Type.MISS) return fail(p, "abyssorigins.message.portal_no_target", (int) RANGE);

        Vec3 base = hit.getDirection() == Direction.UP ? hit.getLocation()
            : floorBelow(level, hit.getLocation().subtract(p.getLookAngle().scale(0.6)));
        if (base == null) return fail(p, "abyssorigins.message.portal_blocked");

        double dist = p.position().distanceTo(base);
        if (dist > RANGE) return fail(p, "abyssorigins.message.portal_out_of_range", (int) Math.ceil(dist), (int) RANGE);
        if (!level.noCollision(p, p.getDimensions(Pose.STANDING).makeBoundingBox(base)))
            return fail(p, "abyssorigins.message.portal_blocked");

        if (level.dimension() == Realms.POCKET) {
            OptionalInt own = RealmData.get(p.server).existingPlot(p.getUUID());
            if (own.isEmpty() || PocketRealm.plotAt(base).orElse(-1) != own.getAsInt())
                return fail(p, "abyssorigins.message.portal_pocket_foreign");
        }
        if (first != null && first.dimension() == level.dimension() && first.base().distanceTo(base) < MIN_GAP)
            return fail(p, "abyssorigins.message.portal_too_close");

        // Face the caster, so the portal opens towards where it was placed from
        Vec3 toPlayer = new Vec3(p.getX() - base.x, 0, p.getZ() - base.z);
        if (toPlayer.lengthSqr() < 0.01) toPlayer = new Vec3(-p.getLookAngle().x, 0, -p.getLookAngle().z);
        if (toPlayer.lengthSqr() < 1.0E-4) toPlayer = Vec3.directionFromRotation(0, p.getYRot() + 180);
        return new Endpoint(level.dimension(), base, toPlayer.normalize());
    }

    /** Top of the first solid floor at or up to three blocks below {@code at}, or null. */
    private static Vec3 floorBelow(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int i = 0; i <= 3; i++, pos = pos.below()) {
            BlockPos under = pos.below();
            BlockState state = level.getBlockState(under);
            VoxelShape shape = state.getCollisionShape(level, under);
            if (!shape.isEmpty()) return new Vec3(at.x, under.getY() + shape.max(Direction.Axis.Y), at.z);
        }
        return null;
    }

    private static Endpoint fail(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
        return null;
    }
}
