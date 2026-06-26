package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.specialization.occultist.skills.OccultistSkillTree;
import net.minecraft.resources.ResourceLocation;

/**
 * Occultist Specialization.
 *
 * Available to ALL origins — any player can become an occultist via RP.
 * Assigned exclusively by OPs: /abyssorigins specialization set <player> occultist
 *
 * To add a new specialization in the future, copy this class, change the ID,
 * write your passives/abilities/skill tree, and add one line to SpecializationRegistry.
 */
public class OccultistSpecialization extends Specialization {

    public OccultistSpecialization() {
        super(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "occultist"),
            "abyssorigins.spec.occultist.name",
            "abyssorigins.spec.occultist.description"
        );

        setThemeColor(0xFF9922CC); // purple

        // Available to all origins — no restriction
        // allowedOrigins("vampire") <- would restrict to vampires only if needed

        // Passives
        addPassive(new OccultistDarkSensePassive());

        // Abilities — bound to F key (spec ability slot)
        addAbility(new OccultVisionAbility());

        // Skill tree — built inline, no separate registration needed
        setSkillTree(OccultistSkillTree.build(getId()));
    }
}
