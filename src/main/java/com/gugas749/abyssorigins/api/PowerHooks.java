package com.gugas749.abyssorigins.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Behavioural hooks an Origin or Class plugs into the combat / tick pipeline.
 * This is where skill-tree nodes with real mechanics live — each origin and class
 * owns one implementation, dispatched by CombatEvents and the tick handlers.
 *
 * All methods are server-side only.
 */
public interface PowerHooks {

    PowerHooks NONE = new PowerHooks() {};

    /** Every server tick. Prefer {@code player.tickCount % n} gating for periodic work. */
    default void tick(ServerPlayer player) {}

    /** The player is about to damage {@code target}. Modify {@code event.setAmount()} for bonuses. */
    default void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {}

    /** Final damage (after armour) the player dealt to {@code target}. Good for lifesteal. */
    default void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {}

    /** The player is about to take damage. Cancel or scale the event here. */
    default void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {}

    /** The player killed {@code victim}. */
    default void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {}

    /**
     * The player is about to die. Return true to prevent the death — the hook is then
     * responsible for setting health / applying effects.
     */
    default boolean onLethal(ServerPlayer player, DamageSource source) { return false; }

    /** Called after any ability (origin or class) was successfully activated. */
    default void onAbilityUsed(ServerPlayer player, Ability ability) {}

    /** Multiplier applied to the cooldown of {@code ability}. Hooks from origin and class stack. */
    default float cooldownMultiplier(ServerPlayer player, Ability ability) { return 1f; }

    /** Multiplier applied to the origin resource's passive drain (e.g. blood). */
    default float resourceDrainMultiplier(ServerPlayer player) { return 1f; }
}
