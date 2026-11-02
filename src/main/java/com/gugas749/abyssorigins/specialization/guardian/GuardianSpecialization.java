package com.gugas749.abyssorigins.specialization.guardian;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.ClassTraitPassive;
import com.gugas749.abyssorigins.api.Resonance;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.api.StatBonus;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Guardian — the wall. Stuns, taunts, shields allies and takes their pain. */
public class GuardianSpecialization extends Specialization {

    private static final String P = "abyssorigins.skill.";

    public GuardianSpecialization() {
        super(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "guardian"),
            "abyssorigins.spec.guardian.name", "abyssorigins.spec.guardian.description");

        setThemeColor(0xFF5B8DEF);
        setSigil("⛨");

        addStat(StatBonus.add(Attributes.ARMOR, 2));                       // Steadfast
        addStat(StatBonus.add(Attributes.KNOCKBACK_RESISTANCE, 0.15));
        addPassive(ClassTraitPassive.of("guardian_steadfast"));
        addPassive(ClassTraitPassive.of("guardian_bulwark"));

        addAbility(new GuardianAbilities.ShieldBash());
        addAbility(new GuardianAbilities.Aegis());

        addResonance(Resonance.of("seraph", "abyssorigins.resonance.guardian_seraph.name", "abyssorigins.resonance.guardian_seraph.desc"));
        addResonance(Resonance.of("human",  "abyssorigins.resonance.guardian_human.name",  "abyssorigins.resonance.guardian_human.desc"));

        setHooks(new GuardianHooks());
        setSkillTree(buildTree(getId()));
    }

    /** Oath of the Bulwark — left: Stone (self), middle: Oath (punish), right: Ward (allies). */
    private static SkillTree buildTree(ResourceLocation id) {
        SkillTree t = new SkillTree(id, "abyssorigins.skilltree.guardian.name");

        t.addNode(SkillNode.builder("guardian_iron_skin").lang(P + "guardian_iron_skin")
            .icon("▣").tier(SkillTier.TIER_1).branch(SkillBranch.DEFENSE).cost(1).pos(-1, 0)
            .stat(Attributes.ARMOR, 3).build());
        t.addNode(SkillNode.builder("guardian_vigor").lang(P + "guardian_vigor")
            .icon("♥").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(1, 0)
            .stat(Attributes.MAX_HEALTH, 4).build());

        t.addNode(SkillNode.builder("guardian_concussive").lang(P + "guardian_concussive")
            .icon("✸").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(-1, 1)
            .requires("guardian_iron_skin").build());
        t.addNode(SkillNode.builder("guardian_retribution").lang(P + "guardian_retribution")
            .icon("⚖").tier(SkillTier.TIER_2).branch(SkillBranch.SPECIALIST).cost(2).pos(0, 1)
            .requiresAny("guardian_iron_skin", "guardian_vigor").build());
        t.addNode(SkillNode.builder("guardian_rally").lang(P + "guardian_rally")
            .icon("✚").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(1, 1)
            .requires("guardian_vigor").build());

        t.addNode(SkillNode.builder("guardian_bastion").lang(P + "guardian_bastion")
            .icon("♖").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(-1, 2)
            .requires("guardian_concussive").build());
        t.addNode(SkillNode.builder("guardian_provoke").lang(P + "guardian_provoke")
            .icon("❗").tier(SkillTier.TIER_3).branch(SkillBranch.HYBRID).cost(3).pos(0, 2)
            .requires("guardian_retribution").build());
        t.addNode(SkillNode.builder("guardian_second_skin").lang(P + "guardian_second_skin")
            .icon("◐").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(1, 2)
            .requires("guardian_rally").build());

        t.addNode(SkillNode.builder("guardian_unbreakable").lang(P + "guardian_unbreakable")
            .icon("⬡").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 3)
            .requiresAny("guardian_bastion", "guardian_provoke").stat(Attributes.ARMOR_TOUGHNESS, 2).build());
        t.addNode(SkillNode.builder("guardian_martyr").lang(P + "guardian_martyr")
            .icon("✝").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(4).pos(1, 3)
            .requires("guardian_second_skin").build());

        t.addNode(SkillNode.builder("guardian_eternal_vow").lang(P + "guardian_eternal_vow")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(0, 4)
            .requiresAny("guardian_unbreakable", "guardian_martyr").build());

        // Apex (tier VI) — the late-game row beyond the capstones
        t.addNode(SkillNode.builder("guardian_apex_living_bulwark").lang("abyssorigins.skill.guardian_apex_living_bulwark")
            .icon("⛨").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 5)
            .requires("guardian_eternal_vow").stat(Attributes.ARMOR, 3).stat(Attributes.ARMOR_TOUGHNESS, 2).build());
        t.addNode(SkillNode.builder("guardian_apex_undying_oath").lang("abyssorigins.skill.guardian_apex_undying_oath")
            .icon("♥").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(1, 5)
            .requires("guardian_eternal_vow").stat(Attributes.MAX_HEALTH, 6).build());

        return t;
    }
}
