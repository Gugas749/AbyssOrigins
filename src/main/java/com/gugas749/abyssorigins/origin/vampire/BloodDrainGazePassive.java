package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BloodDrainGazePassive extends OriginPassive {

    private static final Set<String> BLOODLESS = Set.of(
        "zombie","zombie_villager","husk","drowned","skeleton","stray","wither_skeleton",
        "phantom","wither","zoglin","zombified_piglin","skeleton_horse","zombie_horse",
        "iron_golem","snow_golem","shulker","armor_stand","blaze","magma_cube","ghast","slime",
        "enderman","endermite","ender_dragon","warden","allay","vex"
    );
    private static final Set<String> TOO_SMALL = Set.of(
        "bat","bee","rabbit","chicken","parrot","frog","tadpole",
        "cod","salmon","tropical_fish","pufferfish","silverfish","cave_spider"
    );

    private final float damagePerTick   = 0.5f;
    private final float healPerTick     = 0.25f;
    private final float resourcePerTick = 1.0f;
    private final int   ticksToStart    = 100;
    private final double maxRange       = 8.0;
    private static final Map<UUID, DrainState> DRAIN_STATES = new HashMap<>();

    public BloodDrainGazePassive() {
        super("blood_drain_gaze",
            "abyssorigins.passive.blood_drain_gaze.name",
            "abyssorigins.passive.blood_drain_gaze.desc");
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        UUID id = player.getUUID();
        DrainState state = DRAIN_STATES.computeIfAbsent(id, k -> new DrainState());
        if (!player.isShiftKeyDown()) { if (state.targetId != null) resetDrain(player, state); return; }
        LivingEntity target = getTarget(player);
        if (target == null) { if (state.targetId != null) resetDrain(player, state); return; }
        if (state.targetId == null || !state.targetId.equals(target.getUUID())) {
            state.targetId = target.getUUID(); state.ticks = 0; state.draining = false;
            player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_focusing",
                target.getName().getString()), true);
        }
        state.ticks++;
        if (!state.draining && state.ticks < ticksToStart && state.ticks % 20 == 0) {
            int pct = (state.ticks * 100) / ticksToStart;
            String bar = "\u00a74" + "\u2588".repeat(pct/10) + "\u00a78" + "\u2591".repeat(10-pct/10) + " \u00a7c" + pct + "%";
            player.displayClientMessage(Component.literal(bar), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.2f, 1.5f);
        }
        if (state.ticks >= ticksToStart) {
            if (!state.draining) {
                state.draining = true;
                player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_activated"), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
            drain(player, target, state);
        }
    }

    private void drain(Player player, LivingEntity target, DrainState state) {
        target.hurt(player.damageSources().magic(), damagePerTick);
        player.heal(healPerTick);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        data.addResource(resourcePerTick);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          40, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
        if (state.ticks % 10 == 0) spawnParticles(player, target);
        if (state.ticks % 20 == 0) player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.3f, 0.5f);
        if (target.isDeadOrDying()) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_drained", target.getName().getString()), false);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,  200, 0, false, true));
            data.addResource(30f);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 0.5f);
            resetDrain(player, state);
        }
    }

    private LivingEntity getTarget(Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(maxRange));
        AABB box = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(1.0);
        LivingEntity closest = null; double closestDist = maxRange;
        for (Entity e : player.level().getEntities(player, box, entity -> entity instanceof LivingEntity le && le.isAlive() && hasBlood(le))) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isPresent()) { double d = eye.distanceTo(hit.get()); if (d < closestDist) { closestDist = d; closest = (LivingEntity) e; } }
        }
        return closest;
    }

    private boolean hasBlood(LivingEntity e) {
        String id = EntityType.getKey(e.getType()).getPath();
        if (BLOODLESS.contains(id) || TOO_SMALL.contains(id)) return false;
        if (e instanceof Player || e instanceof Animal || e instanceof Squid || e instanceof AbstractVillager) return true;
        if (e instanceof Mob mob && mob.isInvertedHealAndHarm()) return false;
        if (e instanceof Monster) return !BLOODLESS.contains(id);
        return e.getBbHeight() >= 0.5f && e.getBbWidth() >= 0.3f;
    }

    private void spawnParticles(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Vec3 pPos = player.getEyePosition(), tPos = target.position().add(0, target.getBbHeight()/2, 0);
        Vec3 dir = pPos.subtract(tPos).normalize(); double dist = pPos.distanceTo(tPos);
        for (double d = 0; d < dist; d += 0.5) { Vec3 p = tPos.add(dir.scale(d)); sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.02); }
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tPos.x, tPos.y, tPos.z, 1, 0.2, 0.2, 0.2, 0);
        sl.sendParticles(ParticleTypes.HEART, pPos.x, pPos.y-0.5, pPos.z, 1, 0.2, 0.2, 0.2, 0);
    }

    private void resetDrain(Player player, DrainState state) {
        if (state.draining) player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_interrupted"), true);
        state.targetId = null; state.ticks = 0; state.draining = false;
    }

    @Override public void onEquip(Player player) {}
    @Override public void onRemove(Player player) { DRAIN_STATES.remove(player.getUUID()); }

    private static class DrainState { UUID targetId = null; int ticks = 0; boolean draining = false; }
}
