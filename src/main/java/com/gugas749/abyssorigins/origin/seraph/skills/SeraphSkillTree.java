package com.gugas749.abyssorigins.origin.seraph.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Celestial Choir - left: Judgement (smiting), middle: Mercy (healing, wards),
 * right: Sky (flight). Capstone choice: Archangel ascension, or Fall from Grace.
 */
public class SeraphSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "seraph"),
            "abyssorigins.skilltree.seraph.name");

        tree.addNode(SkillNode.builder("seraph_halo").lang(P + "seraph_halo")
            .icon("○").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(-1, 0)
            .stat(Attributes.MAX_HEALTH, 2).build());
        tree.addNode(SkillNode.builder("seraph_wingspan").lang(P + "seraph_wingspan")
            .icon("✧").tier(SkillTier.TIER_1).branch(SkillBranch.UTILITY).cost(1).pos(1, 0).build());

        tree.addNode(SkillNode.builder("seraph_smite").lang(P + "seraph_smite")
            .icon("⚔").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-1, 1)
            .requires("seraph_halo").build());
        tree.addNode(SkillNode.builder("seraph_mending_light").lang(P + "seraph_mending_light")
            .icon("✚").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(0, 1)
            .requiresAny("seraph_halo", "seraph_wingspan").build());
        tree.addNode(SkillNode.builder("seraph_skyward").lang(P + "seraph_skyward")
            .icon("☁").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(1, 1)
            .requires("seraph_wingspan").build());

        tree.addNode(SkillNode.builder("seraph_judgement").lang(P + "seraph_judgement")
            .icon("⚖").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-1, 2)
            .requires("seraph_smite").build());
        tree.addNode(SkillNode.builder("seraph_aegis").lang(P + "seraph_aegis")
            .icon("⛨").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(0, 2)
            .requires("seraph_mending_light").stat(Attributes.ARMOR, 2).build());
        tree.addNode(SkillNode.builder("seraph_zephyr").lang(P + "seraph_zephyr")
            .icon("≋").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(1, 2)
            .requires("seraph_skyward").build());

        tree.addNode(SkillNode.builder("seraph_wrath_of_heaven").lang(P + "seraph_wrath_of_heaven")
            .icon("⚡").tier(SkillTier.TIER_4).branch(SkillBranch.ELEMENTAL_A).cost(4).pos(-1, 3)
            .requires("seraph_judgement").build());
        tree.addNode(SkillNode.builder("seraph_beacon").lang(P + "seraph_beacon")
            .icon("☸").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(0, 3)
            .requires("seraph_aegis").build());

        tree.addNode(SkillNode.builder("seraph_ascension").lang(P + "seraph_ascension")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(-1, 4)
            .requiresAny("seraph_wrath_of_heaven", "seraph_beacon", "seraph_zephyr")
            .excludes("seraph_fallen_grace").stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("seraph_fallen_grace").lang(P + "seraph_fallen_grace")
            .icon("✡").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(1, 4)
            .requiresAny("seraph_wrath_of_heaven", "seraph_beacon", "seraph_zephyr")
            .excludes("seraph_ascension").statPct(Attributes.ATTACK_DAMAGE, 0.30).build());


        // Apex (tier VI) - the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("seraph_apex_choir").lang("abyssorigins.skill.seraph_apex_choir")
            .icon("♫").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(-1, 5)
            .requiresAny("seraph_ascension", "seraph_fallen_grace").stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("seraph_apex_radiant_halo").lang("abyssorigins.skill.seraph_apex_radiant_halo")
            .icon("☼").tier(SkillTier.TIER_6).branch(SkillBranch.SPECIALIST).cost(4).pos(1, 5)
            .requiresAny("seraph_ascension", "seraph_fallen_grace").stat(Attributes.ARMOR_TOUGHNESS, 2).statPct(Attributes.MOVEMENT_SPEED, 0.04).build());

        // Taught power (tier IV, right): Judgement - the falling blade
        tree.addNode(SkillNode.builder("seraph_judgement_blade").lang("abyssorigins.skill.seraph_judgement_blade")
            .icon("✝").tier(SkillTier.TIER_4).branch(SkillBranch.ASCENDED).cost(3).pos(1, 3)
            .requiresAny("seraph_judgement", "seraph_aegis", "seraph_zephyr")
            .grants("seraph_judgement_blade").build());

        // Side branches: an offensive chain on the left (tiers II-IV), a defensive one on the right (III-IV)
        tree.addNode(SkillNode.builder("seraph_sacred_edge").lang(P + "seraph_sacred_edge")
            .icon("✦").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-2, 1)
            .requires("seraph_halo").build());
        tree.addNode(SkillNode.builder("seraph_blinding_light").lang(P + "seraph_blinding_light")
            .icon("☀").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-2, 2)
            .requires("seraph_sacred_edge").build());
        tree.addNode(SkillNode.builder("seraph_fury").lang(P + "seraph_fury")
            .icon("⚔").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(-2, 3)
            .requires("seraph_blinding_light").build());
        tree.addNode(SkillNode.builder("seraph_guardian_wings").lang(P + "seraph_guardian_wings")
            .icon("✧").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(2, 2)
            .requires("seraph_skyward").build());
        tree.addNode(SkillNode.builder("seraph_light_ward").lang(P + "seraph_light_ward")
            .icon("⛨").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(2, 3)
            .requires("seraph_guardian_wings").build());

        // Ascension rows: tier VII opens at the Noble stage (level 25), tier VIII at the Lord stage (level 50)
        tree.addNode(SkillNode.builder("seraph_noble_lightwing").lang(P + "seraph_noble_lightwing")
            .icon("✧").tier(SkillTier.TIER_7).branch(SkillBranch.UTILITY).cost(4).pos(-1, 7)
            .requiresAny("seraph_apex_choir", "seraph_apex_radiant_halo").statPct(Attributes.MOVEMENT_SPEED, 0.1).statPct(Attributes.GRAVITY, -0.1).build());
        tree.addNode(SkillNode.builder("seraph_noble_hymn").lang(P + "seraph_noble_hymn")
            .icon("♫").tier(SkillTier.TIER_7).branch(SkillBranch.CORE).cost(4).pos(0, 7)
            .requiresAny("seraph_apex_choir", "seraph_apex_radiant_halo").cooldown(0.08).statPct(Attributes.ATTACK_DAMAGE, 0.05).build());
        tree.addNode(SkillNode.builder("seraph_noble_sanctity").lang(P + "seraph_noble_sanctity")
            .icon("✚").tier(SkillTier.TIER_7).branch(SkillBranch.DEFENSE).cost(4).pos(1, 7)
            .requiresAny("seraph_apex_choir", "seraph_apex_radiant_halo").stat(Attributes.MAX_HEALTH, 6).stat(Attributes.ARMOR, 2).build());
        tree.addNode(SkillNode.builder("seraph_lord_aegis").lang(P + "seraph_lord_aegis")
            .icon("⛨").tier(SkillTier.TIER_8).branch(SkillBranch.DEFENSE).cost(5).pos(-1, 9)
            .requiresAny("seraph_noble_lightwing", "seraph_noble_hymn").stat(Attributes.MAX_HEALTH, 8).stat(Attributes.ARMOR_TOUGHNESS, 3).build());
        tree.addNode(SkillNode.builder("seraph_lord_judgement").lang(P + "seraph_lord_judgement")
            .icon("⚔").tier(SkillTier.TIER_8).branch(SkillBranch.OFFENSE).cost(5).pos(1, 9)
            .requiresAny("seraph_noble_hymn", "seraph_noble_sanctity").statPct(Attributes.ATTACK_DAMAGE, 0.2).build());
        tree.addNode(SkillNode.builder("seraph_lord_throne").lang(P + "seraph_lord_throne")
            .icon("☀").tier(SkillTier.TIER_8).branch(SkillBranch.ASCENDED).cost(6).pos(0, 10)
            .requiresAny("seraph_lord_aegis", "seraph_lord_judgement").cooldown(0.12).stat(Attributes.MAX_HEALTH, 4).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());

        SkillTreeRegistry.register(tree);
    }
}
