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
 * Celestial Choir — left: Judgement (smiting), middle: Mercy (healing, wards),
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


        // Apex (tier VI) — the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("seraph_apex_choir").lang("abyssorigins.skill.seraph_apex_choir")
            .icon("♫").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(-1, 5)
            .requiresAny("seraph_ascension", "seraph_fallen_grace").stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("seraph_apex_radiant_halo").lang("abyssorigins.skill.seraph_apex_radiant_halo")
            .icon("☼").tier(SkillTier.TIER_6).branch(SkillBranch.SPECIALIST).cost(4).pos(1, 5)
            .requiresAny("seraph_ascension", "seraph_fallen_grace").stat(Attributes.ARMOR_TOUGHNESS, 2).statPct(Attributes.MOVEMENT_SPEED, 0.04).build());

        SkillTreeRegistry.register(tree);
    }
}
