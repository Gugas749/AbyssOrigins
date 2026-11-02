package com.gugas749.abyssorigins.origin.demon;

import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
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
 * Demon mechanics. Wrath is the heart of the kit: every blow given or taken stokes
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
        if (player.tickCount % 20 != 0) return;
        float wrath = Skills.resource(player);
        var level = player.serverLevel();

        // Doused: water saps infernal strength
        if (!Skills.has(player, "demon_tempered") && (player.isInWaterOrRain())) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));
            if (player.isInWater() && player.tickCount % 40 == 0) {
                float dmg = Skills.has(player, "demon_forbidden_pact") ? 3f : 1f;
                player.hurt(player.damageSources().magic(), dmg);
                Vfx.sparks(level, Fx.chest(player), 0.5f, 0xB8C0C8, 0xFFFFFF);
                Fx.sound(level, player.position(), SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.2f);
            }
        }

        // Nether Native
        if (player.level().dimension() == Level.NETHER)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, false, true));

        // Infernal Resilience
        if (Skills.has(player, "demon_infernal_resilience") && wrath >= 75f)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));

        // Archfiend: a burning aura while wrathful
        if (Skills.has(player, "demon_archfiend") && wrath >= 50f) {
            for (LivingEntity e : Targeting.enemiesAround(player, player.position(), 2.5)) {
                e.igniteForSeconds(2);
                e.hurt(Targeting.spell(player), 1f);
            }
            Vfx.of(VfxType.AURA).follow(player).scale(2.5f).colors(Palette.INFERNO, Palette.INFERNO_HI).duration(24).send(level);
        }

        // Ember Heart: peak wrath erupts on its own
        if (Skills.has(player, "demon_ember_heart") && wrath >= 95f
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
            Vfx.burst(player.serverLevel(), Fx.chest(target), 0.6f, Palette.SOULFIRE, 0xD8FFFF);
        }

        // Smoldering Wrath: above half wrath, fists carry fire
        if (wrath >= 50f) target.igniteForSeconds(3);
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        Skills.addResource(player, wrathGain(player, WRATH_ON_HIT));
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        // Hellborn Hide: fire and lava cannot burn what was born in them
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }
        Skills.addResource(player, wrathGain(player, WRATH_ON_HURT));

        // Brimstone Skin: melee attackers catch fire
        if (Skills.has(player, "demon_brimstone_skin") && event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker) {
            attacker.igniteForSeconds(3);
        }
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (Skills.has(player, "demon_bloodlust")) {
            Skills.addResource(player, 15f);
            Vfx.sparks(player.serverLevel(), Fx.chest(victim), 1.0f, Palette.INFERNO, Palette.INFERNO_HI);
        }
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        return ability.getId().equals("demon_hellfire_nova") && Skills.has(player, "demon_archfiend") ? 0.5f : 1f;
    }

    @Override
    public float resourceDrainMultiplier(ServerPlayer player) {
        return Skills.has(player, "demon_forbidden_pact") ? 0f : 1f;
    }
}
