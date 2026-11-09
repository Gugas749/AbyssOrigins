package com.gugas749.abyssorigins.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.origin.demon.DemonAbilities;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.origin.eminence.EminenceAbilities;
import com.gugas749.abyssorigins.origin.eminence.EminenceHooks;
import com.gugas749.abyssorigins.origin.demon.DemonHooks;
import com.gugas749.abyssorigins.origin.demon.skills.DemonSkillTree;
import com.gugas749.abyssorigins.origin.human.*;
import com.gugas749.abyssorigins.origin.human.skills.HumanSkillTree;
import com.gugas749.abyssorigins.origin.seraph.SeraphAbilities;
import com.gugas749.abyssorigins.origin.seraph.SeraphHooks;
import com.gugas749.abyssorigins.origin.seraph.skills.SeraphSkillTree;
import com.gugas749.abyssorigins.origin.shade.ShadeAbilities;
import com.gugas749.abyssorigins.origin.shade.ShadeHooks;
import com.gugas749.abyssorigins.origin.shade.skills.ShadeSkillTree;
import com.gugas749.abyssorigins.origin.vampire.*;
import com.gugas749.abyssorigins.origin.vampire.skills.VampireSkillTree;
import com.gugas749.abyssorigins.origin.voidborn.VoidbornAbilities;
import com.gugas749.abyssorigins.origin.voidborn.VoidbornHooks;
import com.gugas749.abyssorigins.origin.voidborn.skills.VoidbornSkillTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * All built-in origins. Each origin = identity (colour, sigil, traits) + resource +
 * two abilities (R / V) + passives + PowerHooks + a skill tree.
 */
public class OriginRegistry {

    private static final String O = "abyssorigins.origin.";

    public static void registerAll() {
        registerHuman();
        registerVampire();
        registerDemon();
        registerSeraph();
        registerVoidborn();
        registerShade();
        registerEminence();

        HumanSkillTree.register();
        VampireSkillTree.register();
        DemonSkillTree.register();
        SeraphSkillTree.register();
        VoidbornSkillTree.register();
        ShadeSkillTree.register();
    }

    private static Origin create(String path, ImpactLevel impact) {
        return new Origin(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path),
            O + path + ".name", O + path + ".description", impact);
    }

    private static void registerHuman() {
        Origin human = create("human", ImpactLevel.LOW);
        human.setThemeColor(0xFFE8C872).setSigil("✦")
             .setResourceType(null)
             .addTrait(O + "human.trait.1", true)
             .addTrait(O + "human.trait.2", true)
             .addTrait(O + "human.trait.3", false)
             .addPassive(new HumanRegenPassive())
             .addPassive(new HumanResiliencePassive())
             .addPassive(new HumanIngenuityPassive())
             .addAbility(new HumanAdaptabilityAbility())
             .addAbility(new HumanSecondWindAbility())
             .setHooks(new HumanHooks());
        AbyssOriginsAPI.registerOrigin(human);
    }

    private static void registerVampire() {
        Origin vampire = create("vampire", ImpactLevel.HIGH);
        vampire.setThemeColor(0xFFC0182E).setSigil("❧")
               .setHealthModifier(1.2f)
               .setDamageModifier(1.1f)
               .setResourceType(ResourceType.blood())
               .addTrait(O + "vampire.trait.1", true)
               .addTrait(O + "vampire.trait.2", true)
               .addTrait(O + "vampire.trait.3", false)
               .addTrait(O + "vampire.trait.4", false)
               .addPassive(new VampireHungerPassive())
               .addPassive(new VampireSunPassive())
               .addPassive(new VampireNightPassive())
               .addPassive(new BloodDrainGazePassive())
               .addAbility(new VampireBloodDrainAbility())
               .addAbility(new VampireBatFormAbility())
               .setHooks(new VampireHooks());
        AbyssOriginsAPI.registerOrigin(vampire);
    }

    private static void registerDemon() {
        Origin demon = create("demon", ImpactLevel.HIGH);
        demon.setThemeColor(0xFFFF6A1F).setSigil("✹")
             .setHealthModifier(1.1f)
             .setResourceType(ResourceType.wrath())
             .addTrait(O + "demon.trait.1", true)
             .addTrait(O + "demon.trait.2", true)
             .addTrait(O + "demon.trait.3", false)
             .addPassive(TraitPassive.of("demon_hellborn"))
             .addPassive(TraitPassive.of("demon_wrath"))
             .addPassive(TraitPassive.of("demon_nether_native"))
             .addPassive(TraitPassive.of("demon_doused"))
             .addAbility(new DemonAbilities.HellfireNova())
             .addAbility(new DemonAbilities.InfernalCharge())
             .setHooks(new DemonHooks());
        AbyssOriginsAPI.registerOrigin(demon);
    }

    private static void registerSeraph() {
        Origin seraph = create("seraph", ImpactLevel.MEDIUM);
        seraph.setThemeColor(0xFFF6D77A).setSigil("✧")
              .setHealthModifier(0.9f)
              .addStat(StatBonus.percent(Attributes.GRAVITY, -0.15))
              .setResourceType(ResourceType.radiance())
              .addTrait(O + "seraph.trait.1", true)
              .addTrait(O + "seraph.trait.2", true)
              .addTrait(O + "seraph.trait.3", false)
              .addTrait(O + "seraph.trait.4", false)
              .addPassive(TraitPassive.of("seraph_feathered"))
              .addPassive(TraitPassive.of("seraph_sunlit_grace"))
              .addPassive(TraitPassive.of("seraph_holy_bane"))
              .addPassive(TraitPassive.of("seraph_earthbound"))
              .addAbility(new SeraphAbilities.AscendantWings())
              .addAbility(new SeraphAbilities.DivineLight())
              .setHooks(new SeraphHooks());
        AbyssOriginsAPI.registerOrigin(seraph);
    }

    private static void registerVoidborn() {
        Origin voidborn = create("voidborn", ImpactLevel.HIGH);
        voidborn.setThemeColor(0xFF9B6BFF).setSigil("◈")
                .setHealthModifier(0.9f)
                .setSpeedModifier(1.05f)
                .setResourceType(ResourceType.voidEssence())
                .addTrait(O + "voidborn.trait.1", true)
                .addTrait(O + "voidborn.trait.2", true)
                .addTrait(O + "voidborn.trait.3", false)
                .addPassive(TraitPassive.of("void_entropic_touch"))
                .addPassive(TraitPassive.of("void_unanchored"))
                .addPassive(TraitPassive.of("void_hollow_form"))
                .addAbility(new VoidbornAbilities.VoidStep())
                .addAbility(new VoidbornAbilities.RiftCollapse())
                .setHooks(new VoidbornHooks());
        AbyssOriginsAPI.registerOrigin(voidborn);
    }

    /**
     * The Eminence — hidden sovereign of every bloodline. Not listed anywhere; granted only
     * with /abyssorigins set <player> eminence. Holds every origin skill and power, binds any
     * five to its sovereign keys, and rules with Presence, Royal Decree and Usurp.
     */
    private static void registerEminence() {
        Origin eminence = create(Eminence.PATH, ImpactLevel.HIGH);
        eminence.setHidden(true)
                .setThemeColor(0xFF000000 | Eminence.COLOR).setSigil("♛")
                // Sovereign Body: the crown has no tree of its own, so its strength is innate.
                // Health: (20 + 16) x 1.25 = 45 (22.5 hearts). Damage: (base + 2) x 1.30.
                .setHealthModifier(1.25f)
                .setDamageModifier(1.30f)
                .setSpeedModifier(1.10f)
                .addStat(StatBonus.add(Attributes.MAX_HEALTH, 16))
                .addStat(StatBonus.add(Attributes.ARMOR, 8))
                .addStat(StatBonus.add(Attributes.ARMOR_TOUGHNESS, 4))
                .addStat(StatBonus.add(Attributes.KNOCKBACK_RESISTANCE, 0.4))
                .addStat(StatBonus.add(Attributes.ATTACK_DAMAGE, 2))
                .addStat(StatBonus.percent(Attributes.ATTACK_SPEED, 0.15))
                .addStat(StatBonus.add(Attributes.SAFE_FALL_DISTANCE, 4))
                .setResourceType(ResourceType.dominion())
                .addTrait(O + "eminence.trait.1", true)
                .addTrait(O + "eminence.trait.2", true)
                .addTrait(O + "eminence.trait.3", true)
                .addTrait(O + "eminence.trait.4", false)
                .addPassive(TraitPassive.of("eminence_crown"))
                .addPassive(TraitPassive.of("eminence_sovereign_body"))
                .addPassive(TraitPassive.of("eminence_unbowed"))
                // Inherited bloodline passives (their text lives with their own origins)
                .addPassive(new HumanRegenPassive().hidden())
                .addPassive(new HumanResiliencePassive().hidden())
                .addPassive(new VampireNightPassive().hidden())
                .addPassive(new BloodDrainGazePassive().hidden())
                .addAbility(new EminenceAbilities.Presence())
                .addAbility(new EminenceAbilities.RoyalDecree())
                .addAbility(new EminenceAbilities.Usurp())
                .setHooks(new EminenceHooks());
        AbyssOriginsAPI.registerOrigin(eminence);
    }

    private static void registerShade() {
        ShadeAbilities.ShadowMeld meld = new ShadeAbilities.ShadowMeld();
        Origin shade = create("shade", ImpactLevel.MEDIUM);
        shade.setThemeColor(0xFF7D93C9).setSigil("☾")
             .setHealthModifier(0.9f)
             .setSpeedModifier(1.1f)
             .setResourceType(ResourceType.umbra())
             .addTrait(O + "shade.trait.1", true)
             .addTrait(O + "shade.trait.2", true)
             .addTrait(O + "shade.trait.3", false)
             .addPassive(TraitPassive.of("shade_umbral_sight"))
             .addPassive(TraitPassive.of("shade_shadow_woven"))
             .addPassive(TraitPassive.of("shade_light_bane"))
             .addAbility(meld)
             .addAbility(new ShadeAbilities.UmbralGrasp())
             .setHooks(new ShadeHooks(meld));
        AbyssOriginsAPI.registerOrigin(shade);
    }
}
