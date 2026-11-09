package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.event.XpEventHandler;

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

    private static final Set<String> unlockedSkills = new HashSet<>();

    private ClientOriginData() {}

    // ---- Update from packets -------------------------------------------

    public static void updateOrigin(Origin origin, float resource, int lvl, int xpVal) {
        currentOrigin  = origin;
        resourceBar    = resource;
        level          = lvl;
        xp             = xpVal;
    }

    public static void updateSkills(List<String> skills, int points) {
        unlockedSkills.clear();
        unlockedSkills.addAll(skills);
        skillPoints = points;
    }

    public static void clear() {
        currentOrigin = null;
        level = 1; xp = 0; resourceBar = 100f; skillPoints = 0;
        unlockedSkills.clear();
    }

    // ---- Accessors -----------------------------------------------------

    public static Origin  getOrigin()       { return currentOrigin; }
    public static int     getLevel()        { return level; }
    public static float   getResourceBar()  { return resourceBar; }
    public static int     getSkillPoints()  { return skillPoints; }

    public static Set<String>  getUnlockedSkills()        { return Collections.unmodifiableSet(unlockedSkills); }
    public static boolean      hasSkill(String id)        { return unlockedSkills.contains(id); }

    public static float getXpProgress() {
        int xpFloor   = XpEventHandler.xpRequiredToReach(level);
        int xpCeiling = XpEventHandler.xpRequiredToReach(level + 1);
        if (xpCeiling <= xpFloor) return 1f;
        return Math.max(0f, Math.min(1f, (float) (xp - xpFloor) / (xpCeiling - xpFloor)));
    }

    /** XP earned inside the current level. */
    public static int getXpIntoLevel()   { return xp - XpEventHandler.xpRequiredToReach(level); }
}
