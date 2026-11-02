package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.data.EminenceData;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerAbilityData;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * The slot system shared by every Origin and Class.
 *
 * A player <b>knows</b> an ability of a group when it is in their origin's / class's default
 * kit, inherited (the Eminence knows every origin power), taught by an unlocked skill node,
 * or granted directly. Known abilities can be bound into that group's slots; only bound
 * abilities can be activated. The known-set maths is pure so the client (Abilities tab,
 * staff profile) and the server agree; every mutation below is server-only and re-validated.
 */
public final class Loadouts {

    /** Why a player knows an ability — shown as a badge in the UI. */
    public enum Source { DEFAULT, INHERITED, SKILL, GRANTED }

    private Loadouts() {}

    public static PlayerAbilityData data(Player p) { return p.getData(OriginAttachments.ABILITIES.get()); }

    // ---- Known abilities (both sides) -----------------------------------------------------

    /**
     * Every ability id known in {@code group}, in display order, with the reason it is known.
     * Pure: works for the local player and for a staff snapshot of someone else alike.
     */
    public static Map<String, Source> known(SlotGroup group, Origin origin, Specialization spec,
                                            Collection<String> granted,
                                            Collection<String> originSkills, Collection<String> classSkills) {
        Map<String, Source> out = new LinkedHashMap<>();
        if (group == SlotGroup.ORIGIN) {
            if (origin != null) {
                origin.getAbilities().forEach(a -> out.putIfAbsent(a.getId(), Source.DEFAULT));
                if (Eminence.is(origin)) Eminence.bindable().keySet().forEach(id -> out.putIfAbsent(id, Source.INHERITED));
                SkillTreeRegistry.getTree(origin.getId()).ifPresent(t -> taught(t, originSkills, group, out));
            }
        } else if (spec != null) {
            spec.getAbilities().forEach(a -> out.putIfAbsent(a.getId(), Source.DEFAULT));
            if (spec.getSkillTree() != null) taught(spec.getSkillTree(), classSkills, group, out);
        }
        for (String id : granted) {
            AbilityRegistry.Entry e = AbilityRegistry.get(id);
            if (e != null && e.group() == group) out.putIfAbsent(id, Source.GRANTED);
        }
        return out;
    }

    private static void taught(SkillTree tree, Collection<String> unlocked, SlotGroup group, Map<String, Source> out) {
        for (String nodeId : unlocked) {
            SkillNode n = tree.getSkill(nodeId);
            if (n == null || !n.grantsAbility()) continue;
            AbilityRegistry.Entry e = AbilityRegistry.get(n.getGrantedAbility());
            if (e != null && e.group() == group) out.putIfAbsent(e.id(), Source.SKILL);
        }
    }

    /** Known abilities of a live player (server, or the local player on the client). */
    public static Map<String, Source> known(Player p, SlotGroup group) {
        return known(group, AbyssOriginsAPI.getPlayerOrigin(p), Skills.specOf(p), data(p).granted(),
            p.getData(SkillAttachments.PLAYER_SKILLS.get()).getUnlockedSkills(),
            p.getData(SpecializationAttachments.PLAYER_SPEC.get()).getUnlockedSkills());
    }

    public static boolean knows(Player p, SlotGroup group, String id) {
        return known(p, group).containsKey(id);
    }

    /** The ability bound to {@code slot}, or null when empty / unknown. */
    public static Ability bound(Player p, SlotGroup group, int slot) {
        String id = data(p).slot(group, slot);
        return id.isEmpty() ? null : AbilityRegistry.ability(id);
    }

    /**
     * Whether {@code id} may sit in {@code slot}: it must belong to the group, and the
     * class-specific slots only take powers owned by the player's current class.
     */
    public static boolean fits(SlotGroup group, int slot, String id, Specialization spec) {
        AbilityRegistry.Entry e = AbilityRegistry.get(id);
        if (e == null || e.group() != group || !group.valid(slot)) return false;
        return !group.isSpecific(slot) || (spec != null && spec.getId().equals(e.owner()));
    }

    // ---- Mutations (server) -----------------------------------------------------------------

    /** Re-seeds any loadout that was laid out for a different origin / class (or never laid out). */
    public static void ensure(ServerPlayer p) {
        PlayerAbilityData d = data(p);
        if (!d.loadoutOwner(SlotGroup.ORIGIN).equals(ownerKey(p, SlotGroup.ORIGIN))) reset(p, SlotGroup.ORIGIN);
        if (!d.loadoutOwner(SlotGroup.CLASS).equals(ownerKey(p, SlotGroup.CLASS)))   reset(p, SlotGroup.CLASS);
        prune(p);
    }

    /** Clears a loadout and lays the current origin's / class's default kit into the first slots. */
    public static void reset(ServerPlayer p, SlotGroup group) {
        PlayerAbilityData d = data(p);
        d.clearSlots(group);
        d.clearCooldowns(AbilityRegistry.all().stream().filter(e -> e.group() == group).map(AbilityRegistry.Entry::id).toList());
        d.setLoadoutOwner(group, ownerKey(p, group));

        if (group == SlotGroup.ORIGIN) {
            Origin origin = AbyssOriginsAPI.getPlayerOrigin(p);
            if (origin == null) return;
            if (Eminence.is(origin) && migrateEminence(p, d)) return;
            List<OriginAbility> kit = origin.getAbilities();
            for (int i = 0; i < Math.min(group.general(), kit.size()); i++) d.bind(group, i, kit.get(i).getId());
        } else {
            Specialization spec = Skills.specOf(p);
            if (spec == null) return;
            List<SpecializationAbility> kit = spec.getAbilities();
            for (int i = 0; i < Math.min(group.general(), kit.size()); i++) d.bind(group, i, kit.get(i).getId());
        }
    }

    /** Carries an Eminence's bindings from the old sovereign data over once. */
    private static boolean migrateEminence(ServerPlayer p, PlayerAbilityData d) {
        EminenceData legacy = p.getData(OriginAttachments.EMINENCE.get());
        if (!legacy.isInitialised()) return false;
        List<String> old = legacy.bindings();
        for (int i = 0; i < Math.min(SlotGroup.ORIGIN.size(), old.size()); i++)
            if (AbilityRegistry.exists(old.get(i))) d.bind(SlotGroup.ORIGIN, i, old.get(i));
        p.removeData(OriginAttachments.EMINENCE.get());
        return true;
    }

    /** Unbinds anything the player no longer knows or that no longer fits its slot. */
    public static boolean prune(ServerPlayer p) {
        PlayerAbilityData d = data(p);
        Specialization spec = Skills.specOf(p);
        boolean changed = false;
        for (SlotGroup g : SlotGroup.values()) {
            Map<String, Source> known = known(p, g);
            for (int i = 0; i < g.size(); i++) {
                String id = d.slot(g, i);
                if (id.isEmpty() || (known.containsKey(id) && fits(g, i, id, spec))) continue;
                d.bind(g, i, "");
                changed = true;
            }
        }
        return changed;
    }

    /** Player-requested binding (Abilities tab). Empty id unbinds. */
    public static boolean bind(ServerPlayer p, SlotGroup group, int slot, String id) {
        if (!group.valid(slot)) return false;
        if (!id.isEmpty() && (!knows(p, group, id) || !fits(group, slot, id, Skills.specOf(p)))) return false;
        data(p).bind(group, slot, id);
        ModPackets.syncAbilities(p);
        return true;
    }

    /**
     * Grants access to any registered ability — hidden ones (the Eminence's) included —
     * and drops it into the first free slot of its group. @return false if unknown id.
     */
    public static boolean grant(ServerPlayer p, String id) {
        AbilityRegistry.Entry e = AbilityRegistry.get(id);
        if (e == null) return false;
        data(p).grant(id);
        autoBind(p, id);
        ModPackets.syncAbilities(p);
        return true;
    }

    /** Removes a direct grant (default-kit / skill-taught access is unaffected). */
    public static boolean revoke(ServerPlayer p, String id) {
        if (!data(p).revoke(id)) return false;
        prune(p);
        ModPackets.syncAbilities(p);
        return true;
    }

    /** Places a newly known ability into the first empty general slot, if it isn't bound yet. */
    public static void autoBind(ServerPlayer p, String id) {
        AbilityRegistry.Entry e = AbilityRegistry.get(id);
        if (e == null || !knows(p, e.group(), id)) return;
        PlayerAbilityData d = data(p);
        if (d.slotOf(e.group(), id) >= 0) return;
        for (int i = 0; i < e.group().general(); i++)
            if (d.slot(e.group(), i).isEmpty()) { d.bind(e.group(), i, id); return; }
    }

    private static String ownerKey(Player p, SlotGroup group) {
        if (group == SlotGroup.ORIGIN) {
            Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
            return o == null ? "" : o.getId().toString();
        }
        Specialization s = Skills.specOf(p);
        return s == null ? "" : s.getId().toString();
    }
}
