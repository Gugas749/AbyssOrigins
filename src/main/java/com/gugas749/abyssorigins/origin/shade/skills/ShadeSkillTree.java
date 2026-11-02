package com.gugas749.abyssorigins.origin.shade.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Veil of Night — left: Meld (stealth), middle: Dusk (living with light),
 * right: Grasp (control). Capstone: Lord of Night or Eternal Night.
 */
public class ShadeSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "shade"),
            "abyssorigins.skilltree.shade.name");

        tree.addNode(SkillNode.builder("shade_deep_shadows").lang(P + "shade_deep_shadows")
            .icon("◑").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1).pos(-1, 0).build());
        tree.addNode(SkillNode.builder("shade_ambush").lang(P + "shade_ambush")
            .icon("†").tier(SkillTier.TIER_1).branch(SkillBranch.OFFENSE).cost(1).pos(1, 0).build());

        tree.addNode(SkillNode.builder("shade_lingering_dark").lang(P + "shade_lingering_dark")
            .icon("☾").tier(SkillTier.TIER_2).branch(SkillBranch.ELEMENTAL_B).cost(2).pos(-1, 1)
            .requires("shade_deep_shadows").build());
        tree.addNode(SkillNode.builder("shade_nightstalker").lang(P + "shade_nightstalker")
            .icon("★").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(2).pos(0, 1)
            .requiresAny("shade_deep_shadows", "shade_ambush").build());
        tree.addNode(SkillNode.builder("shade_tendril_reach").lang(P + "shade_tendril_reach")
            .icon("☘").tier(SkillTier.TIER_2).branch(SkillBranch.OFFENSE).cost(2).pos(1, 1)
            .requires("shade_ambush").build());

        tree.addNode(SkillNode.builder("shade_eclipse_skin").lang(P + "shade_eclipse_skin")
            .icon("◐").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3).pos(-1, 2)
            .requires("shade_lingering_dark").stat(Attributes.ARMOR, 2).build());
        tree.addNode(SkillNode.builder("shade_dusk_dweller").lang(P + "shade_dusk_dweller")
            .icon("☼").tier(SkillTier.TIER_3).branch(SkillBranch.UTILITY).cost(2).pos(0, 2)
            .requires("shade_nightstalker").build());
        tree.addNode(SkillNode.builder("shade_soul_siphon").lang(P + "shade_soul_siphon")
            .icon("♦").tier(SkillTier.TIER_3).branch(SkillBranch.HYBRID).cost(3).pos(1, 2)
            .requires("shade_tendril_reach").build());

        tree.addNode(SkillNode.builder("shade_shadow_clone").lang(P + "shade_shadow_clone")
            .icon("⚇").tier(SkillTier.TIER_4).branch(SkillBranch.DEFENSE).cost(4).pos(-1, 3)
            .requires("shade_eclipse_skin").build());
        tree.addNode(SkillNode.builder("shade_phantom_blade").lang(P + "shade_phantom_blade")
            .icon("⚔").tier(SkillTier.TIER_4).branch(SkillBranch.OFFENSE).cost(4).pos(1, 3)
            .requiresAny("shade_dusk_dweller", "shade_soul_siphon").build());

        tree.addNode(SkillNode.builder("shade_lord_of_night").lang(P + "shade_lord_of_night")
            .icon("♕").tier(SkillTier.TIER_5).branch(SkillBranch.ASCENDED).cost(5).pos(-1, 4)
            .requiresAny("shade_shadow_clone", "shade_phantom_blade").excludes("shade_void_shroud")
            .stat(Attributes.MAX_HEALTH, 4).build());
        tree.addNode(SkillNode.builder("shade_void_shroud").lang(P + "shade_void_shroud")
            .icon("●").tier(SkillTier.TIER_5).branch(SkillBranch.FORBIDDEN).cost(5).pos(1, 4)
            .requiresAny("shade_shadow_clone", "shade_phantom_blade").excludes("shade_lord_of_night").build());


        // Apex (tier VI) — the late-game row beyond the capstones
        tree.addNode(SkillNode.builder("shade_apex_umbral_crown").lang("abyssorigins.skill.shade_apex_umbral_crown")
            .icon("☽").tier(SkillTier.TIER_6).branch(SkillBranch.ASCENDED).cost(4).pos(-1, 5)
            .requiresAny("shade_lord_of_night", "shade_void_shroud").stat(Attributes.ATTACK_DAMAGE, 1.5).build());
        tree.addNode(SkillNode.builder("shade_apex_unseen").lang("abyssorigins.skill.shade_apex_unseen")
            .icon("◌").tier(SkillTier.TIER_6).branch(SkillBranch.UTILITY).cost(4).pos(1, 5)
            .requiresAny("shade_lord_of_night", "shade_void_shroud").statPct(Attributes.MOVEMENT_SPEED, 0.08).build());

        SkillTreeRegistry.register(tree);
    }
}
