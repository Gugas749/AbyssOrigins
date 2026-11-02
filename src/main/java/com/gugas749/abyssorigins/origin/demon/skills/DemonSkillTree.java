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
 * Infernal Pacts — left: Hellfire (Wrath → damage), right: Brimstone (Wrath → survival).
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


        // Apex (tier VI) — the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("demon_apex_hell_sovereign").lang("abyssorigins.skill.demon_apex_hell_sovereign")
            .icon("♆").tier(SkillTier.TIER_6).branch(SkillBranch.FORBIDDEN).cost(4).pos(-1, 5)
            .requiresAny("demon_archfiend", "demon_forbidden_pact").stat(Attributes.ATTACK_DAMAGE, 2).build());
        tree.addNode(SkillNode.builder("demon_apex_brimstone_heart").lang("abyssorigins.skill.demon_apex_brimstone_heart")
            .icon("✺").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(1, 5)
            .requiresAny("demon_archfiend", "demon_forbidden_pact").stat(Attributes.ARMOR, 3).stat(Attributes.MAX_HEALTH, 2).build());

        SkillTreeRegistry.register(tree);
    }
}
