package com.gugas749.abyssorigins.specialization.occultist.skills;

import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Occultist class skill tree.
 * Built as a static factory — no separate registration needed.
 * The tree is carried inside OccultistSpecialization and surfaced via getSkillTree().
 *
 * Layout:
 *   Tier 1:  Dark Knowledge (core)
 *   Tier 2:  Veil Sight, Shadow Rites (branch — perception vs ritual)
 *   Tier 3:  Soul Drain, Hex Mark / Blood Ritual, Phantom Step
 *   Tier 4:  Dark Covenant, Umbral Form
 *   Tier 5:  Ascended Occultist — reachable from EITHER tier-4 node. The old tree
 *            required both, which Soul Drain / Blood Ritual's exclusion made impossible.
 */
public class OccultistSkillTree {

    private static final String P = "abyssorigins.skill.occultist.";

    public static SkillTree build(ResourceLocation originId) {
        SkillTree tree = new SkillTree(originId, "abyssorigins.skilltree.occultist.name");

        // TIER 1
        tree.addNode(SkillNode.builder("occ_dark_knowledge").lang(P + "dark_knowledge")
            .icon("‡").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(1, 0)
            .stat(Attributes.ARMOR, 1).build());

        // TIER 2
        tree.addNode(SkillNode.builder("occ_veil_sight").lang(P + "veil_sight")
            .icon("◎").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(0, 1)
            .requires("occ_dark_knowledge").build());

        tree.addNode(SkillNode.builder("occ_shadow_rites").lang(P + "shadow_rites")
            .icon("✶").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(2, 1)
            .requires("occ_dark_knowledge").build());

        // TIER 3 — Perception branch
        tree.addNode(SkillNode.builder("occ_soul_drain").lang(P + "soul_drain")
            .icon("♦").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-1, 2)
            .requires("occ_veil_sight").excludes("occ_blood_ritual").build());

        tree.addNode(SkillNode.builder("occ_hex_mark").lang(P + "hex_mark")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(1, 2)
            .requires("occ_veil_sight").build());

        // TIER 3 — Ritual branch
        tree.addNode(SkillNode.builder("occ_blood_ritual").lang(P + "blood_ritual")
            .icon("♥").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(2, 2)
            .requires("occ_shadow_rites").excludes("occ_soul_drain").build());

        tree.addNode(SkillNode.builder("occ_phantom_step").lang(P + "phantom_step")
            .icon("⚡").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(3, 2)
            .requires("occ_shadow_rites").statPct(Attributes.MOVEMENT_SPEED, 0.08).build());

        // TIER 4
        tree.addNode(SkillNode.builder("occ_dark_covenant").lang(P + "dark_covenant")
            .icon("☬").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(0, 3)
            .requires("occ_soul_drain", "occ_hex_mark")
            .stat(Attributes.MAX_HEALTH, 6).stat(Attributes.ARMOR, 2).build());

        tree.addNode(SkillNode.builder("occ_umbral_form").lang(P + "umbral_form")
            .icon("◐").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(2, 3)
            .requires("occ_blood_ritual", "occ_phantom_step")
            .statPct(Attributes.ATTACK_DAMAGE, 0.15).build());

        // TIER 5 — Ultimate
        tree.addNode(SkillNode.builder("occ_ascended_occultist").lang(P + "ascended_occultist")
            .icon("★").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(1, 4)
            .requiresAny("occ_dark_covenant", "occ_umbral_form").build());


        // Apex (tier VI) — the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("occ_apex_veilwalker").lang("abyssorigins.skill.occ_apex_veilwalker")
            .icon("◌").tier(SkillTier.TIER_6).branch(SkillBranch.UTILITY).cost(4).pos(0, 5)
            .requires("occ_ascended_occultist").statPct(Attributes.MOVEMENT_SPEED, 0.08).build());
        tree.addNode(SkillNode.builder("occ_apex_forbidden_lore").lang("abyssorigins.skill.occ_apex_forbidden_lore")
            .icon("✠").tier(SkillTier.TIER_6).branch(SkillBranch.FORBIDDEN).cost(4).pos(2, 5)
            .requires("occ_ascended_occultist").stat(Attributes.MAX_HEALTH, 4).stat(Attributes.ARMOR, 1).build());

        return tree;
    }
}
