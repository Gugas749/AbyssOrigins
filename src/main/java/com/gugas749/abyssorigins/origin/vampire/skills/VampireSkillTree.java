package com.gugas749.abyssorigins.origin.vampire.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;

/**
 * Blood Arts. Node ids are unchanged from earlier versions so existing saves keep their unlocks; behaviour
 * lives in VampireHooks and the vampire abilities.
 *
 * The tree was trimmed to the size of the other origins' trees: the nodes in {@link #RETIRED} were folded
 * into related ones (Swiftness into Agility, Heightened Senses into Charm, Blood Drain into Blood Frenzy,
 * Shadow Cloak into Shadow Stalker, Bat Mastery into Night Hunter, Crimson Tide into Blood Lord, Perfect
 * Assassin into Shadow Master, Progenitor into Day-walker; the Hybrid and Synergy nodes were dropped).
 * Points spent on them are refunded on login (SkillService.refundRetired).
 */
public class VampireSkillTree {

    private static final String P = "abyssorigins.skill.";

    /** Removed node ids and what they cost, for refunds. */
    public static final Map<String, Integer> RETIRED = Map.of(
        "vamp_swiftness", 2, "vamp_senses", 2, "vamp_blood_drain_skill", 3, "vamp_shadow_cloak", 3,
        "vamp_bat_mastery", 3, "vamp_crimson_tide", 4, "vamp_hybrid_predator", 4, "vamp_specialist_assassin", 4,
        "vamp_synergy_bloodshadow", 4, "vamp_ascended_progenitor", 5);

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "abyssorigins.skilltree.vampire.name"
        ).retire(RETIRED);

        // TIER 1
        tree.addNode(SkillNode.builder("vamp_vitality").lang(P + "vamp_vitality")
            .icon("♥").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(-2, 0)
            .stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("vamp_agility").lang(P + "vamp_agility")
            .icon("»").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(0, 0)
            .statPct(Attributes.MOVEMENT_SPEED, 0.08).build());
        tree.addNode(SkillNode.builder("vamp_hunger").lang(P + "vamp_hunger")
            .icon("☾").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(2, 0).build());

        // TIER 2
        tree.addNode(SkillNode.builder("vamp_fangs").lang(P + "vamp_fangs")
            .icon("⚔").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-2, 1)
            .requires("vamp_vitality").statPct(Attributes.ATTACK_DAMAGE, 0.10).build());
        tree.addNode(SkillNode.builder("vamp_resilience").lang(P + "vamp_resilience")
            .icon("⛨").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(-1, 1)
            .requires("vamp_vitality").stat(Attributes.ARMOR, 2).stat(Attributes.MAX_HEALTH, 2).build());
        tree.addNode(SkillNode.builder("vamp_charm").lang(P + "vamp_charm")
            .icon("◉").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(2, 1)
            .requires("vamp_hunger").build());

        // TIER 3 - Blood or Shadow (a choice), Immortal Flesh or Rapid Regeneration (a choice), Night Hunter
        tree.addNode(SkillNode.builder("vamp_blood_frenzy").lang(P + "vamp_blood_frenzy")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-3, 2)
            .requires("vamp_fangs").excludes("vamp_shadow_stalker")
            .statPct(Attributes.ATTACK_DAMAGE, 0.25).stat(Attributes.ARMOR, -2).build());
        tree.addNode(SkillNode.builder("vamp_shadow_stalker").lang(P + "vamp_shadow_stalker")
            .icon("◐").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(-2, 2)
            .requires("vamp_fangs").excludes("vamp_blood_frenzy")
            .statPct(Attributes.ATTACK_DAMAGE, 0.15).statPct(Attributes.MOVEMENT_SPEED, 0.05).build());
        tree.addNode(SkillNode.builder("vamp_immortal_flesh").lang(P + "vamp_immortal_flesh")
            .icon("♥").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(-1, 2)
            .requires("vamp_resilience").excludes("vamp_rapid_regen")
            .stat(Attributes.MAX_HEALTH, 10).stat(Attributes.KNOCKBACK_RESISTANCE, 0.2).build());
        tree.addNode(SkillNode.builder("vamp_rapid_regen").lang(P + "vamp_rapid_regen")
            .icon("✚").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(0, 2)
            .requires("vamp_resilience").excludes("vamp_immortal_flesh").build());
        tree.addNode(SkillNode.builder("vamp_night_hunter").lang(P + "vamp_night_hunter")
            .icon("☾").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(2, 2)
            .requiresAny("vamp_agility", "vamp_charm").build());

        // TIER 4
        tree.addNode(SkillNode.builder("vamp_blood_lord").lang(P + "vamp_blood_lord")
            .icon("♛").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-3, 3)
            .requires("vamp_blood_frenzy")
            .statPct(Attributes.ATTACK_DAMAGE, 0.20).statPct(Attributes.ATTACK_SPEED, 0.15).build());
        tree.addNode(SkillNode.builder("vamp_shadow_master").lang(P + "vamp_shadow_master")
            .icon("◒").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-2, 3)
            .requires("vamp_shadow_stalker").statPct(Attributes.ATTACK_DAMAGE, 0.18).build());
        tree.addNode(SkillNode.builder("vamp_ancient_one").lang(P + "vamp_ancient_one")
            .icon("⚜").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 3)
            .requiresAny("vamp_immortal_flesh", "vamp_rapid_regen")
            .stat(Attributes.MAX_HEALTH, 8).stat(Attributes.ARMOR, 4).stat(Attributes.ARMOR_TOUGHNESS, 2).build());
        tree.addNode(SkillNode.builder("vamp_mist_form").lang(P + "vamp_mist_form")
            .icon("☁").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(4).pos(2, 3)
            .requires("vamp_night_hunter").statPct(Attributes.MOVEMENT_SPEED, 0.10).build());
        // Taught power: Life Steal Slash
        tree.addNode(SkillNode.builder("vampire_crimson_slash").lang(P + "vampire_crimson_slash")
            .icon("☽").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(3).pos(0, 4)
            .requiresAny("vamp_blood_lord", "vamp_shadow_master", "vamp_ancient_one", "vamp_mist_form")
            .grants("vampire_crimson_slash").build());

        // TIER 5 - Forbidden Bloodpact or Day-walker (a choice), Nosferatu
        tree.addNode(SkillNode.builder("vamp_forbidden_bloodpact").lang(P + "vamp_forbidden_bloodpact")
            .icon("☠").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(-2, 5)
            .requiresAny("vamp_blood_lord", "vamp_shadow_master").excludes("vamp_ascended_daywalker")
            .statPct(Attributes.ATTACK_DAMAGE, 0.35).build());
        tree.addNode(SkillNode.builder("vamp_nosferatu").lang(P + "vamp_nosferatu")
            .icon("☬").tier(SkillTier.TIER_5).branch(SkillBranch.CORE).cost(5).pos(0, 5)
            .requiresAny("vamp_blood_lord", "vamp_shadow_master", "vamp_ancient_one", "vamp_mist_form")
            .statPct(Attributes.ATTACK_DAMAGE, 0.15).stat(Attributes.MAX_HEALTH, 6)
            .statPct(Attributes.MOVEMENT_SPEED, 0.10).build());
        tree.addNode(SkillNode.builder("vamp_ascended_daywalker").lang(P + "vamp_ascended_daywalker")
            .icon("☀").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(2, 5)
            .requiresAny("vamp_ancient_one", "vamp_mist_form").excludes("vamp_forbidden_bloodpact")
            .stat(Attributes.MAX_HEALTH, 4).build());

        // Apex (tier VI) - the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("vamp_apex_elder_blood").lang(P + "vamp_apex_elder_blood")
            .icon("❦").tier(SkillTier.TIER_6).branch(SkillBranch.FORBIDDEN).cost(4).pos(-1, 6)
            .requiresAny("vamp_forbidden_bloodpact", "vamp_nosferatu", "vamp_ascended_daywalker")
            .stat(Attributes.MAX_HEALTH, 4).stat(Attributes.ATTACK_DAMAGE, 1).build());
        tree.addNode(SkillNode.builder("vamp_apex_night_sovereign").lang(P + "vamp_apex_night_sovereign")
            .icon("☾").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(1, 6)
            .requiresAny("vamp_forbidden_bloodpact", "vamp_nosferatu", "vamp_ascended_daywalker")
            .statPct(Attributes.ATTACK_SPEED, 0.08).statPct(Attributes.MOVEMENT_SPEED, 0.04).build());

        // Ascension rows: tier VII opens at the Noble stage (level 25), tier VIII at the Lord stage (level 50)
        tree.addNode(SkillNode.builder("vamp_noble_bloodline").lang(P + "vamp_noble_bloodline")
            .icon("❦").tier(SkillTier.TIER_7).branch(SkillBranch.OFFENSE).cost(4).pos(-1, 8)
            .requiresAny("vamp_apex_elder_blood", "vamp_apex_night_sovereign").stat(Attributes.MAX_HEALTH, 6).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());
        tree.addNode(SkillNode.builder("vamp_noble_court").lang(P + "vamp_noble_court")
            .icon("◉").tier(SkillTier.TIER_7).branch(SkillBranch.CORE).cost(4).pos(0, 8)
            .requiresAny("vamp_apex_elder_blood", "vamp_apex_night_sovereign").cooldown(0.08).stat(Attributes.ARMOR, 2).build());
        tree.addNode(SkillNode.builder("vamp_noble_grace").lang(P + "vamp_noble_grace")
            .icon("☽").tier(SkillTier.TIER_7).branch(SkillBranch.UTILITY).cost(4).pos(1, 8)
            .requiresAny("vamp_apex_elder_blood", "vamp_apex_night_sovereign").statPct(Attributes.MOVEMENT_SPEED, 0.1).statPct(Attributes.ATTACK_SPEED, 0.1).build());
        tree.addNode(SkillNode.builder("vamp_lord_undying").lang(P + "vamp_lord_undying")
            .icon("✚").tier(SkillTier.TIER_8).branch(SkillBranch.DEFENSE).cost(5).pos(-1, 10)
            .requiresAny("vamp_noble_bloodline", "vamp_noble_court").stat(Attributes.MAX_HEALTH, 10).stat(Attributes.ARMOR_TOUGHNESS, 3).build());
        tree.addNode(SkillNode.builder("vamp_lord_crimson_reign").lang(P + "vamp_lord_crimson_reign")
            .icon("♦").tier(SkillTier.TIER_8).branch(SkillBranch.OFFENSE).cost(5).pos(1, 10)
            .requiresAny("vamp_noble_court", "vamp_noble_grace").statPct(Attributes.ATTACK_DAMAGE, 0.2).build());
        tree.addNode(SkillNode.builder("vamp_lord_sovereign").lang(P + "vamp_lord_sovereign")
            .icon("☬").tier(SkillTier.TIER_8).branch(SkillBranch.ASCENDED).cost(6).pos(0, 11)
            .requiresAny("vamp_lord_undying", "vamp_lord_crimson_reign").cooldown(0.12).stat(Attributes.MAX_HEALTH, 4).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());

        SkillTreeRegistry.register(tree);
    }
}
