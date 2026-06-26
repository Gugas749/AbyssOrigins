package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.Specialization;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientSpecializationData {

    private static Specialization spec           = null;
    private static int            specLevel      = 0;
    private static int            skillPoints    = 0;
    private static int            abilityCooldown    = 0;
    private static int            abilityMaxCooldown = 1;
    private static final Set<String> unlockedSkills = new HashSet<>();

    private ClientSpecializationData() {}

    public static void update(Specialization s, int level, int points,
                               List<String> skills, int cd, int maxCd) {
        spec = s;
        specLevel = level;
        skillPoints = points;
        unlockedSkills.clear();
        unlockedSkills.addAll(skills);
        if (cd > abilityCooldown) abilityMaxCooldown = cd;
        if (cd == 0) abilityMaxCooldown = Math.max(1, maxCd);
        abilityCooldown = cd;
    }

    public static void clear() {
        spec = null; specLevel = 0; skillPoints = 0;
        abilityCooldown = 0; abilityMaxCooldown = 1;
        unlockedSkills.clear();
    }

    public static void tickCooldown() { if (abilityCooldown > 0) abilityCooldown--; }

    public static Specialization getSpec()         { return spec; }
    public static boolean        hasSpec()         { return spec != null; }
    public static int            getSpecLevel()    { return specLevel; }
    public static int            getSkillPoints()  { return skillPoints; }
    public static int            getAbilityCooldown()    { return abilityCooldown; }
    public static int            getAbilityMaxCooldown() { return abilityMaxCooldown; }
    public static float          getCooldownPercent()    { return abilityMaxCooldown <= 0 ? 0f : (float) abilityCooldown / abilityMaxCooldown; }
    public static Set<String>    getUnlockedSkills()     { return Collections.unmodifiableSet(unlockedSkills); }
    public static boolean        hasSkill(String id)     { return unlockedSkills.contains(id); }
}
