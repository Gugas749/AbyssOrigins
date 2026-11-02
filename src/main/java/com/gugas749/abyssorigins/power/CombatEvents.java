package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The combat pipeline. Applies the mod's damage-modifying effects, then dispatches
 * to the attacker's and the victim's origin/class PowerHooks.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class CombatEvents {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        DamageSource source = event.getSource();

        // ---- Effects on the victim ------------------------------------------
        MobEffectInstance hex = target.getEffect(ModEffects.HEXED);
        if (hex != null) event.setAmount(event.getAmount() * (1f + 0.15f * (hex.getAmplifier() + 1)));

        MobEffectInstance mark = target.getEffect(ModEffects.HUNTERS_MARK);
        if (mark != null) {
            if (Targeting.isProjectile(source)) event.setAmount(event.getAmount() * 1.25f);
            if (mark.getAmplifier() >= 1)       event.setAmount(event.getAmount() * 1.15f);
        }

        MobEffectInstance sanctified = target.getEffect(ModEffects.SANCTIFIED);
        if (sanctified != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float reduction = Math.min(0.8f, 0.2f * (sanctified.getAmplifier() + 1));
            event.setAmount(event.getAmount() * (1f - reduction));
        }

        // ---- Hooks ----------------------------------------------------------
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target)
            Powers.forEach(attacker, h -> h.onAttack(attacker, target, event));

        if (target instanceof ServerPlayer victim && !event.isCanceled())
            Powers.forEach(victim, h -> h.onHurt(victim, event));
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getNewDamage() <= 0) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != target)
            Powers.forEach(attacker, h -> h.onDamageDealt(attacker, target, event.getSource(), event.getNewDamage()));
    }

    /** Death prevention runs first — a cancelled death never counts as a kill. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLethal(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (Powers.any(victim, h -> h.onLethal(victim, event.getSource()))) {
            event.setCanceled(true);
            if (victim.getHealth() <= 0) victim.setHealth(1f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity())
            Powers.forEach(killer, h -> h.onKill(killer, event.getEntity(), event.getSource()));
    }
}
