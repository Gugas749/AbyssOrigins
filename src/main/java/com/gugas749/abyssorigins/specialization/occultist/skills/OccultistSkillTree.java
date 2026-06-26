package com.gugas749.abyssorigins.specialization.occultist.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.resources.ResourceLocation;

/**
 * Occultist specialization skill tree.
 * Built as a static factory — no separate registration needed.
 * The tree is carried inside OccultistSpecialization and surfaced via getSkillTree().
 *
 * Layout:
 *   Tier 1 (level 1):  Dark Knowledge (core)
 *   Tier 2 (level 2):  Veil Sight, Shadow Rites (branch — perception vs ritual)
 *   Tier 3 (level 5):  Soul Drain, Hex Mark / Blood Ritual, Phantom Step
 *   Tier 4 (level 8):  Dark Covenant, Umbral Form
 *   Tier 5 (level 10): Ascended Occultist
 */
public class OccultistSkillTree {

    private static final String P = "abyssorigins.skill.occultist.";

    public static SkillTree build(ResourceLocation originId) {
        SkillTree tree = new SkillTree(originId, "abyssorigins.skilltree.occultist.name");

        // TIER 1
        tree.addNode(SkillNode.builder("occ_dark_knowledge")
            .name(P + "dark_knowledge.name").desc(P + "dark_knowledge.desc")
            .icon("\u2021").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(1, 0)
            .effect("occ_dark_knowledge").build());

        // TIER 2
        tree.addNode(SkillNode.builder("occ_veil_sight")
            .name(P + "veil_sight.name").desc(P + "veil_sight.desc")
            .icon("\u25CE").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(0, 1)
            .requires("occ_dark_knowledge").effect("occ_veil_sight").build());

        tree.addNode(SkillNode.builder("occ_shadow_rites")
            .name(P + "shadow_rites.name").desc(P + "shadow_rites.desc")
            .icon("\u2736").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(2, 1)
            .requires("occ_dark_knowledge").effect("occ_shadow_rites").build());

        // TIER 3 — Perception branch
        tree.addNode(SkillNode.builder("occ_soul_drain")
            .name(P + "soul_drain.name").desc(P + "soul_drain.desc")
            .icon("\u2666").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(-1, 2)
            .requires("occ_veil_sight")
            .excludes("occ_blood_ritual")
            .effect("occ_soul_drain").build());

        tree.addNode(SkillNode.builder("occ_hex_mark")
            .name(P + "hex_mark.name").desc(P + "hex_mark.desc")
            .icon("\u2620").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(1, 2)
            .requires("occ_veil_sight").effect("occ_hex_mark").build());

        // TIER 3 — Ritual branch
        tree.addNode(SkillNode.builder("occ_blood_ritual")
            .name(P + "blood_ritual.name").desc(P + "blood_ritual.desc")
            .icon("\u2665").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(2, 2)
            .requires("occ_shadow_rites")
            .excludes("occ_soul_drain")
            .effect("occ_blood_ritual").build());

        tree.addNode(SkillNode.builder("occ_phantom_step")
            .name(P + "phantom_step.name").desc(P + "phantom_step.desc")
            .icon("\u26A1").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(3, 2)
            .requires("occ_shadow_rites").effect("occ_phantom_step").build());

        // TIER 4
        tree.addNode(SkillNode.builder("occ_dark_covenant")
            .name(P + "dark_covenant.name").desc(P + "dark_covenant.desc")
            .icon("\u262C").tier(SkillTier.TIER_4).branch(SkillBranch.HYBRID).cost(4).pos(0, 3)
            .requires("occ_soul_drain", "occ_hex_mark").effect("occ_dark_covenant").build());

        tree.addNode(SkillNode.builder("occ_umbral_form")
            .name(P + "umbral_form.name").desc(P + "umbral_form.desc")
            .icon("\u25D0").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(2, 3)
            .requires("occ_blood_ritual", "occ_phantom_step").effect("occ_umbral_form").build());

        // TIER 5 — Ultimate
        tree.addNode(SkillNode.builder("occ_ascended_occultist")
            .name(P + "ascended_occultist.name").desc(P + "ascended_occultist.desc")
            .icon("\u2605").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(1, 4)
            .requires("occ_dark_covenant", "occ_umbral_form")
            .effect("occ_ascended_occultist").build());

        return tree;
    }
}
