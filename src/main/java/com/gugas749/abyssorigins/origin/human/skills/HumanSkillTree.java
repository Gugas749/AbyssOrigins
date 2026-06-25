package com.gugas749.abyssorigins.origin.human.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;

public class HumanSkillTree {

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "human"),
            "Human Mastery"
        );

        tree.addNode(SkillNode.builder("human_tough_skin")
            .name("Tough Skin").desc("Gain +2 armor from years of hard work.")
            .icon("\u2748").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1)
            .pos(0, 0).effect("human_tough_skin").build());

        tree.addNode(SkillNode.builder("human_iron_will")
            .name("Iron Will").desc("Your determination grants +2 hearts of maximum health.")
            .icon("\u2665").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2)
            .requires("human_tough_skin").pos(0, 1).effect("human_iron_will").build());

        tree.addNode(SkillNode.builder("human_quick_learner")
            .name("Quick Learner").desc("You learn faster. Passive regen boost when well-fed.")
            .icon("\u2726").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(1)
            .requires("human_tough_skin").pos(1, 1).effect("human_quick_learner").build());

        tree.addNode(SkillNode.builder("human_fighter")
            .name("Fighter").desc("+1 melee attack damage. You were born to fight.")
            .icon("\u2694").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(2)
            .requires("human_iron_will").pos(0, 2).effect("human_fighter").build());

        tree.addNode(SkillNode.builder("human_endurance")
            .name("Endurance").desc("+3 hearts of maximum health.")
            .icon("\u2764").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3)
            .requires("human_quick_learner").pos(1, 2).effect("human_endurance").build());

        tree.addNode(SkillNode.builder("human_swift")
            .name("Swift").desc("+10% movement speed.")
            .icon("\u26A1").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(2)
            .requires("human_fighter").pos(0, 3).effect("human_swift").build());

        tree.addNode(SkillNode.builder("human_last_stand")
            .name("Last Stand").desc("Below 30% HP: gain Resistance II.")
            .icon("\u2605").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(3)
            .requires("human_endurance").pos(1, 3).effect("human_last_stand").build());

        SkillTreeRegistry.register(tree);
    }
}
