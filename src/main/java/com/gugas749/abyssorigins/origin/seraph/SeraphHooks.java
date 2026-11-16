package com.gugas749.abyssorigins.origin.seraph;

import com.gugas749.abyssorigins.power.AuraToggles;
import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Seraph mechanics. Radiance is gained under open sky; the Seraph is fragile
 * and weakened deep underground, and strong in the air and against undead.
 * The Fallen Grace capstone shifts the kit towards darkness.
 */
public class SeraphHooks implements PowerHooks {

    @Override
    public void tick(ServerPlayer player) {
        // Feathered Descent: hold sneak mid-air to drift
        if (player.isShiftKeyDown() && !player.onGround() && player.getDeltaMovement().y < -0.1)
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));

        if (player.tickCount % 20 != 0) return;
        var level = player.serverLevel();
        boolean fallen = Skills.has(player, "seraph_fallen_grace");

        // Sunlit Grace: gain radiance under open sky
        float gain = 0f;
        if (level.canSeeSky(player.blockPosition())) gain = level.isDay() ? 2f : 0.6f;
        if (fallen && Targeting.lightAt(player) < 7) gain = Math.max(gain, 1.5f);
        if (Skills.has(player, "seraph_halo")) gain *= 1.5f;
        boolean choir = Gear.fullSet(player, "seraph");                // Choir Vestments
        if (choir) gain *= 1.5f;
        if (gain > 0) Skills.addResource(player, gain);

        // Earthbound Burden: weakened far from the sky
        int depth = Skills.has(player, "seraph_skyward") ? 0 : 50;
        if (!fallen && !choir && !level.canSeeSky(player.blockPosition()) && player.getY() < depth)
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));

        // Light Ward: Absorption every 10s above 50 radiance
        if (Skills.has(player, "seraph_light_ward") && player.tickCount % 200 == 0 && Skills.resource(player) > 50f) {
            Skills.originData(player).drainResource(5f);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true));
        }

        // Beacon of Dawn: spends radiance to buff nearby allies
        if (AuraToggles.active(player, "seraph_beacon") && player.tickCount % 100 == 0 && Skills.resource(player) > 50f) {
            Skills.originData(player).drainResource(1f);
            for (LivingEntity ally : Targeting.alliesAround(player, player.position(), 6))
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true), player);
            AbyssVfx.shockwave(level, player.position(), 6, Palette.RADIANT, Palette.RADIANT_HI, 20);
        }
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        boolean undead = Targeting.isUndead(target);
        boolean fallen = Skills.has(player, "seraph_fallen_grace");

        // Holy Bane
        if (undead && !fallen) event.setAmount(event.getAmount() * 1.25f);

        if (!Targeting.isMelee(event.getSource(), player)) return;
        var level = player.serverLevel();

        // Seraphic Fury: +20% above 75 radiance
        if (Skills.has(player, "seraph_fury") && Skills.resource(player) > 75f) event.setAmount(event.getAmount() * 1.2f);

        // Sacred Edge: 2 radiance for +3 damage
        if (Skills.has(player, "seraph_sacred_edge") && Skills.spendResource(player, 2f)) event.setAmount(event.getAmount() + 3f);

        // Blinding Light: 20% chance to blind
        if (Skills.has(player, "seraph_blinding_light") && player.getRandom().nextFloat() < 0.2f)
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), player);

        if (undead && Skills.has(player, "seraph_smite")) {
            event.setAmount(event.getAmount() + 3f);
            target.igniteForSeconds(4);
            AbyssVfx.burst(level, Fx.chest(target), 0.5f, Palette.RADIANT, Palette.RADIANT_HI);
        }

        // Wrath of Heaven: falling critical hits add a light strike
        boolean crit = player.fallDistance > 0 && !player.onGround() && !player.isInWater();
        if (crit && Skills.has(player, "seraph_wrath_of_heaven") && PowerState.tryTrigger(player, "seraph_lance", 60)) {
            event.setAmount(event.getAmount() + 4f);
            Vec3 t = target.position();
            AbyssVfx.of(VfxType.BOLT).at(t.add(0, 11, 0)).to(t.add(0, 1, 0)).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(10).send(level);
            AbyssVfx.burst(level, t.add(0, 1, 0), 1.1f, Palette.RADIANT, Palette.RADIANT_HI);
            Fx.sound(level, t, SoundEvents.TRIDENT_THUNDER, 0.5f, 1.8f);
        }
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        // Feathered Descent: no fall damage
        if (event.getSource().is(DamageTypeTags.IS_FALL)) { event.setCanceled(true); return; }

        // Guardian Wings: 20% less damage while airborne
        if (Skills.has(player, "seraph_guardian_wings") && !player.onGround() && !player.isInWater())
            event.setAmount(event.getAmount() * 0.8f);

        // Guardian Halo: a shield when dropping below 30% health
        if (Skills.has(player, "seraph_aegis")
                && player.getHealth() - event.getAmount() < player.getMaxHealth() * 0.3f
                && PowerState.tryTrigger(player, "seraph_aegis", 1200)) {
            player.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, 120, 0), player);
            AbyssVfx.of(VfxType.DOME).follow(player).scale(1.4f).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(120).send(player.serverLevel());
            Fx.sound(player.level(), player.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f);
        }
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        return ability.getId().equals("seraph_divine_light") && Skills.has(player, "seraph_ascension") ? 0.5f : 1f;
    }
}
