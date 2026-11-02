package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.event.XpEventHandler;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.skilltree.SkillService;
import com.gugas749.abyssorigins.skilltree.registry.SkillEffectHandler;
import net.minecraft.server.level.ServerPlayer;

/**
 * Assigning / clearing origins and classes. Centralised so that every path
 * (commands, addons) ends sustained abilities, refunds the old tree, resets the
 * resource, re-seeds the ability loadout, rebuilds stats and re-broadcasts identity
 * in the same order.
 */
public final class Assignments {

    /** Class skill points granted on first assignment. */
    public static final int CLASS_STARTING_POINTS = 3;

    private Assignments() {}

    public static void setOrigin(ServerPlayer player, Origin origin) {
        Origin old = AbyssOriginsAPI.getPlayerOrigin(player);
        AbilityStates.stopAll(player);

        PlayerOriginData data = Skills.originData(player);
        boolean changed = old == null || origin == null || !old.getId().equals(origin.getId());
        if (changed && old != null) SkillService.respecOrigin(player); // refund the previous tree

        AbyssOriginsAPI.setPlayerOrigin(player, origin);
        data.setOriginId(origin != null ? origin.getId() : null);

        if (changed && origin != null) {
            data.setResource(origin.getResourceType() != null ? origin.getResourceType().getStartingAmount() : 100f);
            XpEventHandler.grantStartingPoints(player);
        }
        if (changed) Loadouts.reset(player, SlotGroup.ORIGIN);
        Loadouts.prune(player);

        SkillEffectHandler.refresh(player);
        ModPackets.syncAll(player);
        ModPackets.broadcastIdentity(player);
    }

    public static void setClass(ServerPlayer player, Specialization spec) {
        PlayerSpecializationData data = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        Specialization old = Skills.specOf(player);
        AbilityStates.stopAll(player);

        if (old != null) {
            old.getPassives().forEach(p -> safe(() -> p.onRemove(player)));
            if (spec == null || !old.getId().equals(spec.getId())) SkillService.respecClass(player);
        }

        data.setSpecId(spec != null ? spec.getId() : null);
        if (spec != null) {
            if (data.getSpecLevel() == 0) data.setSpecLevel(1);
            if (data.getSkillPoints() == 0 && data.getUnlockedSkills().isEmpty())
                data.addSkillPoints(CLASS_STARTING_POINTS);
            spec.getPassives().forEach(p -> safe(() -> p.onEquip(player)));
        }
        if (old == null || spec == null || !old.getId().equals(spec.getId())) Loadouts.reset(player, SlotGroup.CLASS);
        Loadouts.prune(player);

        SkillEffectHandler.refresh(player);
        ModPackets.syncAll(player);
        ModPackets.broadcastIdentity(player);
    }

    private static void safe(Runnable r) {
        try { r.run(); }
        catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Passive equip/remove error", e); }
    }
}
