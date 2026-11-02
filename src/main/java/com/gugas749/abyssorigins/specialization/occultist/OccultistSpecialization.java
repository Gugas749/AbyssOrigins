package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.ClassTraitPassive;
import com.gugas749.abyssorigins.api.Resonance;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.specialization.occultist.skills.OccultistSkillTree;
import net.minecraft.resources.ResourceLocation;

/**
 * Occultist — the seer. Reveals what hides (including invisible Shades and Vampires),
 * curses targets with Hexbolt, and sustains through forbidden rites.
 *
 * Available to ALL origins. Assigned by OPs: /abyssorigins specialization set <player> occultist
 *
 * To add a new class, copy this class, change the ID, write passives/abilities/hooks/
 * skill tree, and add one line to SpecializationRegistry.
 */
public class OccultistSpecialization extends Specialization {

    public OccultistSpecialization() {
        super(
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "occultist"),
            "abyssorigins.spec.occultist.name",
            "abyssorigins.spec.occultist.description"
        );

        setThemeColor(0xFFB35CFF);
        setSigil("‡");

        addPassive(new OccultistDarkSensePassive());
        addPassive(ClassTraitPassive.of("occultist_whispers"));

        // F / G
        addAbility(new OccultVisionAbility());
        addAbility(new HexboltAbility());

        addResonance(Resonance.of("voidborn", "abyssorigins.resonance.occultist_voidborn.name", "abyssorigins.resonance.occultist_voidborn.desc"));
        addResonance(Resonance.of("vampire",  "abyssorigins.resonance.occultist_vampire.name",  "abyssorigins.resonance.occultist_vampire.desc"));

        setHooks(new OccultistHooks());
        setSkillTree(OccultistSkillTree.build(getId()));
    }
}
