package com.gugas749.abyssorigins.client.eminence;

import com.gugas749.abysscore.api.cinematic.client.AbyssCinematicsClient;
import com.gugas749.abyssorigins.item.gear.SovereignScytheItem;
import com.gugas749.abyssorigins.origin.eminence.AtomicCinematic;
import com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic;
import com.gugas749.abyssorigins.origin.eminence.ScytheCombo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

/**
 * First-person Sovereign Scythe: carried over the shoulder at the right edge of the view; each swing of the
 * chain sweeps the blade across the screen its own way; a throw winds it back over the head.
 * Poses: hand position in camera space (x right, y up, z back) + the scythe's yaw/pitch/roll (see
 * {@link ScytheAnimations#orient}).
 */
public final class ScytheFirstPerson implements IClientItemExtensions {

    public static final ScytheFirstPerson INSTANCE = new ScytheFirstPerson();

    record FP(float x, float y, float z, float yaw, float pitch, float roll) {}
    private record Key(float t, FP pose) {
        FP get() { return pose != null ? pose : REST; }
    }

    static final FP REST = new FP(0.78f, -0.78f, -0.95f, 38, 48, -110);
    static final FP THROW = new FP(0.62f, -0.05f, -0.45f, 172, 72, 0);

    private static final Key[][] MOVES = {
        { new Key(0f, null),                                              // Reaping Sweep: across the view, right to left
          new Key(0.28f, new FP(1.1f, -1.0f, -1.0f, 50, 35, -90)),
          new Key(0.55f, new FP(0.0f, -1.0f, -1.1f, 0, 35, -90)),
          new Key(0.8f, new FP(-1.1f, -1.0f, -1.0f, -50, 35, -90)),
          new Key(1f, null) },
        { new Key(0f, null),                                              // Sovereign Strike: raised, then chopped down
          new Key(0.3f, new FP(0.35f, -0.25f, -0.5f, 5, 105, 150)),
          new Key(0.55f, new FP(0.15f, -0.7f, -0.75f, -5, 40, 145)),
          new Key(0.78f, new FP(0.2f, -1.0f, -0.8f, -5, 5, 140)),
          new Key(1f, null) },
        { new Key(0f, null),                                              // Harvest: rising from low left to high right
          new Key(0.28f, new FP(-0.9f, -1.2f, -1.0f, -40, 20, 60)),
          new Key(0.55f, new FP(0.0f, -0.8f, -1.1f, 0, 50, 80)),
          new Key(0.8f, new FP(0.9f, -0.4f, -1.0f, 40, 75, 90)),
          new Key(1f, null) },
    };

    /** I Am Atomic: raised overhead, wound back, cut down through the view (see ScytheAnimations.ATOMIC). */
    private static final Key[] ATOMIC = {
        new Key(0f, null),
        new Key(Atomic.RAISED / ScytheAnimations.ATOMIC_TICKS, new FP(0.3f, 0.15f, -0.55f, 0, 98, 150)),
        new Key(Atomic.SWING / ScytheAnimations.ATOMIC_TICKS, new FP(0.3f, 0.15f, -0.55f, 0, 98, 150)),
        new Key((Atomic.SWING + 3) / ScytheAnimations.ATOMIC_TICKS, new FP(0.3f, 0.25f, -0.45f, 0, 120, 150)),
        new Key(Atomic.CUT / ScytheAnimations.ATOMIC_TICKS, new FP(0.15f, -1.1f, -0.85f, -5, -20, 140)),
        new Key(96 / ScytheAnimations.ATOMIC_TICKS, new FP(0.15f, -1.1f, -0.85f, -5, -20, 140)),
        new Key(1f, null) };

    private ScytheFirstPerson() {}

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partial, float equip, float swing) {
        FP p = poseOf(player, partial);
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float shake = 0;
        if (player.isUsingItem() && player.getUseItem() == stack
                && player.getTicksUsingItem() >= SovereignScytheItem.FULL_CHARGE)
            shake = Mth.sin((player.tickCount + partial) * 2.6f) * 0.012f;
        pose.translate(p.x() * side + shake, p.y() - equip * 0.6f, p.z());
        pose.mulPose(ScytheAnimations.orient(p.yaw(), p.pitch(), p.roll(), side < 0,
            new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, -1)));
        ScytheAnimations.applyGrip(pose);
        return true;
    }

    private static FP poseOf(LocalPlayer player, float partial) {
        float atomic = AbyssCinematicsClient.age(AtomicCinematic.id(player.getId()), partial);
        if (atomic >= 0) return sample(ATOMIC, Math.min(1f, atomic / ScytheAnimations.ATOMIC_TICKS));
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof SovereignScytheItem) {
            float c = Math.min(1f, (player.getTicksUsingItem() + partial) / SovereignScytheItem.FULL_CHARGE);
            return blend(REST, THROW, ScytheAnimations.ease(c));
        }
        float t = ScytheAnimations.swingProgress(player, partial);
        return t < 0 ? REST : sample(MOVES[ScytheCombo.stage(player)], t);
    }

    private static FP sample(Key[] keys, float t) {
        for (int i = 0; i < keys.length - 1; i++) {
            Key a = keys[i], b = keys[i + 1];
            if (t <= b.t()) return blend(a.get(), b.get(), ScytheAnimations.ease((t - a.t()) / (b.t() - a.t())));
        }
        return keys[keys.length - 1].get();
    }

    private static FP blend(FP a, FP b, float t) {
        return new FP(Mth.lerp(t, a.x(), b.x()), Mth.lerp(t, a.y(), b.y()), Mth.lerp(t, a.z(), b.z()),
            Mth.rotLerp(t, a.yaw(), b.yaw()), Mth.lerp(t, a.pitch(), b.pitch()), Mth.rotLerp(t, a.roll(), b.roll()));
    }
}
