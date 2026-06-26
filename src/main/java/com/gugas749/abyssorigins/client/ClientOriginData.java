package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.Origin;

import java.util.*;

/**
 * Client-side cache for all origin data synced from the server.
 * Updated by SyncOriginPacket and SyncSkillsPacket.
 * Read by the HUD overlay, Origin Info screen, and Skill Tree screen.
 */
public class ClientOriginData {

    private static Origin  currentOrigin  = null;
    private static int     level          = 1;
    private static int     xp             = 0;
    private static float   resourceBar    = 100.0f;
    private static int     skillPoints    = 0;
    private static int     cooldown1      = 0;
    private static int     cooldown2      = 0;
    private static int     maxCooldown1   = 1;
    private static int     maxCooldown2   = 1;

    private static final Set<String> unlockedSkills = new HashSet<>();

    private ClientOriginData() {}

    // ---- Update from packets -------------------------------------------

    public static void updateOrigin(Origin origin, float resource, int cd1, int cd2,
                                    int lvl, int xpVal) {
        currentOrigin = origin;
        resourceBar   = resource;
        cooldown1     = cd1;
        cooldown2     = cd2;
        level         = lvl;
        xp            = xpVal;

        // Cache max cooldowns when a new cooldown starts
        if (cd1 > maxCooldown1) maxCooldown1 = cd1;
        if (cd2 > maxCooldown2) maxCooldown2 = cd2;
        // Reset max when cooldown expires
        if (cd1 == 0) maxCooldown1 = 1;
        if (cd2 == 0) maxCooldown2 = 1;
    }

    public static void updateSkills(List<String> skills, int points) {
        unlockedSkills.clear();
        unlockedSkills.addAll(skills);
        skillPoints = points;
    }

    public static void clear() {
        currentOrigin = null;
        level = 1; xp = 0; resourceBar = 100f; skillPoints = 0;
        cooldown1 = 0; cooldown2 = 0; maxCooldown1 = 1; maxCooldown2 = 1;
        unlockedSkills.clear();
    }

    /** Tick down cooldowns client-side for smooth display between syncs. */
    public static void tickCooldowns() {
        if (cooldown1 > 0) cooldown1--;
        if (cooldown2 > 0) cooldown2--;
    }

    // ---- Accessors -----------------------------------------------------

    public static Origin  getOrigin()       { return currentOrigin; }
    public static int     getLevel()        { return level; }
    public static int     getXp()           { return xp; }
    public static float   getResourceBar()  { return resourceBar; }
    public static int     getSkillPoints()  { return skillPoints; }

    public static int getAbilityCooldown(int index) {
        return index == 0 ? cooldown1 : cooldown2;
    }

    public static int getAbilityMaxCooldown(int index) {
        return index == 0 ? maxCooldown1 : maxCooldown2;
    }

    public static float getAbilityCooldownPercent(int index) {
        int cd  = getAbilityCooldown(index);
        int max = getAbilityMaxCooldown(index);
        return max <= 0 ? 0f : (float) cd / max;
    }

    public static Set<String>  getUnlockedSkills()        { return Collections.unmodifiableSet(unlockedSkills); }
    public static boolean      hasSkill(String id)        { return unlockedSkills.contains(id); }

    public static float getXpProgress() {
        int level        = ClientOriginData.level;
        int xpFloor      = xpRequiredToReach(level);      // total XP to reach current level
        int xpCeiling    = xpRequiredToReach(level + 1);  // total XP to reach next level
        int xpIntoLevel  = xp - xpFloor;
        int xpNeeded     = xpCeiling - xpFloor;
        if (xpNeeded <= 0) return 1f;
        return Math.min(1f, (float) xpIntoLevel / xpNeeded);
    }

    public static int getXpForNextLevel() {
        return com.gugas749.abyssorigins.event.XpEventHandler.xpForLevel(level);
    }

    // Helper mirroring the server-side method
    private static int xpRequiredToReach(int targetLevel) {
        int total = 0;
        for (int lvl = 1; lvl < targetLevel; lvl++) {
            total += com.gugas749.abyssorigins.event.XpEventHandler.xpForLevel(lvl);
        }
        return total;
    }
}
