package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.block.ModBlocks;
import com.gugas749.abyssorigins.hiddenpower.realm.Realms;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Poses a player bound to the punishment cross: arms straight out along the beam (a T-pose with a
 * slight sag), legs together, head lowered.
 *
 * Needs no synced state: anyone standing on the centre of a punishment cross in the punishment
 * realm is the one bound to it ({@code PunishRealm} pins the target exactly there every tick).
 * Applied at the end of {@code PlayerModel#setupAnim} by {@code PlayerModelMixin}.
 */
public final class Crucifixion {

    private Crucifixion() {}

    public static boolean isBound(LivingEntity e) {
        if (!(e instanceof Player) || e.level().dimension() != Realms.PUNISHMENT) return false;
        BlockPos feet = BlockPos.containing(e.getX(), e.getY() - 0.2, e.getZ());
        if (!e.level().getBlockState(feet).is(ModBlocks.PUNISH_CROSS.get())) return false;
        double dx = e.getX() - (feet.getX() + 0.5), dz = e.getZ() - (feet.getZ() + 0.5);
        return dx * dx + dz * dz < 0.12;
    }

    public static void pose(HumanoidModel<?> m, float age) {
        float tremble = Mth.sin(age * 1.9f) * 0.012f;
        float breath  = Mth.sin(age * 0.09f);

        // Arms out along the beam, with a slow sag and shake
        set(m.rightArm, 0, 0,  Mth.HALF_PI - 0.07f + tremble + breath * 0.012f);
        set(m.leftArm,  0, 0, -Mth.HALF_PI + 0.07f - tremble - breath * 0.012f);

        // Legs pressed together by the ankle shackle
        set(m.rightLeg, 0, 0, -0.035f);
        set(m.leftLeg,  0, 0,  0.035f);

        // Body still, head lowered with a slight sway
        set(m.body, 0, 0, 0);
        set(m.head, 0.42f + breath * 0.04f, Mth.sin(age * 0.023f) * 0.14f, 0.16f + Mth.sin(age * 0.031f) * 0.05f);
        m.hat.copyFrom(m.head);
    }

    private static void set(ModelPart p, float x, float y, float z) {
        p.xRot = x;
        p.yRot = y;
        p.zRot = z;
    }
}
