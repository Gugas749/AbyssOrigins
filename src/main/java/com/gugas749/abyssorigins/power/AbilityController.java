package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.data.PlayerAbilityData;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.origin.eminence.PresenceEvents;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Single activation path for every ability slot, origin and class alike.
 *
 * Order: slot → still known? → toggle-off for sustained abilities (allowed even on cooldown)
 * → cooldown → resource → canActivate → activate. Cost and cooldown are only consumed when
 * activate() returns true, so "no target" never wastes a cooldown. Cooldowns are kept per
 * ability id, so rebinding a power to another slot can't skip one.
 */
public final class AbilityController {

    /** Cooldowns can never be reduced below this fraction of base, nor below one second. */
    private static final float MIN_COOLDOWN_FACTOR = 0.25f;

    private AbilityController() {}

    public static void activate(ServerPlayer player, SlotGroup group, int slot) {
        if (!group.valid(slot) || player.isSpectator()) return;
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (group == SlotGroup.ORIGIN ? origin == null : Skills.specOf(player) == null) return;
        if (restrained(player)) return;

        PlayerAbilityData data = Loadouts.data(player);
        String id = data.slot(group, slot);
        Ability ability = id.isEmpty() ? null : AbilityRegistry.ability(id);
        if (ability == null) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.slot_unbound", slot + 1), true);
            return;
        }
        // Access can be revoked (or a skill respecced) while the power is still bound
        if (!Loadouts.knows(player, group, id)) {
            Loadouts.prune(player);
            ModPackets.syncAbilities(player);
            return;
        }

        if (toggleOff(player, ability)) { ModPackets.syncAbilities(player); return; }
        if (data.cooldown(id) > 0) { cooldownMessage(player, data.cooldown(id)); return; }

        // Powers are paid from the origin's resource; without one (Human, or no origin) they cost nothing.
        PlayerOriginData od = Skills.originData(player);
        ResourceType rt = origin != null ? origin.getResourceType() : null;
        int cost = rt == null ? 0 : ability.resourceCost(player);
        if (cost > 0 && od.getResource() < cost) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.not_enough_resource",
                rt.getDisplayName(), cost), true);
            return;
        }

        if (!ability.canActivate(player) || !ability.activate(player)) return;

        if (cost > 0) od.drainResource(cost);
        data.startCooldown(id, effectiveCooldown(player, ability));
        Powers.forEach(player, h -> h.onAbilityUsed(player, ability));
        if (cost > 0) ModPackets.syncPlayer(player);
        ModPackets.syncAbilities(player);
    }

    /** Held by a Presence, kneeling, or under a Royal Decree: no powers answer. */
    private static boolean restrained(ServerPlayer player) {
        if (PresenceEvents.held(player)) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.held"), true);
            return true;
        }
        if (player.hasEffect(ModEffects.SEALED)) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.sealed"), true);
            return true;
        }
        return false;
    }

    public static int effectiveCooldown(ServerPlayer player, Ability ability) {
        int base = ability.cooldownTicks(player);
        float mult = Math.max(MIN_COOLDOWN_FACTOR, Powers.product(player, h -> h.cooldownMultiplier(player, ability)));
        return Math.max(20, Math.round(base * mult));
    }

    /** Reduces a running cooldown — used by reset/refund mechanics. */
    public static void reduceCooldown(ServerPlayer player, String abilityId, int ticks) {
        Loadouts.data(player).reduceCooldown(abilityId, ticks);
        ModPackets.syncAbilities(player);
    }

    /** Reduces the cooldown of the class's {@code index}-th default power, wherever it is bound. */
    public static void reduceClassCooldown(ServerPlayer player, int index, int ticks) {
        Specialization spec = Skills.specOf(player);
        if (spec != null) reduceKit(player, spec.getAbilities(), index, ticks);
    }

    /** Reduces the cooldown of the origin's {@code index}-th default power, wherever it is bound. */
    public static void reduceOriginCooldown(ServerPlayer player, int index, int ticks) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin != null) reduceKit(player, origin.getAbilities(), index, ticks);
    }

    private static void reduceKit(ServerPlayer player, List<? extends Ability> kit, int index, int ticks) {
        if (index >= 0 && index < kit.size()) reduceCooldown(player, kit.get(index).getId(), ticks);
    }

    private static boolean toggleOff(ServerPlayer player, Ability ability) {
        if (ability instanceof Sustained && AbilityStates.isActive(player, ability.getId())) {
            AbilityStates.stop(player, ability.getId());
            return true;
        }
        return false;
    }

    private static void cooldownMessage(ServerPlayer player, int ticks) {
        player.displayClientMessage(Component.translatable("abyssorigins.message.on_cooldown",
            String.format("%.1f", ticks / 20f)), true);
    }
}
