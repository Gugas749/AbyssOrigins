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
 * Abyssal Resonance - left: Rift-walking (blink), middle: Entropy (erosion, gravity),
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


        // Apex (tier VI) - the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("void_apex_event_horizon").lang("abyssorigins.skill.void_apex_event_horizon")
            .icon("◉").tier(SkillTier.TIER_6).branch(SkillBranch.ELEMENTAL_B).cost(4).pos(-1, 5)
            .requiresAny("void_abyss_walker", "void_heart_of_abyss").statPct(Attributes.MOVEMENT_SPEED, 0.08).build());
        tree.addNode(SkillNode.builder("void_apex_null_body").lang("abyssorigins.skill.void_apex_null_body")
            .icon("▣").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(1, 5)
            .requiresAny("void_abyss_walker", "void_heart_of_abyss").stat(Attributes.ARMOR, 3).stat(Attributes.KNOCKBACK_RESISTANCE, 0.15).build());

        // Taught power (tier IV, centre): Echo Strikes
        tree.addNode(SkillNode.builder("void_echo_strikes").lang("abyssorigins.skill.void_echo_strikes")
            .icon("❖").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(3).pos(0, 3)
            .requiresAny("void_unseen_step", "void_singularity", "void_null_ward")
            .grants("void_echo_strikes").build());

        // Side branches: an offensive chain on the left (tiers II-IV), a defensive one on the right (III-IV)
        tree.addNode(SkillNode.builder("void_null_edge").lang(P + "void_null_edge")
            .icon("◈").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-2, 1)
            .requires("void_attunement").build());
        tree.addNode(SkillNode.builder("void_siphon").lang(P + "void_siphon")
            .icon("∞").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(-2, 2)
            .requires("void_null_edge").build());
        tree.addNode(SkillNode.builder("void_implosion").lang(P + "void_implosion")
            .icon("✺").tier(SkillTier.TIER_4).branch(SkillBranch.ELEMENTAL_B).cost(4).pos(-2, 3)
            .requires("void_siphon").build());
        tree.addNode(SkillNode.builder("void_recoil").lang(P + "void_recoil")
            .icon("◎").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(2, 2)
            .requires("void_hunger").build());
        tree.addNode(SkillNode.builder("void_starless").lang(P + "void_starless")
            .icon("●").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(2, 3)
            .requires("void_recoil").build());

        // Ascension rows: tier VII opens at the Noble stage (level 25), tier VIII at the Lord stage (level 50)
        tree.addNode(SkillNode.builder("void_noble_unbound").lang(P + "void_noble_unbound")
            .icon("◈").tier(SkillTier.TIER_7).branch(SkillBranch.UTILITY).cost(4).pos(-1, 7)
            .requiresAny("void_apex_event_horizon", "void_apex_null_body").statPct(Attributes.MOVEMENT_SPEED, 0.1).statPct(Attributes.ATTACK_DAMAGE, 0.05).build());
        tree.addNode(SkillNode.builder("void_noble_entropy").lang(P + "void_noble_entropy")
            .icon("∞").tier(SkillTier.TIER_7).branch(SkillBranch.CORE).cost(4).pos(0, 7)
            .requiresAny("void_apex_event_horizon", "void_apex_null_body").cooldown(0.08).statPct(Attributes.ATTACK_SPEED, 0.05).build());
        tree.addNode(SkillNode.builder("void_noble_null_shell").lang(P + "void_noble_null_shell")
            .icon("⬡").tier(SkillTier.TIER_7).branch(SkillBranch.DEFENSE).cost(4).pos(1, 7)
            .requiresAny("void_apex_event_horizon", "void_apex_null_body").stat(Attributes.MAX_HEALTH, 6).stat(Attributes.ARMOR, 2).build());
        tree.addNode(SkillNode.builder("void_lord_event_shell").lang(P + "void_lord_event_shell")
            .icon("◎").tier(SkillTier.TIER_8).branch(SkillBranch.DEFENSE).cost(5).pos(-1, 9)
            .requiresAny("void_noble_unbound", "void_noble_entropy").stat(Attributes.MAX_HEALTH, 8).stat(Attributes.ARMOR_TOUGHNESS, 3).build());
        tree.addNode(SkillNode.builder("void_lord_annihilation").lang(P + "void_lord_annihilation")
            .icon("✺").tier(SkillTier.TIER_8).branch(SkillBranch.OFFENSE).cost(5).pos(1, 9)
            .requiresAny("void_noble_entropy", "void_noble_null_shell").statPct(Attributes.ATTACK_DAMAGE, 0.2).build());
        tree.addNode(SkillNode.builder("void_lord_throne").lang(P + "void_lord_throne")
            .icon("⊗").tier(SkillTier.TIER_8).branch(SkillBranch.ASCENDED).cost(6).pos(0, 10)
            .requiresAny("void_lord_event_shell", "void_lord_annihilation").cooldown(0.12).stat(Attributes.MAX_HEALTH, 4).statPct(Attributes.ATTACK_DAMAGE, 0.1).build());

        SkillTreeRegistry.register(tree);
    }
}
