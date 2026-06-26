package com.gugas749.abyssorigins.origin.human.skills;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillBranch;
import com.gugas749.abyssorigins.skilltree.data.SkillNode.SkillTier;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.resources.ResourceLocation;

public class HumanSkillTree {

    private static final String P = "abyssorigins.skill.";

    public static void register() {
        SkillTree tree = new SkillTree(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "human"),
            "abyssorigins.skilltree.human.name"
        );

        tree.addNode(SkillNode.builder("human_tough_skin")
            .name(P + "human_tough_skin.name").desc(P + "human_tough_skin.desc")
            .icon("\u2748").tier(SkillTier.TIER_1).branch(SkillBranch.CORE).cost(1)
            .pos(0, 0).effect("human_tough_skin").build());

        tree.addNode(SkillNode.builder("human_iron_will")
            .name(P + "human_iron_will.name").desc(P + "human_iron_will.desc")
            .icon("\u2665").tier(SkillTier.TIER_2).branch(SkillBranch.DEFENSE).cost(2)
            .requires("human_tough_skin").pos(0, 1).effect("human_iron_will").build());

        tree.addNode(SkillNode.builder("human_quick_learner")
            .name(P + "human_quick_learner.name").desc(P + "human_quick_learner.desc")
            .icon("\u2726").tier(SkillTier.TIER_2).branch(SkillBranch.UTILITY).cost(1)
            .requires("human_tough_skin").pos(1, 1).effect("human_quick_learner").build());

        tree.addNode(SkillNode.builder("human_fighter")
            .name(P + "human_fighter.name").desc(P + "human_fighter.desc")
            .icon("\u2694").tier(SkillTier.TIER_3).branch(SkillBranch.OFFENSE).cost(2)
            .requires("human_iron_will").pos(0, 2).effect("human_fighter").build());

        tree.addNode(SkillNode.builder("human_endurance")
            .name(P + "human_endurance.name").desc(P + "human_endurance.desc")
            .icon("\u2764").tier(SkillTier.TIER_3).branch(SkillBranch.DEFENSE).cost(3)
            .requires("human_quick_learner").pos(1, 2).effect("human_endurance").build());

        tree.addNode(SkillNode.builder("human_swift")
            .name(P + "human_swift.name").desc(P + "human_swift.desc")
            .icon("\u26A1").tier(SkillTier.TIER_4).branch(SkillBranch.UTILITY).cost(2)
            .requires("human_fighter").pos(0, 3).effect("human_swift").build());

        tree.addNode(SkillNode.builder("human_last_stand")
            .name(P + "human_last_stand.name").desc(P + "human_last_stand.desc")
            .icon("\u2605").tier(SkillTier.TIER_4).branch(SkillBranch.SPECIALIST).cost(3)
            .requires("human_endurance").pos(1, 3).effect("human_last_stand").build());

        SkillTreeRegistry.register(tree);
    }
}
