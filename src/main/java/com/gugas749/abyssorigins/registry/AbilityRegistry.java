package com.gugas749.abyssorigins.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Every ability of every Origin and Class, looked up by id.
 *
 * Built lazily from the origin / class registries (default kits and unlockable powers),
 * so abilities need no separate registration. Abilities of hidden origins (the Eminence)
 * are flagged hidden: left out of public listings, but still grantable and tracked by id.
 */
public final class AbilityRegistry {

    /** Where an ability comes from, for grouping and colouring it in the UI. */
    public record Entry(Ability ability, SlotGroup group, ResourceLocation owner,
                        Component ownerName, String ownerSigil, int ownerColor, boolean hidden) {
        public String id() { return ability.getId(); }
    }

    private static Map<String, Entry> entries;

    private AbilityRegistry() {}

    /** Called whenever an origin or class registers, so late addons are picked up. */
    public static void invalidate() { entries = null; }

    public static Entry get(String id) { return map().get(id); }

    public static Ability ability(String id) {
        Entry e = map().get(id);
        return e == null ? null : e.ability();
    }

    public static boolean exists(String id) { return map().containsKey(id); }

    /** Every ability, origins first (registration order), then classes. Includes hidden ones. */
    public static Collection<Entry> all() { return map().values(); }

    /** Abilities safe to list publicly (hidden origins' powers are left out). */
    public static List<Entry> visible() {
        return map().values().stream().filter(e -> !e.hidden()).toList();
    }

    private static Map<String, Entry> map() {
        if (entries == null) build();
        return entries;
    }

    private static void build() {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (Origin o : AbyssOriginsAPI.getAllOrigins()) {
            List<Ability> all = new ArrayList<>(o.getAbilities());
            all.addAll(o.getUnlockableAbilities());
            for (Ability a : all)
                put(map, new Entry(a, SlotGroup.ORIGIN, o.getId(), o.getDisplayName(), o.getSigil(), o.getThemeColor(), o.isHidden()));
        }
        for (Specialization s : SpecializationRegistry.getAll()) {
            List<Ability> all = new ArrayList<>(s.getAbilities());
            all.addAll(s.getUnlockableAbilities());
            for (Ability a : all)
                put(map, new Entry(a, SlotGroup.CLASS, s.getId(), s.getDisplayName(), s.getSigil(), s.getThemeColor(), false));
        }
        entries = Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, Entry> map, Entry e) {
        Entry old = map.putIfAbsent(e.id(), e);
        if (old != null && old.ability() != e.ability())
            Abyssorigins.LOGGER.warn("[AbyssOrigins] Duplicate ability id '{}' ({} and {}) — keeping the first",
                e.id(), old.owner(), e.owner());
    }
}
