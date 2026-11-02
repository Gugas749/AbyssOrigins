package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.event.XpEventHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientSpecializationData {

    private static Specialization spec           = null;
    private static int            specLevel      = 0;
    private static int            specXp         = 0;
    private static int            skillPoints    = 0;
    private static final Set<String> unlockedSkills = new HashSet<>();

    private ClientSpecializationData() {}

    public static void update(Specialization s, int level, int xp, int points, List<String> skills) {
        spec = s;
        specLevel = level;
        specXp = xp;
        skillPoints = points;
        unlockedSkills.clear();
        unlockedSkills.addAll(skills);
    }

    public static void clear() {
        spec = null; specLevel = 0; specXp = 0; skillPoints = 0;
        unlockedSkills.clear();
    }

    public static Specialization getSpec()         { return spec; }
    public static boolean        hasSpec()         { return spec != null; }
    public static int            getSpecLevel()    { return specLevel; }
    public static int            getSpecXp()       { return specXp; }
    public static int            getSkillPoints()  { return skillPoints; }
    public static Set<String>    getUnlockedSkills()     { return Collections.unmodifiableSet(unlockedSkills); }
    public static boolean        hasSkill(String id)     { return unlockedSkills.contains(id); }

    public static float getXpProgress() {
        int lvl = Math.max(1, specLevel);
        int floor = XpEventHandler.xpRequiredToReach(lvl), ceil = XpEventHandler.xpRequiredToReach(lvl + 1);
        if (ceil <= floor) return 1f;
        return Math.max(0f, Math.min(1f, (float) (specXp - floor) / (ceil - floor)));
    }
}
