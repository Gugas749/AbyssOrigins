package com.gugas749.abyssorigins.skilltree.data;

import net.minecraft.network.chat.Component;

import java.util.*;

public class SkillNode {

    // ---- Enums ----------------------------------------------------------

    public enum SkillBranch {
        CORE, OFFENSE, DEFENSE, UTILITY,
        ELEMENTAL_A, ELEMENTAL_B,
        HYBRID, SPECIALIST,
        FORBIDDEN, ASCENDED,
        SYNERGY_AD, SYNERGY_AU, SYNERGY_DU
    }

    public enum SkillTier {
        TIER_1(1, 0), TIER_2(2, 5), TIER_3(3, 15), TIER_4(4, 25), TIER_5(5, 40);

        private final int tier;
        private final int levelRequired;

        SkillTier(int tier, int levelRequired) { this.tier = tier; this.levelRequired = levelRequired; }

        public int getTier()          { return tier; }
        public int getLevelRequired() { return levelRequired; }
    }

    // ---- Fields ---------------------------------------------------------

    private final String    id;
    private final Component displayName;
    private final Component description;
    private final String    icon;
    private final SkillTier  tier;
    private final SkillBranch branch;
    private final int       pointCost;
    private final int       gridX;
    private final int       gridY;
    private final List<String> prerequisites;
    private final List<String> exclusions;
    private final String    effectId;

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

    public String    getId()            { return id; }
    public Component getDisplayName()   { return displayName; }
    public Component getDescription()   { return description; }
    public String    getIcon()          { return icon; }
    public SkillTier  getTier()          { return tier; }
    public SkillBranch getBranch()       { return branch; }
    public int       getPointCost()     { return pointCost; }
    public int       getGridX()         { return gridX; }
    public int       getGridY()         { return gridY; }
    public int       getGridCol()       { return gridX; }
    public int       getGridRow()       { return gridY; }
    public List<String> getPrereqs()    { return prerequisites; }
    public List<String> getPrerequisites() { return prerequisites; }
    public List<String> getExclusions() { return exclusions; }
    public String    getEffectId()      { return effectId; }

    // ---- Logic ----------------------------------------------------------

    public boolean canUnlock(Set<String> unlocked, Set<String> excluded, int playerLevel) {
        if (playerLevel < tier.getLevelRequired()) return false;
        if (excluded.contains(id)) return false;
        for (String req : prerequisites) if (!unlocked.contains(req)) return false;
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
        private Component displayName   = Component.literal("Unnamed");
        private Component description   = Component.literal("");
        private String    icon          = "\u2605";
        private SkillTier  tier          = SkillTier.TIER_1;
        private SkillBranch branch       = SkillBranch.CORE;
        private int       pointCost     = 1;
        private int       gridX         = 0;
        private int       gridY         = 0;
        private final List<String> prerequisites = new ArrayList<>();
        private final List<String> exclusions    = new ArrayList<>();
        private String    effectId      = "";

        public Builder(String id)                          { this.id = id; }

        /** Pass a lang key — will be wrapped in Component.translatable(). */
        public Builder name(String langKey)                { this.displayName = Component.translatable(langKey); return this; }
        /** Pass a lang key — will be wrapped in Component.translatable(). */
        public Builder desc(String langKey)                { this.description = Component.translatable(langKey); return this; }
        /** Pass a Component directly (e.g. Component.literal for hardcoded text). */
        public Builder nameComponent(Component c)          { this.displayName = c; return this; }
        public Builder descComponent(Component c)          { this.description = c; return this; }

        public Builder icon(String i)                      { this.icon = i;          return this; }
        public Builder tier(SkillTier t)                   { this.tier = t;          return this; }
        public Builder branch(SkillBranch b)               { this.branch = b;        return this; }
        public Builder cost(int c)                         { this.pointCost = c;     return this; }
        public Builder pos(int x, int y)                   { this.gridX = x; this.gridY = y; return this; }
        public Builder requires(String... ids)             { prerequisites.addAll(Arrays.asList(ids)); return this; }
        public Builder excludes(String... ids)             { exclusions.addAll(Arrays.asList(ids));    return this; }
        public Builder effect(String e)                    { this.effectId = e;      return this; }
        public SkillNode build()                           { return new SkillNode(this); }
    }
}
