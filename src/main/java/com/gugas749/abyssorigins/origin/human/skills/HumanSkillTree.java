package com.gugas749.abyssorigins.origin.human.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Human Mastery - three paths from one root:
 *   left  (Iron)    : toughness -> combat -> speed when hurt
 *   right (Hearth)  : sustain   -> endurance -> last stand
 *   middle (Mind)   : class mastery (Tactician, Rallying Cry)
 * Capstones are mutually exclusive: Unbroken (cheat death) vs Paragon (raw resilience).
 */
public class HumanSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "human"),
            "abyssorigins.skilltree.human.name"
        );

        tree.addNode(SkillNode.builder("human_tough_skin").lang(P + "human_tough_skin")
            .icon("❈").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(0, 0)
            .stat(Attributes.ARMOR, 2).build());

        tree.addNode(SkillNode.builder("human_iron_will").lang(P + "human_iron_will")
            .icon("♥").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(-1, 1)
            .requires("human_tough_skin").stat(Attributes.MAX_HEALTH, 4).build());

        tree.addNode(SkillNode.builder("human_quick_learner").lang(P + "human_quick_learner")
            .icon("✦").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(1).pos(1, 1)
            .requires("human_tough_skin").build());

        tree.addNode(SkillNode.builder("human_fighter").lang(P + "human_fighter")
            .icon("⚔").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(2).pos(-1, 2)
            .requires("human_iron_will").stat(Attributes.ATTACK_DAMAGE, 1).build());

        tree.addNode(SkillNode.builder("human_tactician").lang(P + "human_tactician")
            .icon("☸").tier(SkillTier.TIER_3).branch(SkillBranch.SPECIALIST).cost(2).pos(0, 2)
            .requiresAny("human_iron_will", "human_quick_learner").build());

        tree.addNode(SkillNode.builder("human_endurance").lang(P + "human_endurance")
            .icon("❤").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(1, 2)
            .requires("human_quick_learner").stat(Attributes.MAX_HEALTH, 6).build());

        tree.addNode(SkillNode.builder("human_swift").lang(P + "human_swift")
            .icon("»").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(2).pos(-1, 3)
            .requires("human_fighter").statPct(Attributes.MOVEMENT_SPEED, 0.10).build());

        tree.addNode(SkillNode.builder("human_rallying_cry").lang(P + "human_rallying_cry")
            .icon("⚑").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(3).pos(0, 3)
            .requires("human_tactician").build());

        tree.addNode(SkillNode.builder("human_last_stand").lang(P + "human_last_stand")
            .icon("★").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(3).pos(1, 3)
            .requires("human_endurance").build());

        tree.addNode(SkillNode.builder("human_unbroken").lang(P + "human_unbroken")
            .icon("✠").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(-1, 4)
            .requiresAny("human_swift", "human_rallying_cry", "human_last_stand")
            .excludes("human_paragon").build());

        tree.addNode(SkillNode.builder("human_paragon").lang(P + "human_paragon")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(1, 4)
            .requiresAny("human_swift", "human_rallying_cry", "human_last_stand")
            .excludes("human_unbroken")
            .stat(Attributes.MAX_HEALTH, 4).stat(Attributes.ARMOR_TOUGHNESS, 2).build());


        // Apex (tier VI) - the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("human_apex_legacy").lang("abyssorigins.skill.human_apex_legacy")
            .icon("♔").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(-1, 5)
            .requiresAny("human_unbroken", "human_paragon").stat(Attributes.MAX_HEALTH, 4).stat(Attributes.ARMOR_TOUGHNESS, 1).build());
        tree.addNode(SkillNode.builder("human_apex_boundless").lang("abyssorigins.skill.human_apex_boundless")
            .icon("✧").tier(SkillTier.TIER_6).branch(SkillBranch.SPECIALIST).cost(4).pos(1, 5)
            .requiresAny("human_unbroken", "human_paragon").statPct(Attributes.MOVEMENT_SPEED, 0.05).stat(Attributes.ATTACK_DAMAGE, 1).build());

        // Taught power (tier IV, centre): Perfect Adaptability
        tree.addNode(SkillNode.builder("human_perfect_adaptation").lang(P + "human_perfect_adaptation")
            .icon("❂").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(3).pos(0, 4)
            .requiresAny("human_swift", "human_rallying_cry", "human_last_stand")
            .grants("human_perfect_adaptation").build());

        // Side branches: an offensive chain on the left (tiers II-IV), a defensive one on the right (III-IV)
        tree.addNode(SkillNode.builder("human_keen_edge").lang(P + "human_keen_edge")
            .icon("⚔").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-2, 1)
            .requires("human_tough_skin").build());
        tree.addNode(SkillNode.builder("human_veteran").lang(P + "human_veteran")
            .icon("✪").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-2, 2)
            .requires("human_keen_edge").build());
        tree.addNode(SkillNode.builder("human_warlord").lang(P + "human_warlord")
            .icon("⚑").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(-2, 3)
            .requires("human_veteran").build());
        tree.addNode(SkillNode.builder("human_steady_breath").lang(P + "human_steady_breath")
            .icon("✚").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(2, 2)
            .requires("human_quick_learner").build());
        tree.addNode(SkillNode.builder("human_unyielding").lang(P + "human_unyielding")
            .icon("⛨").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(2, 3)
            .requires("human_steady_breath").stat(Attributes.KNOCKBACK_RESISTANCE, 0.2).build());

        // Ascension rows: tier VII opens at the Noble stage (level 25), tier VIII at the Lord stage (level 50)
        tree.addNode(SkillNode.builder("human_noble_steel").lang(P + "human_noble_steel")
            .icon("⛨").tier(SkillTier.TIER_7).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 7)
            .requiresAny("human_apex_legacy", "human_apex_boundless").stat(Attributes.ARMOR, 3).stat(Attributes.ARMOR_TOUGHNESS, 2).build());
        tree.addNode(SkillNode.builder("human_noble_resolve").lang(P + "human_noble_resolve")
            .icon("✦").tier(SkillTier.TIER_7).branch(SkillBranch.CORE).cost(4).pos(0, 7)
            .requiresAny("human_apex_legacy", "human_apex_boundless").cooldown(0.08).stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("human_noble_vigor").lang(P + "human_noble_vigor")
            .icon("»").tier(SkillTier.TIER_7).branch(SkillBranch.UTILITY).cost(4).pos(1, 7)
            .requiresAny("human_apex_legacy", "human_apex_boundless").statPct(Attributes.MOVEMENT_SPEED, 0.08).statPct(Attributes.ATTACK_SPEED, 0.1).build());
        tree.addNode(SkillNode.builder("human_lord_bulwark").lang(P + "human_lord_bulwark")
            .icon("⛨").tier(SkillTier.TIER_8).branch(SkillBranch.DEFENSE).cost(5).pos(-1, 9)
            .requiresAny("human_noble_steel", "human_noble_resolve").stat(Attributes.MAX_HEALTH, 8).stat(Attributes.KNOCKBACK_RESISTANCE, 0.3).build());
        tree.addNode(SkillNode.builder("human_lord_conqueror").lang(P + "human_lord_conqueror")
            .icon("⚔").tier(SkillTier.TIER_8).branch(SkillBranch.OFFENSE).cost(5).pos(1, 9)
            .requiresAny("human_noble_resolve", "human_noble_vigor").statPct(Attributes.ATTACK_DAMAGE, 0.15).statPct(Attributes.ATTACK_SPEED, 0.05).build());
        tree.addNode(SkillNode.builder("human_lord_paragon").lang(P + "human_lord_paragon")
            .icon("♛").tier(SkillTier.TIER_8).branch(SkillBranch.ASCENDED).cost(6).pos(0, 10)
            .requiresAny("human_lord_bulwark", "human_lord_conqueror").cooldown(0.12).stat(Attributes.MAX_HEALTH, 4).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());

        SkillTreeRegistry.register(tree);
    }
}
