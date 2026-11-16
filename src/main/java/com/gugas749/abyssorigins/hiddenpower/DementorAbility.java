package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.api.HiddenAbility;
import com.gugas749.abyssorigins.entity.DementorEntity;
import com.gugas749.abyssorigins.entity.ModEntities;
import com.gugas749.abyssorigins.power.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Summon Dementors (hidden power). Three Dementors spawn out of the ground around the
 * caster and target every other player nearby for {@link DementorEntity#LIFETIME} ticks, starting with the
 * one the caster is aiming at, if any. They never target the caster.
 *
 * While they are active: use again aiming at a player to retarget all of them to that player; use again
 * aiming at nobody to despawn them.
 */
public final class DementorAbility extends HiddenAbility {

    public static final int COUNT = 3;
    public static final double AIM_RANGE = 48, LEASH = 128;

    DementorAbility() {
        super("hidden_dementor", "abyssorigins.ability.hidden_dementor.name", "abyssorigins.ability.hidden_dementor.desc", 120);
        icon("☠");
        color(0xFF3E5566);
    }

    @Override
    public boolean recast(ServerPlayer player) {
        List<DementorEntity> mine = summoned(player);
        if (mine.isEmpty()) return false;
        ServerPlayer target = aimedPlayer(player);
        if (target != null) {
            mine.forEach(d -> d.hunt(target));
            player.displayClientMessage(Component.translatable("abyssorigins.message.dementor_sic", target.getDisplayName()), true);
        } else {
            mine.forEach(DementorEntity::fade);
            player.displayClientMessage(Component.translatable("abyssorigins.message.dementor_dismissed"), true);
        }
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ServerPlayer target = aimedPlayer(player);
        float yaw = player.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        for (int i = 0; i < COUNT; i++) {
            // A loose arc in front of the caster, facing the way they face
            double a = Math.toRadians((i - (COUNT - 1) / 2.0) * 55);
            Vec3 dir = new Vec3(forward.x * Math.cos(a) - forward.z * Math.sin(a), 0, forward.x * Math.sin(a) + forward.z * Math.cos(a));
            Vec3 at = player.position().add(dir.scale(i == 1 ? 3.2 : 2.6));
            DementorEntity d = ModEntities.DEMENTOR.get().create(level);
            if (d == null) return false;
            d.moveTo(at.x, ground(level, at, player.getY()), at.z, yaw, 0);
            d.setYHeadRot(yaw);
            d.setYBodyRot(yaw);
            d.bind(player, target);
            level.addFreshEntity(d);
            level.sendParticles(ParticleTypes.SQUID_INK, at.x, d.getY() + 0.1, at.z, 30, 0.7, 0.05, 0.7, 0.05);
            Fx.ring(level, ParticleTypes.SNOWFLAKE, new Vec3(at.x, d.getY() + 0.1, at.z), 1.2, 16, 0.08);
        }
        // Brief darkness for everyone but the caster
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(24), p -> p != player))
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, false));
        Fx.sound(level, player.position(), SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, 2.0f, 0.5f);
        Fx.sound(level, player.position(), SoundEvents.WITHER_SPAWN, 0.5f, 0.5f);
        player.displayClientMessage(target != null
            ? Component.translatable("abyssorigins.message.dementor_summoned_at", target.getDisplayName())
            : Component.translatable("abyssorigins.message.dementor_summoned"), true);
        return true;
    }

    /** The caster's Dementors still active (not despawning). */
    public static List<DementorEntity> summoned(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(DementorEntity.class, player.getBoundingBox().inflate(LEASH),
            d -> player.getUUID().equals(d.ownerId()) && d.state() != DementorEntity.FADING);
    }

    /** The player in the caster's sights (ignoring their own Dementors, which would block the aim). */
    @Nullable
    private static ServerPlayer aimedPlayer(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle(), end = eye.add(look.scale(AIM_RANGE));
        HitResult block = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double max = block.getType() == HitResult.Type.MISS ? AIM_RANGE : eye.distanceTo(block.getLocation());
        ServerPlayer best = null;
        AABB box = caster.getBoundingBox().expandTowards(look.scale(AIM_RANGE)).inflate(1.0);
        for (ServerPlayer p : caster.serverLevel().getEntitiesOfClass(ServerPlayer.class, box, p -> p != caster && p.isAlive() && !p.isSpectator())) {
            Optional<Vec3> hit = p.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceTo(hit.get());
            if (d <= max) { max = d; best = p; }
        }
        return best;
    }

    /** Ground level under {@code at} near {@code y}, so they rise out of the floor rather than the air. */
    private static double ground(ServerLevel level, Vec3 at, double y) {
        net.minecraft.core.BlockPos.MutableBlockPos p = net.minecraft.core.BlockPos.containing(at.x, y + 1, at.z).mutable();
        for (int i = 0; i < 6 && level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty(); i++) p.move(0, -1, 0);
        return Mth.floor(p.getY());
    }
}
