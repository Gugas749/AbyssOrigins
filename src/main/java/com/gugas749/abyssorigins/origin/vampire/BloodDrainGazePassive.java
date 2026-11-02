package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Blood Gaze: crouch and stare at a living creature to lock on, then drain it.
 * Mesmerize (vamp_charm) halves the focus time and roots the victim harder.
 */
public class BloodDrainGazePassive extends OriginPassive {

    private static final float  DAMAGE_PER_TICK   = 0.5f;
    private static final float  HEAL_PER_TICK     = 0.25f;
    private static final float  BLOOD_PER_TICK    = 1.0f;
    private static final int    FOCUS_TICKS       = 100;
    private static final int    FOCUS_TICKS_CHARM = 50;
    private static final double MAX_RANGE         = 8.0;
    private static final Map<UUID, DrainState> DRAIN_STATES = new HashMap<>();

    public BloodDrainGazePassive() {
        super("blood_drain_gaze",
            "abyssorigins.passive.blood_drain_gaze.name",
            "abyssorigins.passive.blood_drain_gaze.desc");
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        DrainState state = DRAIN_STATES.computeIfAbsent(player.getUUID(), k -> new DrainState());
        if (!player.isShiftKeyDown()) { if (state.targetId != null) resetDrain(player, state); return; }
        LivingEntity target = getTarget(player);
        if (target == null) { if (state.targetId != null) resetDrain(player, state); return; }
        if (state.targetId == null || !state.targetId.equals(target.getUUID())) {
            state.targetId = target.getUUID(); state.ticks = 0; state.draining = false;
            player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_focusing",
                target.getName().getString()), true);
        }

        int focus = Skills.has(player, "vamp_charm") ? FOCUS_TICKS_CHARM : FOCUS_TICKS;
        state.ticks++;
        if (!state.draining && state.ticks < focus && state.ticks % 10 == 0) {
            int pct = (state.ticks * 100) / focus;
            String bar = "§4" + "█".repeat(pct/10) + "§8" + "░".repeat(10-pct/10) + " §c" + pct + "%";
            player.displayClientMessage(Component.literal(bar), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.2f, 1.5f);
            if (player.level() instanceof ServerLevel sl)
                Vfx.beam(sl, Fx.hand(player), Fx.chest(target), 0.07f, Palette.BLOOD_DARK, Palette.BLOOD, 10);
        }
        if (state.ticks >= focus) {
            if (!state.draining) {
                state.draining = true;
                player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_activated"), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
            drain(player, target, state);
        }
    }

    private void drain(Player player, LivingEntity target, DrainState state) {
        target.hurt(player.damageSources().magic(), DAMAGE_PER_TICK);
        player.heal(HEAL_PER_TICK);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        data.addResource(BLOOD_PER_TICK);
        boolean charm = Skills.has(player, "vamp_charm");
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          40, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, charm ? 3 : 2, false, false));
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
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(MAX_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(MAX_RANGE)).inflate(1.0);
        LivingEntity closest = null; double closestDist = MAX_RANGE;
        for (Entity e : player.level().getEntities(player, box, entity -> entity instanceof LivingEntity le && VampireBlood.hasBlood(le))) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isPresent()) { double d = eye.distanceTo(hit.get()); if (d < closestDist) { closestDist = d; closest = (LivingEntity) e; } }
        }
        return closest;
    }

    /** A pulsing crimson stream from the victim to the vampire's mouth. */
    private void spawnParticles(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Vfx.beam(sl, Fx.chest(target), player.getEyePosition().subtract(0, 0.25, 0), 0.24f, Palette.BLOOD, Palette.BLOOD_HI, 12);
        Vfx.sparks(sl, Fx.chest(target), 0.4f, Palette.BLOOD, Palette.BLOOD_DARK);
    }

    private void resetDrain(Player player, DrainState state) {
        if (state.draining) player.displayClientMessage(Component.translatable("abyssorigins.message.blood_gaze_interrupted"), true);
        state.targetId = null; state.ticks = 0; state.draining = false;
    }

    @Override public void onRemove(Player player) { DRAIN_STATES.remove(player.getUUID()); }

    private static class DrainState { UUID targetId = null; int ticks = 0; boolean draining = false; }
}
