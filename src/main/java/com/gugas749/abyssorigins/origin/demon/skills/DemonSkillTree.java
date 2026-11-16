package com.gugas749.abyssorigins.origin.demon.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Infernal Pacts - left: Hellfire (Wrath -> damage), right: Brimstone (Wrath -> survival).
 * Capstones: Archfiend (mastery) or the Forbidden Pact (endless wrath, crippling water weakness).
 */
public class DemonSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "demon"),
            "abyssorigins.skilltree.demon.name");

        tree.addNode(SkillNode.builder("demon_kindling").lang(P + "demon_kindling")
            .icon("♨").tier(SkillTier.TIER_1).branch(SkillBranch.OFFENSE).cost(1).pos(-1, 0).build());
        tree.addNode(SkillNode.builder("demon_brimstone_skin").lang(P + "demon_brimstone_skin")
            .icon("⬢").tier(SkillTier.TIER_1).branch(SkillBranch.DEFENSE).cost(1).pos(1, 0)
            .stat(Attributes.ARMOR, 2).build());

        tree.addNode(SkillNode.builder("demon_hellfire_mastery").lang(P + "demon_hellfire_mastery")
            .icon("✹").tier(SkillTier.TIER_2).branch(SkillBranch.ELEMENTAL_A).cost(2).pos(-1, 1)
            .requires("demon_kindling").build());
        tree.addNode(SkillNode.builder("demon_bloodlust").lang(P + "demon_bloodlust")
            .icon("☠").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(0, 1)
            .requiresAny("demon_kindling", "demon_brimstone_skin").build());
        tree.addNode(SkillNode.builder("demon_cinder_dash").lang(P + "demon_cinder_dash")
            .icon("➹").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(1, 1)
            .requires("demon_brimstone_skin").build());

        tree.addNode(SkillNode.builder("demon_wrathful_strikes").lang(P + "demon_wrathful_strikes")
            .icon("⚔").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-1, 2)
            .requires("demon_hellfire_mastery").build());
        tree.addNode(SkillNode.builder("demon_tempered").lang(P + "demon_tempered")
            .icon("⚒").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(2).pos(0, 2)
            .requires("demon_bloodlust").build());
        tree.addNode(SkillNode.builder("demon_infernal_resilience").lang(P + "demon_infernal_resilience")
            .icon("⛨").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(1, 2)
            .requires("demon_cinder_dash").stat(Attributes.MAX_HEALTH, 4).build());

        tree.addNode(SkillNode.builder("demon_soul_brand").lang(P + "demon_soul_brand")
            .icon("☢").tier(SkillTier.TIER_4).branch(SkillBranch.ELEMENTAL_B).cost(4).pos(-1, 3)
            .requires("demon_wrathful_strikes").build());
        tree.addNode(SkillNode.builder("demon_ember_heart").lang(P + "demon_ember_heart")
            .icon("❤").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(1, 3)
            .requiresAny("demon_tempered", "demon_infernal_resilience").build());

        tree.addNode(SkillNode.builder("demon_archfiend").lang(P + "demon_archfiend")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(-1, 4)
            .requiresAny("demon_soul_brand", "demon_ember_heart").excludes("demon_forbidden_pact")
            .stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("demon_forbidden_pact").lang(P + "demon_forbidden_pact")
            .icon("⛧").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(1, 4)
            .requiresAny("demon_soul_brand", "demon_ember_heart").excludes("demon_archfiend")
            .statPct(Attributes.ATTACK_DAMAGE, 0.30).build());


        // Apex (tier VI) - the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("demon_apex_hell_sovereign").lang("abyssorigins.skill.demon_apex_hell_sovereign")
            .icon("♆").tier(SkillTier.TIER_6).branch(SkillBranch.FORBIDDEN).cost(4).pos(-1, 5)
            .requiresAny("demon_archfiend", "demon_forbidden_pact").stat(Attributes.ATTACK_DAMAGE, 2).build());
        tree.addNode(SkillNode.builder("demon_apex_brimstone_heart").lang("abyssorigins.skill.demon_apex_brimstone_heart")
            .icon("✺").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(1, 5)
            .requiresAny("demon_archfiend", "demon_forbidden_pact").stat(Attributes.ARMOR, 3).stat(Attributes.MAX_HEALTH, 2).build());

        // Taught power (tier IV, centre): Rage Mode
        tree.addNode(SkillNode.builder("demon_rage_mode").lang(P + "demon_rage_mode")
            .icon("☄").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(3).pos(0, 3)
            .requiresAny("demon_wrathful_strikes", "demon_tempered", "demon_infernal_resilience")
            .grants("demon_rage_mode").build());

        // Side branches: an offensive chain on the left (tiers II-IV), a defensive one on the right (III-IV)
        tree.addNode(SkillNode.builder("demon_spreading_flames").lang(P + "demon_spreading_flames")
            .icon("♨").tier(SkillTier.TIER_2).branch(SkillBranch.ELEMENTAL_A).cost(2).pos(-2, 1)
            .requires("demon_kindling").build());
        tree.addNode(SkillNode.builder("demon_cinder_feast").lang(P + "demon_cinder_feast")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-2, 2)
            .requires("demon_spreading_flames").build());
        tree.addNode(SkillNode.builder("demon_pyre").lang(P + "demon_pyre")
            .icon("✺").tier(SkillTier.TIER_4).branch(SkillBranch.ELEMENTAL_A).cost(4).pos(-2, 3)
            .requires("demon_cinder_feast").build());
        tree.addNode(SkillNode.builder("demon_molten_blood").lang(P + "demon_molten_blood")
            .icon("❤").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(2, 2)
            .requires("demon_cinder_dash").build());
        tree.addNode(SkillNode.builder("demon_unquenchable").lang(P + "demon_unquenchable")
            .icon("⬢").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(4).pos(2, 3)
            .requires("demon_molten_blood").build());

        // Ascension rows: tier VII opens at the Noble stage (level 25), tier VIII at the Lord stage (level 50)
        tree.addNode(SkillNode.builder("demon_noble_hellforged").lang(P + "demon_noble_hellforged")
            .icon("⬢").tier(SkillTier.TIER_7).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 7)
            .requiresAny("demon_apex_hell_sovereign", "demon_apex_brimstone_heart").stat(Attributes.ARMOR, 4).stat(Attributes.ARMOR_TOUGHNESS, 2).build());
        tree.addNode(SkillNode.builder("demon_noble_infernal_will").lang(P + "demon_noble_infernal_will")
            .icon("✹").tier(SkillTier.TIER_7).branch(SkillBranch.CORE).cost(4).pos(0, 7)
            .requiresAny("demon_apex_hell_sovereign", "demon_apex_brimstone_heart").cooldown(0.08).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());
        tree.addNode(SkillNode.builder("demon_noble_brimstone_hide").lang(P + "demon_noble_brimstone_hide")
            .icon("❤").tier(SkillTier.TIER_7).branch(SkillBranch.UTILITY).cost(4).pos(1, 7)
            .requiresAny("demon_apex_hell_sovereign", "demon_apex_brimstone_heart").stat(Attributes.MAX_HEALTH, 6).stat(Attributes.KNOCKBACK_RESISTANCE, 0.2).build());
        tree.addNode(SkillNode.builder("demon_lord_unbroken_horns").lang(P + "demon_lord_unbroken_horns")
            .icon("⛨").tier(SkillTier.TIER_8).branch(SkillBranch.DEFENSE).cost(5).pos(-1, 9)
            .requiresAny("demon_noble_hellforged", "demon_noble_infernal_will").stat(Attributes.MAX_HEALTH, 8).stat(Attributes.ARMOR, 3).build());
        tree.addNode(SkillNode.builder("demon_lord_cataclysm").lang(P + "demon_lord_cataclysm")
            .icon("☄").tier(SkillTier.TIER_8).branch(SkillBranch.OFFENSE).cost(5).pos(1, 9)
            .requiresAny("demon_noble_infernal_will", "demon_noble_brimstone_hide").statPct(Attributes.ATTACK_DAMAGE, 0.2).build());
        tree.addNode(SkillNode.builder("demon_lord_throne").lang(P + "demon_lord_throne")
            .icon("♆").tier(SkillTier.TIER_8).branch(SkillBranch.ASCENDED).cost(6).pos(0, 10)
            .requiresAny("demon_lord_unbroken_horns", "demon_lord_cataclysm").cooldown(0.12).stat(Attributes.MAX_HEALTH, 4).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());

        SkillTreeRegistry.register(tree);
    }
}
