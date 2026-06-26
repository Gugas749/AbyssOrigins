package com.gugas749.abyssorigins.origin.vampire.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;

public class VampireSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "abyssorigins.skilltree.vampire.name"
        );

        // TIER 1
        tree.addNode(SkillNode.builder("vamp_vitality")
            .name(P+"vamp_vitality.name").desc(P+"vamp_vitality.desc")
            .icon("\u2665").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(0,0)
            .effect("vamp_vitality").build());
        tree.addNode(SkillNode.builder("vamp_agility")
            .name(P+"vamp_agility.name").desc(P+"vamp_agility.desc")
            .icon("\u00bb").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(2,0)
            .effect("vamp_agility").build());
        tree.addNode(SkillNode.builder("vamp_hunger")
            .name(P+"vamp_hunger.name").desc(P+"vamp_hunger.desc")
            .icon("\u263E").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(4,0)
            .effect("vamp_hunger").build());

        // TIER 2
        tree.addNode(SkillNode.builder("vamp_fangs")
            .name(P+"vamp_fangs.name").desc(P+"vamp_fangs.desc")
            .icon("\u2694").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-1,1)
            .requires("vamp_vitality").effect("vamp_fangs").build());
        tree.addNode(SkillNode.builder("vamp_resilience")
            .name(P+"vamp_resilience.name").desc(P+"vamp_resilience.desc")
            .icon("\u26E8").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(1,1)
            .requires("vamp_vitality").effect("vamp_resilience").build());
        tree.addNode(SkillNode.builder("vamp_swiftness")
            .name(P+"vamp_swiftness.name").desc(P+"vamp_swiftness.desc")
            .icon("\u263D").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(3,1)
            .requires("vamp_agility").effect("vamp_swiftness").build());
        tree.addNode(SkillNode.builder("vamp_senses")
            .name(P+"vamp_senses.name").desc(P+"vamp_senses.desc")
            .icon("\u2605").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(5,1)
            .requires("vamp_hunger").effect("vamp_senses").build());
        tree.addNode(SkillNode.builder("vamp_charm")
            .name(P+"vamp_charm.name").desc(P+"vamp_charm.desc")
            .icon("\u25D0").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(4,1)
            .requires("vamp_hunger").effect("vamp_charm").build());

        // TIER 3 — Blood Path
        tree.addNode(SkillNode.builder("vamp_blood_frenzy")
            .name(P+"vamp_blood_frenzy.name").desc(P+"vamp_blood_frenzy.desc")
            .icon("\u2620").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-2,2)
            .requires("vamp_fangs").excludes("vamp_shadow_stalker","vamp_shadow_cloak")
            .effect("vamp_blood_frenzy").build());
        tree.addNode(SkillNode.builder("vamp_blood_drain_skill")
            .name(P+"vamp_blood_drain_skill.name").desc(P+"vamp_blood_drain_skill.desc")
            .icon("\u2666").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-1,2)
            .requires("vamp_fangs").excludes("vamp_shadow_strike")
            .effect("vamp_blood_drain_skill").build());

        // TIER 3 — Shadow Path
        tree.addNode(SkillNode.builder("vamp_shadow_stalker")
            .name(P+"vamp_shadow_stalker.name").desc(P+"vamp_shadow_stalker.desc")
            .icon("\u25D0").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(0,2)
            .requires("vamp_fangs").excludes("vamp_blood_frenzy","vamp_blood_drain_skill")
            .effect("vamp_shadow_stalker").build());
        tree.addNode(SkillNode.builder("vamp_shadow_cloak")
            .name(P+"vamp_shadow_cloak.name").desc(P+"vamp_shadow_cloak.desc")
            .icon("\u25D1").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(1,2)
            .requires("vamp_resilience").excludes("vamp_blood_frenzy")
            .effect("vamp_shadow_cloak").build());

        // TIER 3 — Defense
        tree.addNode(SkillNode.builder("vamp_immortal_flesh")
            .name(P+"vamp_immortal_flesh.name").desc(P+"vamp_immortal_flesh.desc")
            .icon("\u2665").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(2,2)
            .requires("vamp_resilience").excludes("vamp_rapid_regen")
            .effect("vamp_immortal_flesh").build());
        tree.addNode(SkillNode.builder("vamp_rapid_regen")
            .name(P+"vamp_rapid_regen.name").desc(P+"vamp_rapid_regen.desc")
            .icon("\u271A").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(3,2)
            .requires("vamp_resilience").excludes("vamp_immortal_flesh")
            .effect("vamp_rapid_regen").build());

        // TIER 3 — Utility
        tree.addNode(SkillNode.builder("vamp_night_hunter")
            .name(P+"vamp_night_hunter.name").desc(P+"vamp_night_hunter.desc")
            .icon("\u263E").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(4,2)
            .requires("vamp_swiftness").effect("vamp_night_hunter").build());
        tree.addNode(SkillNode.builder("vamp_bat_mastery")
            .name(P+"vamp_bat_mastery.name").desc(P+"vamp_bat_mastery.desc")
            .icon("\uD83E\uDD87").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(5,2)
            .requires("vamp_senses").effect("vamp_bat_mastery").build());

        // TIER 4
        tree.addNode(SkillNode.builder("vamp_blood_lord")
            .name(P+"vamp_blood_lord.name").desc(P+"vamp_blood_lord.desc")
            .icon("\u265B").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-2,3)
            .requires("vamp_blood_frenzy").effect("vamp_blood_lord").build());
        tree.addNode(SkillNode.builder("vamp_crimson_tide")
            .name(P+"vamp_crimson_tide.name").desc(P+"vamp_crimson_tide.desc")
            .icon("\u224B").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-1,3)
            .requires("vamp_blood_drain_skill").effect("vamp_crimson_tide").build());
        tree.addNode(SkillNode.builder("vamp_shadow_master")
            .name(P+"vamp_shadow_master.name").desc(P+"vamp_shadow_master.desc")
            .icon("\u25D0").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(0,3)
            .requires("vamp_shadow_stalker").effect("vamp_shadow_master").build());
        tree.addNode(SkillNode.builder("vamp_ancient_one")
            .name(P+"vamp_ancient_one.name").desc(P+"vamp_ancient_one.desc")
            .icon("\u269C").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(2,3)
            .requires("vamp_immortal_flesh").effect("vamp_ancient_one").build());
        tree.addNode(SkillNode.builder("vamp_mist_form")
            .name(P+"vamp_mist_form.name").desc(P+"vamp_mist_form.desc")
            .icon("\u224B").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(4).pos(4,3)
            .requires("vamp_night_hunter").effect("vamp_mist_form").build());
        tree.addNode(SkillNode.builder("vamp_hybrid_predator")
            .name(P+"vamp_hybrid_predator.name").desc(P+"vamp_hybrid_predator.desc")
            .icon("\u25C8").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(1,3)
            .requires("vamp_shadow_cloak","vamp_rapid_regen").effect("vamp_hybrid_predator").build());
        tree.addNode(SkillNode.builder("vamp_specialist_assassin")
            .name(P+"vamp_specialist_assassin.name").desc(P+"vamp_specialist_assassin.desc")
            .icon("\u2620").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(3,3)
            .requires("vamp_shadow_stalker","vamp_night_hunter")
            .excludes("vamp_ancient_one","vamp_hybrid_predator")
            .effect("vamp_specialist_assassin").build());
        tree.addNode(SkillNode.builder("vamp_synergy_bloodshadow")
            .name(P+"vamp_synergy_bloodshadow.name").desc(P+"vamp_synergy_bloodshadow.desc")
            .icon("\u25CE").tier(SkillTier.TIER_4).branch(SkillBranch.SYNERGY_AD).cost(4).pos(-1,4)
            .requires("vamp_blood_drain_skill","vamp_shadow_cloak")
            .effect("vamp_synergy_bloodshadow").build());

        // TIER 5
        tree.addNode(SkillNode.builder("vamp_nosferatu")
            .name(P+"vamp_nosferatu.name").desc(P+"vamp_nosferatu.desc")
            .icon("\u262C").tier(SkillTier.TIER_5).branch(SkillBranch.CORE).cost(5).pos(1,5)
            .requires("vamp_blood_lord","vamp_ancient_one").effect("vamp_nosferatu").build());
        tree.addNode(SkillNode.builder("vamp_forbidden_bloodpact")
            .name(P+"vamp_forbidden_bloodpact.name").desc(P+"vamp_forbidden_bloodpact.desc")
            .icon("\u2620").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(-2,5)
            .requires("vamp_blood_lord","vamp_crimson_tide").excludes("vamp_ascended_daywalker")
            .effect("vamp_forbidden_bloodpact").build());
        tree.addNode(SkillNode.builder("vamp_ascended_daywalker")
            .name(P+"vamp_ascended_daywalker.name").desc(P+"vamp_ascended_daywalker.desc")
            .icon("\u2600").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(3,5)
            .requires("vamp_ancient_one","vamp_mist_form").excludes("vamp_forbidden_bloodpact")
            .effect("vamp_ascended_daywalker").build());
        tree.addNode(SkillNode.builder("vamp_ascended_progenitor")
            .name(P+"vamp_ascended_progenitor.name").desc(P+"vamp_ascended_progenitor.desc")
            .icon("\u269C").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(4,5)
            .requires("vamp_ascended_daywalker").effect("vamp_ascended_progenitor").build());

        SkillTreeRegistry.register(tree);
    }
}
