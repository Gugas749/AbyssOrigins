package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * The Eminence — the hidden sovereign origin. It holds every other origin's skills and
 * powers: all of them are "known" to it, so any five can sit in its origin slots
 * (bound in the Abilities tab like any other origin's).
 */
public final class Eminence {

    public static final String PATH = "eminence";
    public static final int COLOR = 0xF2C14E, COLOR_HI = 0xFFF1C8, COLOR_DARK = 0x3A2408;

    private static Set<String> originSkills;

    private Eminence() {}

    public static boolean is(Player p) {
        Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
        return o != null && PATH.equals(o.getPath());
    }

    public static boolean is(Origin o) { return o != null && PATH.equals(o.getPath()); }

    /** True for every node of every origin skill tree when {@code p} is the Eminence. */
    public static boolean grantsSkill(Player p, String skillId) {
        if (!is(p)) return false;
        if (originSkills == null) {
            Set<String> all = new HashSet<>();
            for (SkillTree tree : SkillTreeRegistry.getAllTrees())
                for (SkillNode node : tree.getNodes()) all.add(node.getId());
            originSkills = all;
        }
        return originSkills.contains(skillId);
    }

    /**
     * Every origin power the crown may bind: its own first, then each origin's in registration order.
     * Hidden powers are not origin powers — the crown only holds them when granted, like anyone else.
     */
    public static Map<String, OriginAbility> bindable() {
        Map<String, OriginAbility> map = new LinkedHashMap<>();
        List<AbilityRegistry.Entry> origin = AbilityRegistry.all().stream()
            .filter(e -> e.group() == SlotGroup.ORIGIN && !e.grantOnly() && e.ability() instanceof OriginAbility).toList();
        origin.stream().filter(e -> PATH.equals(e.owner().getPath()))
            .forEach(e -> map.put(e.id(), (OriginAbility) e.ability()));
        origin.forEach(e -> map.putIfAbsent(e.id(), (OriginAbility) e.ability()));
        return map;
    }
}
