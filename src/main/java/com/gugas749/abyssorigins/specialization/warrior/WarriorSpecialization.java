package com.gugas749.abyssorigins.specialization.warrior;

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

/** Warrior — frontline brawler built around Momentum. */
public class WarriorSpecialization extends Specialization {

    private static final String P = "abyssorigins.skill.";

    public WarriorSpecialization() {
        super(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "warrior"),
            "abyssorigins.spec.warrior.name", "abyssorigins.spec.warrior.description");

        setThemeColor(0xFFE0563C);
        setSigil("⚔");

        addPassive(ClassTraitPassive.of("warrior_momentum"));
        addPassive(ClassTraitPassive.of("warrior_battle_hardened"));

        addAbility(new WarriorAbilities.WarCry());
        addAbility(new WarriorAbilities.Whirlwind());

        addResonance(Resonance.of("demon", "abyssorigins.resonance.warrior_demon.name", "abyssorigins.resonance.warrior_demon.desc"));
        addResonance(Resonance.of("human", "abyssorigins.resonance.warrior_human.name", "abyssorigins.resonance.warrior_human.desc"));

        setHooks(new WarriorHooks());
        setSkillTree(buildTree(getId()));
    }

    /** Path of Steel — left: Iron (survive), middle: Command, right: Blade (kill). */
    private static SkillTree buildTree(ResourceLocation id) {
        SkillTree t = new SkillTree(id, "abyssorigins.skilltree.warrior.name");

        t.addNode(SkillNode.builder("warrior_conditioning").lang(P + "warrior_conditioning")
            .icon("♥").tier(SkillTier.TIER_1).branch(SkillBranch.DEFENSE).cost(1).pos(-1, 0)
            .stat(Attributes.MAX_HEALTH, 4).build());
        t.addNode(SkillNode.builder("warrior_weapon_master").lang(P + "warrior_weapon_master")
            .icon("⚔").tier(SkillTier.TIER_1).branch(SkillBranch.OFFENSE).cost(1).pos(1, 0).build());

        t.addNode(SkillNode.builder("warrior_unyielding").lang(P + "warrior_unyielding")
            .icon("⛨").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2).pos(-1, 1)
            .requires("warrior_conditioning")
            .stat(Attributes.KNOCKBACK_RESISTANCE, 0.3).stat(Attributes.ARMOR, 2).build());
        t.addNode(SkillNode.builder("warrior_battle_trance").lang(P + "warrior_battle_trance")
            .icon("⚑").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(0, 1)
            .requiresAny("warrior_conditioning", "warrior_weapon_master").build());
        t.addNode(SkillNode.builder("warrior_cyclone").lang(P + "warrior_cyclone")
            .icon("✵").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(1, 1)
            .requires("warrior_weapon_master").build());

        t.addNode(SkillNode.builder("warrior_bloodrage").lang(P + "warrior_bloodrage")
            .icon("❣").tier(SkillTier.TIER_3).branch(SkillBranch.HYBRID).cost(3).pos(-1, 2)
            .requires("warrior_unyielding").build());
        t.addNode(SkillNode.builder("warrior_shieldbreaker").lang(P + "warrior_shieldbreaker")
            .icon("⚒").tier(SkillTier.TIER_3).branch(SkillBranch.SPECIALIST).cost(3).pos(0, 2)
            .requires("warrior_battle_trance").build());
        t.addNode(SkillNode.builder("warrior_executioner").lang(P + "warrior_executioner")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(3).pos(1, 2)
            .requires("warrior_cyclone").build());

        t.addNode(SkillNode.builder("warrior_juggernaut").lang(P + "warrior_juggernaut")
            .icon("▣").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 3)
            .requires("warrior_bloodrage").build());
        t.addNode(SkillNode.builder("warrior_relentless").lang(P + "warrior_relentless")
            .icon("⟳").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(1, 3)
            .requiresAny("warrior_shieldbreaker", "warrior_executioner").build());

        t.addNode(SkillNode.builder("warrior_warlord").lang(P + "warrior_warlord")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(0, 4)
            .requiresAny("warrior_juggernaut", "warrior_relentless")
            .stat(Attributes.ARMOR_TOUGHNESS, 2).build());

        // Apex (tier VI) — the late-game row beyond the capstones
        t.addNode(SkillNode.builder("warrior_apex_titan").lang("abyssorigins.skill.warrior_apex_titan")
            .icon("▲").tier(SkillTier.TIER_6).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 5)
            .requires("warrior_warlord").stat(Attributes.MAX_HEALTH, 6).build());
        t.addNode(SkillNode.builder("warrior_apex_war_god").lang("abyssorigins.skill.warrior_apex_war_god")
            .icon("⚔").tier(SkillTier.TIER_6).branch(SkillBranch.OFFENSE).cost(4).pos(1, 5)
            .requires("warrior_warlord").stat(Attributes.ATTACK_DAMAGE, 2).build());

        return t;
    }
}
