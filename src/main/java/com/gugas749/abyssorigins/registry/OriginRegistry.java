package com.gugas749.abyssorigins.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.origin.human.*;
import com.gugas749.abyssorigins.origin.human.skills.HumanSkillTree;
import com.gugas749.abyssorigins.origin.vampire.*;
import com.gugas749.abyssorigins.origin.vampire.skills.VampireSkillTree;
import net.minecraft.resources.ResourceLocation;

public class OriginRegistry {

    public static void registerAll() {
        registerHuman();
        registerVampire();
        HumanSkillTree.register();
        VampireSkillTree.register();
    }

    private static void registerHuman() {
        Origin human = new Origin(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "human"),
            "Human",
            "An ordinary person in an extraordinary world. No supernatural gifts — only wit, resilience, and the will to survive.",
            ImpactLevel.LOW
        );
        human.setResourceType(null)
             .addPassive(new HumanRegenPassive())
             .addPassive(new HumanResiliencePassive())
             .addAbility(new HumanAdaptabilityAbility());
        AbyssOriginsAPI.registerOrigin(human);
    }

    private static void registerVampire() {
        // Ability instance shared between ability slot and tick passive
        VampireBatFormAbility batForm = new VampireBatFormAbility();

        Origin vampire = new Origin(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "Vampire",
            "An immortal creature of the night. Blood is your sustenance; sunlight your doom. Master the darkness and prey upon the living.",
            ImpactLevel.HIGH
        );
        vampire.setHealthModifier(1.2f)
               .setDamageModifier(1.1f)
               .setResourceType(ResourceType.blood())
               // Passives
               .addPassive(new VampireHungerPassive())
               .addPassive(new VampireSunPassive())           // helmet does NOT protect — only umbrella or Daywalker skill
               .addPassive(new VampireNightPassive())
               .addPassive(new BloodDrainGazePassive())       // crouch + stare to drain
               .addPassive(new VampireBatFormTickPassive(batForm))
               // Abilities
               .addAbility(new VampireBloodDrainAbility())    // R — instant drain + blood gain
               .addAbility(batForm);                          // V — bat form toggle
        AbyssOriginsAPI.registerOrigin(vampire);
    }
}
