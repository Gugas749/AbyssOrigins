package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.power.AuraToggles;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Every Blood Arts node with behaviour beyond plain stats.
 * Paths: Blood (frenzy, lifesteal, explosions), Shadow (invisibility ambushes),
 * Flesh (undying), Night (mobility, cooldowns).
 */
public class VampireHooks implements PowerHooks {

    @Override
    public void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;

        boolean dark = !player.level().isDay() || Targeting.lightAt(player) < 8;

        // Night Hunter: Strength, Speed and Resistance at night / underground
        if (Skills.has(player, "vamp_night_hunter") && dark) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,      30, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,    30, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
        }

        // Shadow Stalker: invisibility in low light
        if (Skills.has(player, "vamp_shadow_stalker") && Targeting.lightAt(player) < 8
                && !player.hasEffect(MobEffects.INVISIBILITY))
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));

        // Blood Frenzy: Strength as health drops
        if (Skills.has(player, "vamp_blood_frenzy")) {
            float hp = player.getHealth() / player.getMaxHealth();
            if (hp < 0.5f)
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, hp < 0.25f ? 1 : 0, false, false, true));
        }

        // Undead Resilience: immune to poison
        if (Skills.has(player, "vamp_resilience") && player.hasEffect(MobEffects.POISON))
            player.removeEffect(MobEffects.POISON);

        // Rapid Regeneration: convert blood into flesh every 4s
        if (Skills.has(player, "vamp_rapid_regen") && player.tickCount % 80 == 0
                && player.getHealth() < player.getMaxHealth() && Skills.resource(player) > 30f) {
            player.heal(2f);
            Skills.originData(player).drainResource(2f);
            AbyssVfx.sparks(player.serverLevel(), Fx.chest(player), 0.6f, Palette.BLOOD, Palette.BLOOD_HI);
        }

        // Day-walker: passive blood gain at night
        if (Skills.has(player, "vamp_ascended_daywalker") && !player.level().isDay() && player.tickCount % 60 == 0)
            Skills.addResource(player, 1f);
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;
        boolean invisible = player.hasEffect(MobEffects.INVISIBILITY);

        if (Skills.has(player, "vamp_fangs"))
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0), player);

        if (invisible && Skills.has(player, "vamp_shadow_stalker"))
            event.setAmount(event.getAmount() * 1.5f);

        if (invisible && Skills.has(player, "vamp_shadow_master")) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,  60, 0), player);
        }

        // Shadow Master: execute weakened prey (non-boss mobs die outright, players take double)
        if (Skills.has(player, "vamp_shadow_master") && target.getHealth() / target.getMaxHealth() < 0.15f) {
            if (target instanceof Player) event.setAmount(event.getAmount() * 2f);
            else if (!target.getType().is(Tags.EntityTypes.BOSSES)) event.setAmount(target.getHealth() + 20f);
            AbyssVfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(target.position().subtract(player.position())).scale(1.3f).extra(150)
                .colors(Palette.BLOOD, Palette.BLOOD_HI).flags(AbyssVfx.VERTICAL).duration(12).send(player.serverLevel());
        }
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        if (!Targeting.isMelee(source, player) || !VampireBlood.hasBlood(target)) return;
        float lifesteal = 0f;
        if (Skills.has(player, "vamp_blood_frenzy"))        lifesteal += 0.10f;
        if (Skills.has(player, "vamp_forbidden_bloodpact")) lifesteal += 0.25f;
        if (lifesteal > 0f) player.heal(amount * lifesteal);
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        var level = player.serverLevel();

        // Blood Lord: killed targets burst, applying Bleeding nearby
        if (AuraToggles.active(player, "vamp_blood_lord") && VampireBlood.hasBlood(victim)) {
            for (LivingEntity e : Targeting.enemiesAround(player, victim.position(), 4)) {
                if (e == victim) continue;
                e.hurt(Targeting.spell(player), 4f);
                e.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80, 0), player);
            }
            AbyssVfx.burst(level, Fx.chest(victim), 1.6f, Palette.BLOOD, Palette.BLOOD_HI);
            AbyssVfx.shockwave(level, victim.position(), 4, Palette.BLOOD, Palette.BLOOD_HI, 16);
            Fx.sound(level, victim.position(), SoundEvents.SLIME_SQUISH, 1.0f, 0.5f);
        }

    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) && Skills.has(player, "vamp_agility"))
            event.setAmount(event.getAmount() * 0.5f);

        // Mist Form Mastery: avoid a heavy hit by turning to mist (60s)
        if (Skills.has(player, "vamp_mist_form")
                && player.getHealth() - event.getAmount() < player.getMaxHealth() * 0.3f
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && PowerState.tryTrigger(player, "vamp_mist", 1200)) {
            event.setCanceled(true);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            var level = player.serverLevel();
            AbyssVfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.4f).colors(0x2A0610, Palette.BLOOD).flags(AbyssVfx.VOID).duration(16).send(level);
            AbyssVfx.of(VfxType.AURA).follow(player).scale(1.1f).colors(0x2A0610, Palette.BLOOD).flags(AbyssVfx.VOID).duration(30).send(level);
            Fx.sound(level, player.position(), SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.6f);
            player.displayClientMessage(Component.translatable("abyssorigins.message.vamp_mist"), true);
        }
    }

    @Override
    public boolean onLethal(ServerPlayer player, DamageSource source) {
        // Immortal Flesh: spend 50 blood to survive a lethal hit (2 min)
        if (!Skills.has(player, "vamp_immortal_flesh") || Skills.resource(player) < 50f) return false;
        if (!PowerState.tryTrigger(player, "vamp_undying", 2400)) return false;

        Skills.originData(player).drainResource(50f);
        player.setHealth(6f);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
        var level = player.serverLevel();
        AbyssVfx.helix(level, player, 2.4f, Palette.BLOOD, Palette.BLOOD_HI, 30);
        AbyssVfx.of(VfxType.PILLAR).at(player.position()).scale(6).extra(0.8f).colors(Palette.BLOOD, Palette.BLOOD_HI).duration(30).send(level);
        Fx.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, 1.2f, 0.7f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.vamp_undying"), true);
        return true;
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        if (!(ability instanceof OriginAbility)) return 1f;
        float m = 1f;
        if (Skills.has(player, "vamp_agility"))      m *= 0.95f;
        if (Skills.has(player, "vamp_night_hunter")) m *= 0.90f;
        if (Skills.has(player, "vamp_mist_form"))   m *= 0.80f;
        if (Skills.has(player, "vamp_nosferatu"))   m *= 0.75f;
        return m;
    }

    @Override
    public float resourceDrainMultiplier(ServerPlayer player) {
        float m = 1f;
        if (Skills.has(player, "vamp_hunger"))      m *= 0.70f;
        if (Skills.has(player, "vamp_rapid_regen")) m *= 0.75f;
        return m;
    }
}
