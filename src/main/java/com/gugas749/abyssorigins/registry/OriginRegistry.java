package com.gugas749.abyssorigins.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.origin.human.*;
import com.gugas749.abyssorigins.origin.vampire.*;
import net.minecraft.resources.ResourceLocation;

public class OriginRegistry {

    public static void registerAll() {
        registerHuman();
        registerVampire();
    }

    private static void registerHuman() {
        Origin human = new Origin(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "human"),
            "Human",
            "An ordinary person in an extraordinary world. No supernatural gifts — only wit, resilience, and the will to survive.",
            ImpactLevel.LOW
        );
        human.setHealthModifier(1.0f)
             .setSpeedModifier(1.0f)
             .setDamageModifier(1.0f)
             .setResourceType(null)
             .addPassive(new HumanRegenPassive())
             .addPassive(new HumanResiliencePassive())
             .addAbility(new HumanAdaptabilityAbility());

        AbyssOriginsAPI.registerOrigin(human);
    }

    private static void registerVampire() {
        Origin vampire = new Origin(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire"),
            "Vampire",
            "An immortal creature of the night. Blood is your sustenance; sunlight your doom. Master the darkness and prey upon the living.",
            ImpactLevel.HIGH
        );
        vampire.setHealthModifier(1.2f)    // 24 HP (12 hearts)
               .setSpeedModifier(1.0f)
               .setDamageModifier(1.1f)
               .setResourceType(ResourceType.blood())
               .addPassive(new VampireHungerPassive())
               .addPassive(new VampireSunPassive())
               .addPassive(new VampireNightPassive())
               .addPassive(new VampireBatFormTickPassive())
               .addAbility(new VampireBloodDrainAbility())
               .addAbility(new VampireBatFormAbility());

        AbyssOriginsAPI.registerOrigin(vampire);
    }
}
