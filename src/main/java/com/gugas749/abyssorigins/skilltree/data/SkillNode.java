package com.gugas749.abyssorigins.skilltree.data;

import java.util.*;

/**
 * A single unlockable node in a skill tree.
 * Fully replaces the previous SkillNode — drop-in replacement.
 */
public class SkillNode {

    // ---- Enums ----------------------------------------------------------

    public enum SkillBranch {
        CORE,
        OFFENSE, DEFENSE, UTILITY,
        ELEMENTAL_A, ELEMENTAL_B,
        HYBRID, SPECIALIST,
        FORBIDDEN, ASCENDED,
        SYNERGY_AD, SYNERGY_AU, SYNERGY_DU
    }

    public enum SkillTier {
        TIER_1(1, 0),
        TIER_2(2, 5),
        TIER_3(3, 15),
        TIER_4(4, 25),
        TIER_5(5, 40);

        private final int tier;
        private final int levelRequired;

        SkillTier(int tier, int levelRequired) {
            this.tier = tier;
            this.levelRequired = levelRequired;
        }

        public int getTier()          { return tier; }
        public int getLevelRequired() { return levelRequired; }
    }

    // ---- Fields ---------------------------------------------------------

    private final String id;
    private final String displayName;
    private final String description;
    private final String icon;
    private final SkillTier tier;
    private final SkillBranch branch;
    private final int pointCost;
    private final int gridX;
    private final int gridY;
    private final List<String> prerequisites;
    private final List<String> exclusions;
    private final String effectId;

    private SkillNode(Builder b) {
        this.id           = b.id;
        this.displayName  = b.displayName;
        this.description  = b.description;
        this.icon         = b.icon;
        this.tier         = b.tier;
        this.branch       = b.branch;
        this.pointCost    = b.pointCost;
        this.gridX        = b.gridX;
        this.gridY        = b.gridY;
        this.prerequisites = Collections.unmodifiableList(b.prerequisites);
        this.exclusions   = Collections.unmodifiableList(b.exclusions);
        this.effectId     = b.effectId;
    }

    // ---- Accessors ------------------------------------------------------

    public String getId()              { return id; }
    public String getDisplayName()     { return displayName; }
    public String getDescription()     { return description; }
    public String getIcon()            { return icon; }
    public SkillTier getTier()         { return tier; }
    public SkillBranch getBranch()     { return branch; }
    public int getPointCost()          { return pointCost; }
    public int getGridX()              { return gridX; }
    public int getGridY()              { return gridY; }
    /** Alias for GUI compat */
    public int getGridCol()            { return gridX; }
    /** Alias for GUI compat */
    public int getGridRow()            { return gridY; }
    public List<String> getPrereqs()   { return prerequisites; }
    public List<String> getPrerequisites() { return prerequisites; }
    public List<String> getExclusions(){ return exclusions; }
    public String getEffectId()        { return effectId; }

    // ---- Logic ----------------------------------------------------------

    public boolean canUnlock(Set<String> unlockedSkills, Set<String> excludedSkills, int playerLevel) {
        if (playerLevel < tier.getLevelRequired()) return false;
        if (excludedSkills.contains(id)) return false;
        for (String prereq : prerequisites) {
            if (!unlockedSkills.contains(prereq)) return false;
        }
        return true;
    }

    public int getBranchColor() {
        return switch (branch) {
            case CORE        -> 0xFFCCCCCC;
            case OFFENSE     -> 0xFFFF4444;
            case DEFENSE     -> 0xFF4444FF;
            case UTILITY     -> 0xFF44FF44;
            case ELEMENTAL_A -> 0xFFFF8800;
            case ELEMENTAL_B -> 0xFF00CCFF;
            case HYBRID      -> 0xFFAA44AA;
            case SPECIALIST  -> 0xFFFFFF00;
            case FORBIDDEN   -> 0xFF880000;
            case ASCENDED    -> 0xFFFFFFAA;
            case SYNERGY_AD  -> 0xFFFF44FF;
            case SYNERGY_AU  -> 0xFFFFAA44;
            case SYNERGY_DU  -> 0xFF44FFFF;
        };
    }

    public int getTierColor() {
        return switch (tier) {
            case TIER_1 -> 0xFF666666;
            case TIER_2 -> 0xFF44AA44;
            case TIER_3 -> 0xFF4444DD;
            case TIER_4 -> 0xFFAA44AA;
            case TIER_5 -> 0xFFFFAA00;
        };
    }

    // ---- Builder --------------------------------------------------------

    public static Builder builder(String id) { return new Builder(id); }

    public static class Builder {
        private final String id;
        private String displayName   = "Unnamed Skill";
        private String description   = "";
        private String icon          = "\u2605"; // ★
        private SkillTier tier       = SkillTier.TIER_1;
        private SkillBranch branch   = SkillBranch.CORE;
        private int pointCost        = 1;
        private int gridX            = 0;
        private int gridY            = 0;
        private final List<String> prerequisites = new ArrayList<>();
        private final List<String> exclusions    = new ArrayList<>();
        private String effectId      = "";

        public Builder(String id)                          { this.id = id; }
        public Builder name(String n)                      { this.displayName = n;   return this; }
        public Builder desc(String d)                      { this.description = d;   return this; }
        public Builder icon(String i)                      { this.icon = i;          return this; }
        public Builder tier(SkillTier t)                   { this.tier = t;          return this; }
        public Builder branch(SkillBranch b)               { this.branch = b;        return this; }
        public Builder cost(int c)                         { this.pointCost = c;     return this; }
        public Builder pos(int x, int y)                   { this.gridX = x; this.gridY = y; return this; }
        public Builder requires(String... ids)             { prerequisites.addAll(Arrays.asList(ids)); return this; }
        public Builder excludes(String... ids)             { exclusions.addAll(Arrays.asList(ids)); return this; }
        public Builder effect(String e)                    { this.effectId = e;      return this; }
        public SkillNode build()                           { return new SkillNode(this); }
    }
}
