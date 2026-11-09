package com.gugas749.abyssorigins.specialization.assassin;

import com.gugas749.abyssorigins.power.AuraToggles;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.*;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Assassin mechanics — backstabs, poisons, and getting out alive. */
public class AssassinHooks implements PowerHooks {

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;
        var level = player.serverLevel();

        boolean backstab = Targeting.isBehind(player, target)
            || PowerState.consume(player, "assassin_ss")
            || PowerState.flagged(player, "meld_break");                 // Umbral Killer (Shade)

        float mult = 1f;
        if (backstab) {
            mult = Skills.has(player, "assassin_keen_edge") ? 1.75f : 1.5f;
            if (Skills.has(player, "assassin_deathmark") && target.getHealth() / target.getMaxHealth() < 0.25f) mult = 3f;
            Vec3 toward = target.position().subtract(player.position());
            Vfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(toward).scale(1.2f).extra(140).colors(Palette.ASSASSIN, Palette.ASSASSIN_HI)
                .flags(Vfx.VERTICAL).duration(11).send(level);
            Vfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(toward).scale(1.2f).extra(140).colors(Palette.ASSASSIN, Palette.ASSASSIN_HI)
                .flags(Vfx.VERTICAL | Vfx.REVERSE).duration(11).send(level);
            Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 1.4f);
            if (Skills.isOrigin(player, "vampire")) {                    // Crimson Blades
                Skills.addResource(player, 5f);
                player.heal(2f);
            }
        }
        if (Skills.has(player, "assassin_opportunist") && (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                || target.hasEffect(ModEffects.STUNNED) || target.hasEffect(MobEffects.BLINDNESS)))
            mult *= 1.2f;
        event.setAmount(event.getAmount() * mult);

        if (AbilityStates.isActive(player, AssassinAbilities.VENOM)) {
            boolean hemo = Skills.has(player, "assassin_hemorrhage");
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), player);
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, hemo ? 120 : 60, hemo ? 1 : 0), player);
            if (Skills.has(player, "assassin_toxicology"))
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), player);
            Vfx.sparks(level, Fx.chest(target), 0.7f, Palette.VENOM, Palette.VENOM_HI);
        }
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        var level = player.serverLevel();

        // Evasion: sidestep 15% of attacks from creatures
        if (Skills.has(player, "assassin_evasion") && src.getEntity() instanceof LivingEntity
                && player.getRandom().nextFloat() < 0.15f) {
            event.setCanceled(true);
            Vfx.of(VfxType.BURST).at(Fx.chest(player)).scale(0.6f).colors(Palette.SHADOW, Palette.ASSASSIN).flags(Vfx.VOID).duration(10).send(level);
            Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8f, 1.5f);
            return;
        }

        // Smoke Bomb: vanish when cornered (60s)
        if (AuraToggles.active(player, "assassin_smoke_bomb")
                && player.getHealth() - event.getAmount() < player.getMaxHealth() * 0.3f
                && PowerState.tryTrigger(player, "assassin_smoke", 1200)) {
            for (LivingEntity e : Targeting.enemiesAround(player, player.position(), 4))
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), player);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
            Vfx.of(VfxType.DOME).at(player.position()).scale(4).colors(0x14141C, Palette.SHADOW_HI).flags(Vfx.VOID | Vfx.HALF).duration(40).send(level);
            Fx.sound(level, player.position(), SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.6f);
        }
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (Skills.has(player, "assassin_cutthroat")) AbilityController.reduceClassCooldown(player, 0, 20 * 60);
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        return ability.getId().equals(AssassinAbilities.SHADOWSTEP) && Skills.has(player, "assassin_deathmark") ? 0.5f : 1f;
    }
}
