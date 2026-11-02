package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.data.PlayerAbilityData;
import com.gugas749.abyssorigins.network.packet.ActivateAbilityPacket;
import com.gugas749.abyssorigins.network.packet.ActivateSpecAbilityPacket;
import com.gugas749.abyssorigins.network.packet.BindAbilityPacket;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * Client view of the local player's loadouts, read straight from the synced attachment
 * (SyncAbilitiesPacket). Used by the HUD, the Codex and the Abilities tab.
 */
public final class ClientAbilityData {

    private static final PlayerAbilityData EMPTY = new PlayerAbilityData();

    private ClientAbilityData() {}

    public static PlayerAbilityData data() {
        var p = Minecraft.getInstance().player;
        return p == null ? EMPTY : Loadouts.data(p);
    }

    /** Smooth countdown between syncs; once per client tick. */
    public static void tick() { data().tick(); }

    public static String slot(SlotGroup g, int i)   { return data().slot(g, i); }

    public static Ability ability(SlotGroup g, int i) {
        String id = slot(g, i);
        return id.isEmpty() ? null : AbilityRegistry.ability(id);
    }

    public static int cooldown(String id)    { return data().cooldown(id); }
    public static int cooldownMax(String id) { return data().cooldownMax(id); }

    public static Map<String, Loadouts.Source> known(SlotGroup g) {
        var p = Minecraft.getInstance().player;
        return p == null ? Map.of() : Loadouts.known(p, g);
    }

    /**
     * How many slots of a group the HUD shows: every slot up to the last bound one, and enough
     * empty general slots to hint that known-but-unbound powers are waiting to be slotted.
     */
    public static int visibleSlots(SlotGroup g) {
        int last = -1;
        for (int i = 0; i < g.size(); i++) if (!slot(g, i).isEmpty()) last = i;
        int known = Math.min(g.general(), known(g).size());
        return Math.max(last + 1, known);
    }

    public static void activate(SlotGroup g, int slot) {
        if (slot(g, slot).isEmpty()) {
            var p = Minecraft.getInstance().player;
            if (p != null) p.displayClientMessage(Component.translatable("abyssorigins.message.slot_unbound", slot + 1), true);
            return;
        }
        PacketDistributor.sendToServer(g == SlotGroup.ORIGIN ? new ActivateAbilityPacket(slot) : new ActivateSpecAbilityPacket(slot));
    }

    public static void bind(SlotGroup g, int slot, String abilityId) {
        PacketDistributor.sendToServer(new BindAbilityPacket(g, slot, abilityId));
    }
}
