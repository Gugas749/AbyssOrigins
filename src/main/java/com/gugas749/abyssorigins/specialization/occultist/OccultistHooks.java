package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Occult Arts mechanics. */
public class OccultistHooks implements PowerHooks {

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (Skills.has(player, "occ_shadow_rites") && Targeting.lightAt(player) <= 7)
            event.setAmount(event.getAmount() * 1.10f);
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        if (!Skills.has(player, "occ_soul_drain")) return;
        float pct = Skills.has(player, "occ_dark_covenant") ? 0.10f : 0.05f;
        player.heal(amount * pct);
    }

    @Override
    public void onAbilityUsed(ServerPlayer player, Ability ability) {
        // Umbral Form: slip half out of the world after every ability
        if (Skills.has(player, "occ_umbral_form")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 3, false, false));
            Vfx.sparks(player.serverLevel(), Fx.chest(player), 1.0f, Palette.OCCULT, Palette.OCCULT_HI);
        }
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        float m = 1f;
        if (ability instanceof OriginAbility && Skills.has(player, "occ_blood_ritual")) m *= 0.9f;
        if (ability.getId().equals("occult_hexbolt") && Skills.has(player, "occ_ascended_occultist")) m *= 0.5f;
        return m;
    }
}
