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
            "abyssorigins.origin.human.name",
            "abyssorigins.origin.human.description",
            ImpactLevel.LOW
        );
        human.setResourceType(null)
             .addPassive(new HumanRegenPassive())
             .addPassive(new HumanResiliencePassive())
             .addAbility(new HumanAdaptabilityAbility());
        AbyssOriginsAPI.registerOrigin(human);
    }

    private static void registerVampire() {
        VampireBatFormAbility batForm = new VampireBatFormAbility();

        Origin vampire = new Origin(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "abyssorigins.origin.vampire.name",
            "abyssorigins.origin.vampire.description",
            ImpactLevel.HIGH
        );
        vampire.setHealthModifier(1.2f)
               .setDamageModifier(1.1f)
               .setResourceType(ResourceType.blood())
               .addPassive(new VampireHungerPassive())
               .addPassive(new VampireSunPassive())
               .addPassive(new VampireNightPassive())
               .addPassive(new BloodDrainGazePassive())
               .addPassive(new VampireBatFormTickPassive(batForm))
               .addAbility(new VampireBloodDrainAbility())
               .addAbility(batForm);
        AbyssOriginsAPI.registerOrigin(vampire);
    }
}
