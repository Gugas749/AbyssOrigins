package com.gugas749.abyssorigins.origin.voidborn.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Abyssal Resonance — left: Rift-walking (blink), middle: Entropy (erosion, gravity),
 * right: Hollow (survival). Capstone: Abyss Walker (mastery) or Heart of the Abyss (power at a cost).
 */
public class VoidbornSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "voidborn"),
            "abyssorigins.skilltree.voidborn.name");

        tree.addNode(SkillNode.builder("void_attunement").lang(P + "void_attunement")
            .icon("◈").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(-1, 0).build());
        tree.addNode(SkillNode.builder("void_phase_skin").lang(P + "void_phase_skin")
            .icon("◌").tier(SkillTier.TIER_1).branch(SkillBranch.DEFENSE).cost(1).pos(1, 0).build());

        tree.addNode(SkillNode.builder("void_blink_mastery").lang(P + "void_blink_mastery")
            .icon("✴").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(-1, 1)
            .requires("void_attunement").build());
        tree.addNode(SkillNode.builder("void_erosion_deep").lang(P + "void_erosion_deep")
            .icon("☢").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(0, 1)
            .requiresAny("void_attunement", "void_phase_skin").build());
        tree.addNode(SkillNode.builder("void_hunger").lang(P + "void_hunger")
            .icon("◓").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(1, 1)
            .requires("void_phase_skin").build());

        tree.addNode(SkillNode.builder("void_unseen_step").lang(P + "void_unseen_step")
            .icon("◍").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_B).cost(3).pos(-1, 2)
            .requires("void_blink_mastery").build());
        tree.addNode(SkillNode.builder("void_singularity").lang(P + "void_singularity")
            .icon("⦿").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(0, 2)
            .requires("void_erosion_deep").build());
        tree.addNode(SkillNode.builder("void_null_ward").lang(P + "void_null_ward")
            .icon("∅").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(1, 2)
            .requires("void_hunger").stat(Attributes.MAX_HEALTH, 4).build());

        tree.addNode(SkillNode.builder("void_echo").lang(P + "void_echo")
            .icon("⁂").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(-1, 3)
            .requires("void_unseen_step").build());
        tree.addNode(SkillNode.builder("void_entropy_field").lang(P + "void_entropy_field")
            .icon("◎").tier(SkillTier.TIER_4).branch(SkillBranch.ELEMENTAL_A).cost(4).pos(1, 3)
            .requiresAny("void_singularity", "void_null_ward").build());

        tree.addNode(SkillNode.builder("void_abyss_walker").lang(P + "void_abyss_walker")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(-1, 4)
            .requiresAny("void_echo", "void_entropy_field").excludes("void_heart_of_abyss")
            .stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("void_heart_of_abyss").lang(P + "void_heart_of_abyss")
            .icon("⬤").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(1, 4)
            .requiresAny("void_echo", "void_entropy_field").excludes("void_abyss_walker").build());


        // Apex (tier VI) — the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("void_apex_event_horizon").lang("abyssorigins.skill.void_apex_event_horizon")
            .icon("◉").tier(SkillTier.TIER_6).branch(SkillBranch.ELEMENTAL_B).cost(4).pos(-1, 5)
            .requiresAny("void_abyss_walker", "void_heart_of_abyss").statPct(Attributes.MOVEMENT_SPEED, 0.08).build());
        tree.addNode(SkillNode.builder("void_apex_null_body").lang("abyssorigins.skill.void_apex_null_body")
            .icon("▣").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(1, 5)
            .requiresAny("void_abyss_walker", "void_heart_of_abyss").stat(Attributes.ARMOR, 3).stat(Attributes.KNOCKBACK_RESISTANCE, 0.15).build());

        SkillTreeRegistry.register(tree);
    }
}
