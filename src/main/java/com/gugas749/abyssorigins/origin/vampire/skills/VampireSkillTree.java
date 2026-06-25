package com.gugas749.abyssorigins.origin.vampire.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Full vampire skill tree — imported from VeilOrigins' SkillTrees.registerVampireTree().
 * Adapted to AbyssOrigins' SkillNode/SkillTree API.
 *
 * Layout (col = x, row = y):
 *
 * TIER 1 (row 0) — Core foundation
 * TIER 2 (row 1) — Primary branches (Offense/Defense/Utility)
 * TIER 3 (row 2) — Elemental paths: Blood (left) vs Shadow (right) — MUTUALLY EXCLUSIVE
 * TIER 4 (row 3) — Advanced specialisation + Hybrid/Specialist/Synergy
 * TIER 5 (row 5) — Ultimate + Forbidden + Ascended (Daywalker = sun protection)
 */
public class VampireSkillTree {

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "Blood Arts"
        );

        // ============================================================
        // TIER 1 — Core Foundation
        // ============================================================
        tree.addNode(SkillNode.builder("vamp_vitality")
            .name("Vampiric Vitality").desc("Your undead body is more resilient. +2 hearts.")
            .icon("\u2665").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(0, 0)
            .effect("vamp_vitality").build());

        tree.addNode(SkillNode.builder("vamp_agility")
            .name("Predator's Grace").desc("Move with supernatural speed. +5% movement.")
            .icon("\u00bb").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(2, 0)
            .effect("vamp_agility").build());

        tree.addNode(SkillNode.builder("vamp_hunger")
            .name("Insatiable Hunger").desc("Blood sustains you longer. Blood drain rate reduced.")
            .icon("\u263E").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(4, 0)
            .effect("vamp_hunger").build());

        // ============================================================
        // TIER 2 — Primary Branches
        // ============================================================
        tree.addNode(SkillNode.builder("vamp_fangs")
            .name("Razor Fangs").desc("Your bite deals more damage. +10% damage.")
            .icon("\u2694").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-1, 1)
            .requires("vamp_vitality").effect("vamp_fangs").build());

        tree.addNode(SkillNode.builder("vamp_resilience")
            .name("Undead Resilience").desc("+2 armor, +1 heart. Harder to kill.")
            .icon("\u26E8").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(1, 1)
            .requires("vamp_vitality").effect("vamp_resilience").build());

        tree.addNode(SkillNode.builder("vamp_swiftness")
            .name("Bat's Swiftness").desc("+8% speed, -5% ability cooldowns.")
            .icon("\u263D").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(3, 1)
            .requires("vamp_agility").effect("vamp_swiftness").build());

        tree.addNode(SkillNode.builder("vamp_senses")
            .name("Predator Senses").desc("Detect living entities within 20 blocks.")
            .icon("\u2605").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(5, 1)
            .requires("vamp_hunger").effect("vamp_senses").build());

        tree.addNode(SkillNode.builder("vamp_charm")
            .name("Mesmerize").desc("Your gaze weakens the will of mortals.")
            .icon("\u25D0").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(4, 1)
            .requires("vamp_hunger").effect("vamp_charm").build());

        // ============================================================
        // TIER 3 — BLOOD PATH (ELEMENTAL_A) vs SHADOW PATH (ELEMENTAL_B)
        // ============================================================

        // --- BLOOD PATH ---
        tree.addNode(SkillNode.builder("vamp_blood_frenzy")
            .name("Blood Frenzy").desc("+25% damage, -10% defense. Attack speed increases at low HP.")
            .icon("\u2620").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-2, 2)
            .requires("vamp_fangs")
            .excludes("vamp_shadow_stalker", "vamp_shadow_cloak")
            .effect("vamp_blood_frenzy").build());

        tree.addNode(SkillNode.builder("vamp_blood_drain_skill")
            .name("Sanguine Drain").desc("10% of damage dealt heals you. +8% damage.")
            .icon("\u2666").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-1, 2)
            .requires("vamp_fangs")
            .excludes("vamp_shadow_strike")
            .effect("vamp_blood_drain_skill").build());

        // --- SHADOW PATH ---
        tree.addNode(SkillNode.builder("vamp_shadow_stalker")
            .name("Shadow Stalker").desc("+15% damage, +50% bonus damage from invisibility.")
            .icon("\u25D0").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(0, 2)
            .requires("vamp_fangs")
            .excludes("vamp_blood_frenzy", "vamp_blood_drain_skill")
            .effect("vamp_shadow_stalker").build());

        tree.addNode(SkillNode.builder("vamp_shadow_cloak")
            .name("Shadow Cloak").desc("Invisible in low light. +5% speed.")
            .icon("\u25D1").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(1, 2)
            .requires("vamp_resilience")
            .excludes("vamp_blood_frenzy")
            .effect("vamp_shadow_cloak").build());

        // --- DEFENSE SPECIALISATIONS ---
        tree.addNode(SkillNode.builder("vamp_immortal_flesh")
            .name("Immortal Flesh").desc("+5 hearts, +20% knockback resistance.")
            .icon("\u2665").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(2, 2)
            .requires("vamp_resilience")
            .excludes("vamp_rapid_regen")
            .effect("vamp_immortal_flesh").build());

        tree.addNode(SkillNode.builder("vamp_rapid_regen")
            .name("Rapid Regeneration").desc("+100% healing from blood. Blood drain reduced further.")
            .icon("\u271A").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(3, 2)
            .requires("vamp_resilience")
            .excludes("vamp_immortal_flesh")
            .effect("vamp_rapid_regen").build());

        // --- UTILITY SPECIALISATIONS ---
        tree.addNode(SkillNode.builder("vamp_night_hunter")
            .name("Night Hunter").desc("All stats +15% at night or underground.")
            .icon("\u263E").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(4, 2)
            .requires("vamp_swiftness")
            .effect("vamp_night_hunter").build());

        tree.addNode(SkillNode.builder("vamp_bat_mastery")
            .name("Bat Transformation").desc("Bat Form duration doubled. -10% ability cooldowns.")
            .icon("\uD83E\uDD87").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(5, 2)
            .requires("vamp_senses")
            .effect("vamp_bat_mastery").build());

        // ============================================================
        // TIER 4 — Advanced Specialisation
        // ============================================================
        tree.addNode(SkillNode.builder("vamp_blood_lord")
            .name("Blood Lord").desc("+20% damage, +15% attack speed. Kills cause blood explosions.")
            .icon("\u265B").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-2, 3)
            .requires("vamp_blood_frenzy")
            .effect("vamp_blood_lord").build());

        tree.addNode(SkillNode.builder("vamp_crimson_tide")
            .name("Crimson Tide").desc("Lifesteal now affects nearby enemies too. +12% damage.")
            .icon("\u224B").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-1, 3)
            .requires("vamp_blood_drain_skill")
            .effect("vamp_crimson_tide").build());

        tree.addNode(SkillNode.builder("vamp_shadow_master")
            .name("Shadow Master").desc("+18% damage. Teleport to nearby shadows.")
            .icon("\u25D0").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(0, 3)
            .requires("vamp_shadow_stalker")
            .effect("vamp_shadow_master").build());

        tree.addNode(SkillNode.builder("vamp_ancient_one")
            .name("Ancient One").desc("+4 hearts, +4 armor, +2 armor toughness.")
            .icon("\u269C").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(2, 3)
            .requires("vamp_immortal_flesh")
            .effect("vamp_ancient_one").build());

        tree.addNode(SkillNode.builder("vamp_mist_form")
            .name("Mist Form Mastery").desc("-20% cooldowns, +10% speed. Briefly invulnerable in mist.")
            .icon("\u224B").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(4).pos(4, 3)
            .requires("vamp_night_hunter")
            .effect("vamp_mist_form").build());

        tree.addNode(SkillNode.builder("vamp_hybrid_predator")
            .name("Apex Predator").desc("+10% damage, +2 hearts, +8% speed. Adapt to combat.")
            .icon("\u25C8").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(1, 3)
            .requires("vamp_shadow_cloak", "vamp_rapid_regen")
            .effect("vamp_hybrid_predator").build());

        tree.addNode(SkillNode.builder("vamp_specialist_assassin")
            .name("Perfect Assassin").desc("+40% damage. Instant kill below 15% HP. Very fragile.")
            .icon("\u2620").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(3, 3)
            .requires("vamp_shadow_stalker", "vamp_night_hunter")
            .excludes("vamp_ancient_one", "vamp_hybrid_predator")
            .effect("vamp_specialist_assassin").build());

        tree.addNode(SkillNode.builder("vamp_synergy_bloodshadow")
            .name("Blood Shadow").desc("Lifesteal while invisible. Leave damaging shadow clones.")
            .icon("\u25CE").tier(SkillTier.TIER_4).branch(SkillBranch.SYNERGY_AD).cost(4).pos(-1, 4)
            .requires("vamp_blood_drain_skill", "vamp_shadow_cloak")
            .effect("vamp_synergy_bloodshadow").build());

        // ============================================================
        // TIER 5 — Ultimate + Forbidden + Ascended
        // ============================================================
        tree.addNode(SkillNode.builder("vamp_nosferatu")
            .name("Nosferatu").desc("The pinnacle of vampiric power. All abilities enhanced.")
            .icon("\u262C").tier(SkillTier.TIER_5).branch(SkillBranch.CORE).cost(5).pos(1, 5)
            .requires("vamp_blood_lord", "vamp_ancient_one")
            .effect("vamp_nosferatu").build());

        // FORBIDDEN — more power, worse sun curse
        tree.addNode(SkillNode.builder("vamp_forbidden_bloodpact")
            .name("Blood Pact").desc("+35% damage, +25% lifesteal. Take 3x sun damage. PERMANENT.")
            .icon("\u2620").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(-2, 5)
            .requires("vamp_blood_lord", "vamp_crimson_tide")
            .excludes("vamp_ascended_daywalker")
            .effect("vamp_forbidden_bloodpact").build());

        // ASCENDED — the only way to gain full sun immunity
        tree.addNode(SkillNode.builder("vamp_ascended_daywalker")
            .name("Daywalker").desc("\u00a7eImmune to sun damage.\u00a7r Walk freely in daylight. +2 hearts.")
            .icon("\u2600").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(3, 5)
            .requires("vamp_ancient_one", "vamp_mist_form")
            .excludes("vamp_forbidden_bloodpact")
            .effect("vamp_ascended_daywalker").build());

        tree.addNode(SkillNode.builder("vamp_ascended_progenitor")
            .name("Progenitor").desc("+20% damage, +4 hearts. Thralls provide passive blood.")
            .icon("\u269C").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(4, 5)
            .requires("vamp_ascended_daywalker")
            .effect("vamp_ascended_progenitor").build());

        SkillTreeRegistry.register(tree);
    }
}
