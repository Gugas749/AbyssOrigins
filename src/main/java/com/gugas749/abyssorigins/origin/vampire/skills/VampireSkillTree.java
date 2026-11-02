package com.gugas749.abyssorigins.origin.vampire.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Blood Arts. Node ids are unchanged from earlier versions so existing saves keep
 * their unlocks; stats moved from SkillEffectHandler's switch onto the nodes, and
 * behaviour lives in VampireHooks.
 */
public class VampireSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "abyssorigins.skilltree.vampire.name"
        );

        // TIER 1
        tree.addNode(SkillNode.builder("vamp_vitality").lang(P+"vamp_vitality")
            .icon("♥").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(0,0)
            .stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("vamp_agility").lang(P+"vamp_agility")
            .icon("»").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(2,0)
            .statPct(Attributes.MOVEMENT_SPEED, 0.05).build());
        tree.addNode(SkillNode.builder("vamp_hunger").lang(P+"vamp_hunger")
            .icon("☾").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(4,0).build());

        // TIER 2
        tree.addNode(SkillNode.builder("vamp_fangs").lang(P+"vamp_fangs")
            .icon("⚔").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-1,1)
            .requires("vamp_vitality").statPct(Attributes.ATTACK_DAMAGE, 0.10).build());
        tree.addNode(SkillNode.builder("vamp_resilience").lang(P+"vamp_resilience")
            .icon("⛨").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(1,1)
            .requires("vamp_vitality").stat(Attributes.ARMOR, 2).stat(Attributes.MAX_HEALTH, 2).build());
        tree.addNode(SkillNode.builder("vamp_swiftness").lang(P+"vamp_swiftness")
            .icon("☽").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(3,1)
            .requires("vamp_agility").statPct(Attributes.MOVEMENT_SPEED, 0.08).build());
        tree.addNode(SkillNode.builder("vamp_charm").lang(P+"vamp_charm")
            .icon("◉").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(4,1)
            .requires("vamp_hunger").build());
        tree.addNode(SkillNode.builder("vamp_senses").lang(P+"vamp_senses")
            .icon("☉").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(5,1)
            .requires("vamp_hunger").build());

        // TIER 3 — Blood Path
        tree.addNode(SkillNode.builder("vamp_blood_frenzy").lang(P+"vamp_blood_frenzy")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-2,2)
            .requires("vamp_fangs").excludes("vamp_shadow_stalker","vamp_shadow_cloak")
            .statPct(Attributes.ATTACK_DAMAGE, 0.25).stat(Attributes.ARMOR, -2).build());
        tree.addNode(SkillNode.builder("vamp_blood_drain_skill").lang(P+"vamp_blood_drain_skill")
            .icon("♦").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-1,2)
            .requires("vamp_fangs").statPct(Attributes.ATTACK_DAMAGE, 0.08).build());

        // TIER 3 — Shadow Path
        tree.addNode(SkillNode.builder("vamp_shadow_stalker").lang(P+"vamp_shadow_stalker")
            .icon("◐").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(0,2)
            .requires("vamp_fangs").excludes("vamp_blood_frenzy","vamp_blood_drain_skill")
            .statPct(Attributes.ATTACK_DAMAGE, 0.15).build());
        tree.addNode(SkillNode.builder("vamp_shadow_cloak").lang(P+"vamp_shadow_cloak")
            .icon("◑").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(1,2)
            .requires("vamp_resilience").excludes("vamp_blood_frenzy")
            .statPct(Attributes.MOVEMENT_SPEED, 0.05).build());

        // TIER 3 — Defense
        tree.addNode(SkillNode.builder("vamp_immortal_flesh").lang(P+"vamp_immortal_flesh")
            .icon("♥").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(2,2)
            .requires("vamp_resilience").excludes("vamp_rapid_regen")
            .stat(Attributes.MAX_HEALTH, 10).stat(Attributes.KNOCKBACK_RESISTANCE, 0.2).build());
        tree.addNode(SkillNode.builder("vamp_rapid_regen").lang(P+"vamp_rapid_regen")
            .icon("✚").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(3,2)
            .requires("vamp_resilience").excludes("vamp_immortal_flesh").build());

        // TIER 3 — Utility
        tree.addNode(SkillNode.builder("vamp_night_hunter").lang(P+"vamp_night_hunter")
            .icon("☾").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(4,2)
            .requires("vamp_swiftness").build());
        tree.addNode(SkillNode.builder("vamp_bat_mastery").lang(P+"vamp_bat_mastery")
            .icon("❧").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(5,2)
            .requires("vamp_senses").build());

        // TIER 4
        tree.addNode(SkillNode.builder("vamp_blood_lord").lang(P+"vamp_blood_lord")
            .icon("♛").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-2,3)
            .requires("vamp_blood_frenzy")
            .statPct(Attributes.ATTACK_DAMAGE, 0.20).statPct(Attributes.ATTACK_SPEED, 0.15).build());
        tree.addNode(SkillNode.builder("vamp_crimson_tide").lang(P+"vamp_crimson_tide")
            .icon("≋").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-1,3)
            .requires("vamp_blood_drain_skill").statPct(Attributes.ATTACK_DAMAGE, 0.12).build());
        tree.addNode(SkillNode.builder("vamp_shadow_master").lang(P+"vamp_shadow_master")
            .icon("◒").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(0,3)
            .requires("vamp_shadow_stalker").statPct(Attributes.ATTACK_DAMAGE, 0.18).build());
        tree.addNode(SkillNode.builder("vamp_hybrid_predator").lang(P+"vamp_hybrid_predator")
            .icon("◈").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(1,3)
            .requires("vamp_shadow_cloak","vamp_rapid_regen")
            .statPct(Attributes.ATTACK_DAMAGE, 0.10).stat(Attributes.MAX_HEALTH, 4)
            .statPct(Attributes.MOVEMENT_SPEED, 0.08).build());
        tree.addNode(SkillNode.builder("vamp_ancient_one").lang(P+"vamp_ancient_one")
            .icon("⚜").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(2,3)
            .requires("vamp_immortal_flesh")
            .stat(Attributes.MAX_HEALTH, 8).stat(Attributes.ARMOR, 4).stat(Attributes.ARMOR_TOUGHNESS, 2).build());
        tree.addNode(SkillNode.builder("vamp_specialist_assassin").lang(P+"vamp_specialist_assassin")
            .icon("†").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(3,3)
            .requires("vamp_shadow_stalker","vamp_night_hunter")
            .excludes("vamp_ancient_one","vamp_hybrid_predator")
            .statPct(Attributes.ATTACK_DAMAGE, 0.40).stat(Attributes.ARMOR, -4).stat(Attributes.MAX_HEALTH, -4).build());
        tree.addNode(SkillNode.builder("vamp_mist_form").lang(P+"vamp_mist_form")
            .icon("☁").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(4).pos(4,3)
            .requires("vamp_night_hunter").statPct(Attributes.MOVEMENT_SPEED, 0.10).build());
        tree.addNode(SkillNode.builder("vamp_synergy_bloodshadow").lang(P+"vamp_synergy_bloodshadow")
            .icon("◎").tier(SkillTier.TIER_4).branch(SkillBranch.SYNERGY_AD).cost(4).pos(-1,4)
            .requires("vamp_blood_drain_skill","vamp_shadow_cloak")
            .statPct(Attributes.ATTACK_DAMAGE, 0.10).build());

        // TIER 5
        tree.addNode(SkillNode.builder("vamp_forbidden_bloodpact").lang(P+"vamp_forbidden_bloodpact")
            .icon("☠").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(-2,5)
            .requires("vamp_blood_lord","vamp_crimson_tide").excludes("vamp_ascended_daywalker")
            .statPct(Attributes.ATTACK_DAMAGE, 0.35).build());
        tree.addNode(SkillNode.builder("vamp_nosferatu").lang(P+"vamp_nosferatu")
            .icon("☬").tier(SkillTier.TIER_5).branch(SkillBranch.CORE).cost(5).pos(1,5)
            .requires("vamp_blood_lord","vamp_ancient_one")
            .statPct(Attributes.ATTACK_DAMAGE, 0.15).stat(Attributes.MAX_HEALTH, 6)
            .statPct(Attributes.MOVEMENT_SPEED, 0.10).build());
        tree.addNode(SkillNode.builder("vamp_ascended_daywalker").lang(P+"vamp_ascended_daywalker")
            .icon("☀").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(3,5)
            .requires("vamp_ancient_one","vamp_mist_form").excludes("vamp_forbidden_bloodpact")
            .stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("vamp_ascended_progenitor").lang(P+"vamp_ascended_progenitor")
            .icon("⚜").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(4,6)
            .requires("vamp_ascended_daywalker")
            .statPct(Attributes.ATTACK_DAMAGE, 0.20).stat(Attributes.MAX_HEALTH, 8).build());


        // Apex (tier VI) — the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("vamp_apex_elder_blood").lang("abyssorigins.skill.vamp_apex_elder_blood")
            .icon("❦").tier(SkillTier.TIER_6).branch(SkillBranch.FORBIDDEN).cost(4).pos(0, 7)
            .requiresAny("vamp_forbidden_bloodpact", "vamp_nosferatu", "vamp_ascended_daywalker", "vamp_ascended_progenitor").stat(Attributes.MAX_HEALTH, 4).stat(Attributes.ATTACK_DAMAGE, 1).build());
        tree.addNode(SkillNode.builder("vamp_apex_night_sovereign").lang("abyssorigins.skill.vamp_apex_night_sovereign")
            .icon("☾").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(3, 7)
            .requiresAny("vamp_forbidden_bloodpact", "vamp_nosferatu", "vamp_ascended_daywalker", "vamp_ascended_progenitor").statPct(Attributes.ATTACK_SPEED, 0.08).statPct(Attributes.MOVEMENT_SPEED, 0.04).build());

        SkillTreeRegistry.register(tree);
    }
}
