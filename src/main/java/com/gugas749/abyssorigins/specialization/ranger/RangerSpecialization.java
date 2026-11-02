package com.gugas749.abyssorigins.specialization.ranger;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.ClassTraitPassive;
import com.gugas749.abyssorigins.api.Resonance;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Ranger — marks prey and punishes it from range. */
public class RangerSpecialization extends Specialization {

    private static final String P = "abyssorigins.skill.";

    public RangerSpecialization() {
        super(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "ranger"),
            "abyssorigins.spec.ranger.name", "abyssorigins.spec.ranger.description");

        setThemeColor(0xFF5FD37A);
        setSigil("➶");

        addPassive(ClassTraitPassive.of("ranger_steady_aim"));
        addPassive(ClassTraitPassive.of("ranger_hunters_instinct"));

        addAbility(new RangerAbilities.HuntersMark());
        addAbility(new RangerAbilities.Volley());

        addResonance(Resonance.of("voidborn", "abyssorigins.resonance.ranger_voidborn.name", "abyssorigins.resonance.ranger_voidborn.desc"));
        addResonance(Resonance.of("seraph",   "abyssorigins.resonance.ranger_seraph.name",   "abyssorigins.resonance.ranger_seraph.desc"));

        setHooks(new RangerHooks());
        setSkillTree(buildTree(getId()));
    }

    /** Wild Hunt — left: Volley, middle: Prey, right: Survival. */
    private static SkillTree buildTree(ResourceLocation id) {
        SkillTree t = new SkillTree(id, "abyssorigins.skilltree.ranger.name");

        t.addNode(SkillNode.builder("ranger_eagle_eye").lang(P + "ranger_eagle_eye")
            .icon("◎").tier(SkillTier.TIER_1).branch(SkillBranch.OFFENSE).cost(1).pos(-1, 0).build());
        t.addNode(SkillNode.builder("ranger_fleetfoot").lang(P + "ranger_fleetfoot")
            .icon("»").tier(SkillTier.TIER_1).branch(SkillBranch.UTILITY).cost(1).pos(1, 0)
            .statPct(Attributes.MOVEMENT_SPEED, 0.08).build());

        t.addNode(SkillNode.builder("ranger_split_volley").lang(P + "ranger_split_volley")
            .icon("➶").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-1, 1)
            .requires("ranger_eagle_eye").build());
        t.addNode(SkillNode.builder("ranger_predator_mark").lang(P + "ranger_predator_mark")
            .icon("⌖").tier(SkillTier.TIER_2).branch(SkillBranch.SPECIALIST).cost(2).pos(0, 1)
            .requiresAny("ranger_eagle_eye", "ranger_fleetfoot").build());
        t.addNode(SkillNode.builder("ranger_survivalist").lang(P + "ranger_survivalist")
            .icon("☘").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(1, 1)
            .requires("ranger_fleetfoot").stat(Attributes.MAX_HEALTH, 2).build());

        t.addNode(SkillNode.builder("ranger_broadhead").lang(P + "ranger_broadhead")
            .icon("❣").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-1, 2)
            .requires("ranger_split_volley").build());
        t.addNode(SkillNode.builder("ranger_quarry").lang(P + "ranger_quarry")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.HYBRID).cost(3).pos(0, 2)
            .requires("ranger_predator_mark").build());
        t.addNode(SkillNode.builder("ranger_trapper").lang(P + "ranger_trapper")
            .icon("⌘").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(3).pos(1, 2)
            .requires("ranger_survivalist").build());

        t.addNode(SkillNode.builder("ranger_storm_of_arrows").lang(P + "ranger_storm_of_arrows")
            .icon("☔").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-1, 3)
            .requires("ranger_broadhead").build());
        t.addNode(SkillNode.builder("ranger_heartseeker").lang(P + "ranger_heartseeker")
            .icon("♥").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(4).pos(1, 3)
            .requiresAny("ranger_quarry", "ranger_trapper").build());

        t.addNode(SkillNode.builder("ranger_apex_hunter").lang(P + "ranger_apex_hunter")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(0, 4)
            .requiresAny("ranger_storm_of_arrows", "ranger_heartseeker").build());

        // Apex (tier VI) — the late-game row beyond the capstones
        t.addNode(SkillNode.builder("ranger_apex_windrunner").lang("abyssorigins.skill.ranger_apex_windrunner")
            .icon("»").tier(SkillTier.TIER_6).branch(SkillBranch.UTILITY).cost(4).pos(-1, 5)
            .requires("ranger_apex_hunter").statPct(Attributes.MOVEMENT_SPEED, 0.1).build());
        t.addNode(SkillNode.builder("ranger_apex_wild_heart").lang("abyssorigins.skill.ranger_apex_wild_heart")
            .icon("❀").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(1, 5)
            .requires("ranger_apex_hunter").stat(Attributes.MAX_HEALTH, 4).stat(Attributes.KNOCKBACK_RESISTANCE, 0.1).build());

        return t;
    }
}
