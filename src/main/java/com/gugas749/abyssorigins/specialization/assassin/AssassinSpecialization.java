package com.gugas749.abyssorigins.specialization.assassin;

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

/** Assassin — burst from behind, poisons, and slippery survival. */
public class AssassinSpecialization extends Specialization {

    private static final String P = "abyssorigins.skill.";

    public AssassinSpecialization() {
        super(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "assassin"),
            "abyssorigins.spec.assassin.name", "abyssorigins.spec.assassin.description");

        setThemeColor(0xFF3FB58A);
        setSigil("†");

        addStat(StatBonus.percent(Attributes.FALL_DAMAGE_MULTIPLIER, -0.4));   // Light Feet
        addPassive(ClassTraitPassive.of("assassin_backstab"));
        addPassive(ClassTraitPassive.of("assassin_light_feet"));

        addAbility(new AssassinAbilities.Shadowstep());
        addAbility(new AssassinAbilities.VenomEdge());

        addResonance(Resonance.of("shade",   "abyssorigins.resonance.assassin_shade.name",   "abyssorigins.resonance.assassin_shade.desc"));
        addResonance(Resonance.of("vampire", "abyssorigins.resonance.assassin_vampire.name", "abyssorigins.resonance.assassin_vampire.desc"));

        setHooks(new AssassinHooks());
        setSkillTree(buildTree(getId()));
    }

    /** Whispering Blades — left: Venom, middle: Opportunity, right: Evasion. */
    private static SkillTree buildTree(ResourceLocation id) {
        SkillTree t = new SkillTree(id, "abyssorigins.skilltree.assassin.name");

        t.addNode(SkillNode.builder("assassin_keen_edge").lang(P + "assassin_keen_edge")
            .icon("†").tier(SkillTier.TIER_1).branch(SkillBranch.OFFENSE).cost(1).pos(-1, 0).build());
        t.addNode(SkillNode.builder("assassin_fleet").lang(P + "assassin_fleet")
            .icon("»").tier(SkillTier.TIER_1).branch(SkillBranch.UTILITY).cost(1).pos(1, 0)
            .statPct(Attributes.MOVEMENT_SPEED, 0.08).build());

        t.addNode(SkillNode.builder("assassin_toxicology").lang(P + "assassin_toxicology")
            .icon("⚗").tier(SkillTier.TIER_2).branch(SkillBranch.ELEMENTAL_A).cost(2).pos(-1, 1)
            .requires("assassin_keen_edge").build());
        t.addNode(SkillNode.builder("assassin_opportunist").lang(P + "assassin_opportunist")
            .icon("◎").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(0, 1)
            .requiresAny("assassin_keen_edge", "assassin_fleet").build());
        t.addNode(SkillNode.builder("assassin_vanish").lang(P + "assassin_vanish")
            .icon("◌").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(1, 1)
            .requires("assassin_fleet").build());

        t.addNode(SkillNode.builder("assassin_hemorrhage").lang(P + "assassin_hemorrhage")
            .icon("❣").tier(SkillTier.TIER_3).branch(SkillBranch.ELEMENTAL_A).cost(3).pos(-1, 2)
            .requires("assassin_toxicology").build());
        t.addNode(SkillNode.builder("assassin_mark_for_death").lang(P + "assassin_mark_for_death")
            .icon("☠").tier(SkillTier.TIER_3).branch(SkillBranch.SPECIALIST).cost(3).pos(0, 2)
            .requires("assassin_opportunist").build());
        t.addNode(SkillNode.builder("assassin_evasion").lang(P + "assassin_evasion")
            .icon("⁑").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(1, 2)
            .requires("assassin_vanish").build());

        t.addNode(SkillNode.builder("assassin_cutthroat").lang(P + "assassin_cutthroat")
            .icon("⟳").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(-1, 3)
            .requiresAny("assassin_hemorrhage", "assassin_mark_for_death").build());
        t.addNode(SkillNode.builder("assassin_smoke_bomb").lang(P + "assassin_smoke_bomb")
            .icon("☁").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(1, 3)
            .requires("assassin_evasion").build());

        t.addNode(SkillNode.builder("assassin_deathmark").lang(P + "assassin_deathmark")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(0, 4)
            .requiresAny("assassin_cutthroat", "assassin_smoke_bomb").build());

        // Apex (tier VI) — the late-game row beyond the capstones
        t.addNode(SkillNode.builder("assassin_apex_shadow_dance").lang("abyssorigins.skill.assassin_apex_shadow_dance")
            .icon("≈").tier(SkillTier.TIER_6).branch(SkillBranch.UTILITY).cost(4).pos(-1, 5)
            .requires("assassin_deathmark").statPct(Attributes.MOVEMENT_SPEED, 0.1).build());
        t.addNode(SkillNode.builder("assassin_apex_killing_art").lang("abyssorigins.skill.assassin_apex_killing_art")
            .icon("✝").tier(SkillTier.TIER_6).branch(SkillBranch.OFFENSE).cost(4).pos(1, 5)
            .requires("assassin_deathmark").stat(Attributes.ATTACK_DAMAGE, 1.5).build());

        return t;
    }
}
