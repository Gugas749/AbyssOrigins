package com.gugas749.abyssorigins.origin.demon;

import com.gugas749.abyssorigins.power.AuraToggles;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Demon mechanics. Wrath is the core resource: dealing or taking damage builds
 * it, it decays out of combat, and most nodes convert it into damage or survival.
 */
public class DemonHooks implements PowerHooks {

    private static final float WRATH_ON_HIT  = 6f;
    private static final float WRATH_ON_HURT = 4f;

    private static float wrathGain(ServerPlayer p, float base) {
        float g = Skills.has(p, "demon_kindling") ? base * 1.25f : base;
        return Gear.fullSet(p, "demon") ? g * 1.5f : g;              // Infernal Harness
    }

    @Override
    public void tick(ServerPlayer player) {
        // Unquenchable: Wrath never falls below 25 (checked every tick, after it decays)
        if (Skills.has(player, "demon_unquenchable") && Skills.resource(player) < 25f) Skills.originData(player).setResource(25f);

        if (player.tickCount % 20 != 0) return;
        float wrath = Skills.resource(player);
        var level = player.serverLevel();

        // Doused: weaker in water
        if (!Skills.has(player, "demon_tempered") && (player.isInWaterOrRain())) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));
            if (player.isInWater() && player.tickCount % 40 == 0) {
                float dmg = Skills.has(player, "demon_forbidden_pact") ? 3f : 1f;
                player.hurt(player.damageSources().magic(), dmg);
                AbyssVfx.sparks(level, Fx.chest(player), 0.5f, 0xB8C0C8, 0xFFFFFF);
                Fx.sound(level, player.position(), SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.2f);
            }
        }

        // Nether Native
        if (player.level().dimension() == Level.NETHER)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, false, true));

        // Infernal Resilience
        if (Skills.has(player, "demon_infernal_resilience") && wrath >= 75f)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));

        // Archfiend: fire aura at 50+ wrath
        if (AuraToggles.active(player, "demon_archfiend") && wrath >= 50f) {
            for (LivingEntity e : Targeting.enemiesAround(player, player.position(), 2.5)) {
                e.igniteForSeconds(2);
                e.hurt(Targeting.spell(player), 1f);
            }
            AbyssVfx.of(VfxType.AURA).follow(player).scale(2.5f).colors(Palette.INFERNO, Palette.INFERNO_HI).duration(24).send(level);
        }

        // Ember Heart: free nova at full wrath
        if (AuraToggles.active(player, "demon_ember_heart") && wrath >= 95f
                && PowerState.tryTrigger(player, "demon_ember_heart", 600)) {
            DemonAbilities.HellfireNova.erupt(player, 0.75f);
        }
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;
        float wrath = Skills.resource(player);

        if (Skills.has(player, "demon_wrathful_strikes"))
            event.setAmount(event.getAmount() * (1f + Math.min(0.20f, (wrath / 10f) * 0.02f)));

        if (Skills.has(player, "demon_soul_brand") && target.isOnFire()) {
            event.setAmount(event.getAmount() + 2f);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), player);
            AbyssVfx.burst(player.serverLevel(), Fx.chest(target), 0.6f, Palette.SOULFIRE, 0xD8FFFF);
        }

        // Spreading Flames: hitting a burning target sets those around it ablaze
        if (Skills.has(player, "demon_spreading_flames") && target.isOnFire())
            for (LivingEntity e : Targeting.enemiesAround(player, target.position(), 3))
                if (e != target) e.igniteForSeconds(3);

        // Pyre: above 75 wrath, hits erupt around the target
        if (Skills.has(player, "demon_pyre") && wrath > 75f && PowerState.tryTrigger(player, "demon_pyre", 20)) {
            for (LivingEntity e : Targeting.enemiesAround(player, target.position(), 2.5)) {
                if (e == target) continue;
                e.hurt(Targeting.spell(player), 3f);
                e.igniteForSeconds(3);
            }
            AbyssVfx.burst(player.serverLevel(), Fx.chest(target), 0.9f, Palette.INFERNO, Palette.INFERNO_HI);
            Fx.sound(player.level(), target.position(), SoundEvents.FIRECHARGE_USE, 0.6f, 0.8f);
        }

        // Smoldering Wrath: at 50+ wrath, melee hits ignite
        if (wrath >= 50f) target.igniteForSeconds(3);

        // Rage Mode: every hit applies Bleeding
        if (DemonAbilities.RageMode.raging(player)) {
            MobEffectInstance old = target.getEffect(ModEffects.BLEEDING);
            int amp = old == null ? 0 : Math.min(2, old.getAmplifier() + 1);   // repeated hits stack it up to III
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 100, amp), player);
            AbyssVfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(target.position().subtract(player.position()))
                .scale(0.9f).extra(120).colors(Palette.BLOOD, Palette.BLOOD_HI)
                .flags(player.getRandom().nextBoolean() ? AbyssVfx.VERTICAL : AbyssVfx.REVERSE).duration(10).send(player.serverLevel());
            AbyssVfx.sparks(player.serverLevel(), Fx.chest(target), 0.7f, Palette.BLOOD, Palette.BLOOD_DARK);
        }
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        Skills.addResource(player, wrathGain(player, WRATH_ON_HIT));
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        // Hellborn Hide: immune to fire and lava damage
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }
        Skills.addResource(player, wrathGain(player, WRATH_ON_HURT));

        // Molten Blood: melee attackers are burned for 30% of the damage (5 Wrath)
        if (Skills.has(player, "demon_molten_blood") && event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker && Skills.spendResource(player, 5f)) {
            attacker.hurt(Targeting.spell(player), event.getAmount() * 0.3f);
            AbyssVfx.sparks(player.serverLevel(), Fx.chest(attacker), 0.6f, Palette.INFERNO, Palette.INFERNO_HI);
        }

        // Brimstone Skin: melee attackers are set on fire
        if (Skills.has(player, "demon_brimstone_skin") && event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker) {
            attacker.igniteForSeconds(3);
        }
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        // Cinder Feast: burning kills heal and feed Wrath
        if (Skills.has(player, "demon_cinder_feast") && victim.isOnFire()) {
            player.heal(4f);
            Skills.addResource(player, 10f);
        }
        if (Skills.has(player, "demon_bloodlust")) {
            Skills.addResource(player, 15f);
            AbyssVfx.sparks(player.serverLevel(), Fx.chest(victim), 1.0f, Palette.INFERNO, Palette.INFERNO_HI);
        }
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        return ability.getId().equals("demon_hellfire_nova") && Skills.has(player, "demon_archfiend") ? 0.5f : 1f;
    }

    @Override
    public float resourceDrainMultiplier(ServerPlayer player) {
        if (Skills.has(player, "demon_unquenchable") && Skills.resource(player) <= 25f) return 0f;   // Unquenchable
        return Skills.has(player, "demon_forbidden_pact") ? 0f : 1f;
    }
}
