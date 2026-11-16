package com.gugas749.abyssorigins.hiddenpower.realm;

import com.gugas749.abyssorigins.block.ModBlocks;
import com.gugas749.abyssorigins.block.PunishCrossBlock;
import com.gugas749.abyssorigins.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Holds a crucified player exactly on the cross's footrest, facing the way the cross faces - on both sides.
 *
 * The server alone can't guarantee it: a player's own client moves them (knockback, pushes, lag) and turns
 * their view, and the server only notices afterwards. So the crucified player's client pins itself too, every
 * tick ({@code HiddenPowerEvents}), and its mouse can't turn the view ({@code EntityMixin}). Knockback is
 * cancelled on the server, so a blow never even sends the shove.
 */
public final class CrossPin {

    /** Where a bound player's feet go, relative to the cross block: on top of it, centred. */
    private static final double FOOT_Y = 1.0;

    private CrossPin() {}

    /** Crucified right now (the effect is refreshed every second while the punishment lasts, on both sides). */
    public static boolean crucified(Player p) {
        return p.level().dimension() == Realms.PUNISHMENT && p.hasEffect(ModEffects.CRUCIFIED);
    }

    /** The punishment cross {@code p} is bound to (searched just around their feet), or null. */
    @Nullable
    public static BlockPos cross(Player p) {
        BlockPos feet = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -3, -2), feet.offset(2, 1, 2)))
            if (p.level().getBlockState(pos).is(ModBlocks.PUNISH_CROSS.get())) return pos.immutable();
        return null;
    }

    /** Puts {@code p} back on the footrest of {@code cross}, still, facing its way. */
    public static void hold(Player p, BlockPos cross) {
        BlockState state = p.level().getBlockState(cross);
        float yaw = state.hasProperty(PunishCrossBlock.FACING) ? state.getValue(PunishCrossBlock.FACING).toYRot() : 0f;
        Vec3 spot = new Vec3(cross.getX() + 0.5, cross.getY() + FOOT_Y, cross.getZ() + 0.5);

        p.setDeltaMovement(Vec3.ZERO);
        if (p.position().distanceToSqr(spot) > 1.0E-6) p.setPos(spot);
        p.xo = p.xOld = spot.x;
        p.yo = p.yOld = spot.y;
        p.zo = p.zOld = spot.z;
        face(p, yaw);
        p.resetFallDistance();
    }

    /** Fixes the view and body to {@code yaw}, level. */
    public static void face(Player p, float yaw) {
        p.setYRot(yaw);
        p.setXRot(0f);
        p.yRotO = yaw;
        p.xRotO = 0f;
        p.setYHeadRot(yaw);
        p.yHeadRotO = yaw;
        p.setYBodyRot(yaw);
        p.yBodyRotO = yaw;
    }
}
