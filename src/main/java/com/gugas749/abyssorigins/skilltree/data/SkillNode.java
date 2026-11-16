package com.gugas749.abyssorigins.skilltree.data;

import com.gugas749.abyssorigins.api.StatBonus;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.*;

public class SkillNode {

    // ---- Enums ----------------------------------------------------------

    public enum SkillBranch {
        CORE, OFFENSE, DEFENSE, UTILITY,
        ELEMENTAL_A, ELEMENTAL_B,
        HYBRID, SPECIALIST,
        FORBIDDEN, ASCENDED,
        SYNERGY_AD
    }

    /**
     * Tier gates. Levels are deliberately reachable: origin XP comes from kills and
     * advancements (see XpEventHandler), so tier 5 lands around level 15. Tier 6 (Apex)
     * is the late-game row past the capstones, where trees grow new selectable powers.
     * Tiers 7 and 8 are the ascension rows, sealed until the origin's Noble (level 25) and
     * Lord (level 50) stages ({@link com.gugas749.abyssorigins.api.Ascension}).
     */
    public enum SkillTier {
        TIER_1(1, 0), TIER_2(2, 3), TIER_3(3, 6), TIER_4(4, 10), TIER_5(5, 15), TIER_6(6, 20),
        TIER_7(7, 25), TIER_8(8, 50);

        private final int tier;
        private final int levelRequired;

        SkillTier(int tier, int levelRequired) { this.tier = tier; this.levelRequired = levelRequired; }

        public int getTier()          { return tier; }
        public int getLevelRequired() { return levelRequired; }
        /** The ascension stage that opens this tier (1 Noble, 2 Lord), or 0 for the ordinary tiers. */
        public int stage()            { return this == TIER_8 ? 2 : this == TIER_7 ? 1 : 0; }
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
    private final List<String> anyPrerequisites;
    private final List<String> exclusions;
    private final List<StatBonus> stats;
    private final String    grantedAbility;
    private final double    cooldownReduction;

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
        this.prerequisites    = Collections.unmodifiableList(b.prerequisites);
        this.anyPrerequisites = Collections.unmodifiableList(b.anyPrerequisites);
        this.exclusions   = Collections.unmodifiableList(b.exclusions);
        this.stats        = Collections.unmodifiableList(b.stats);
        this.grantedAbility = b.grantedAbility;
        this.cooldownReduction = b.cooldownReduction;
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
    public List<String> getPrereqs()    { return prerequisites; }
    public List<String> getPrerequisites() { return prerequisites; }
    /** At least one of these must be unlocked (OR-gate). Empty = no OR requirement. */
    public List<String> getAnyPrerequisites() { return anyPrerequisites; }
    public List<String> getExclusions() { return exclusions; }
    public List<StatBonus> getStats()   { return stats; }
    /** Ability id this node teaches (it then appears in the Abilities tab), or "" for none. */
    public String    getGrantedAbility(){ return grantedAbility; }
    public boolean   grantsAbility()    { return !grantedAbility.isEmpty(); }
    /** Fraction taken off the cooldown of every power while unlocked (0.1 = 10% shorter), or 0. */
    public double    getCooldownReduction() { return cooldownReduction; }
    /** Capstones (tier 5) and the Apex row beyond them use the larger node style. */
    public boolean   isCapstone()       { return tier.getTier() >= SkillTier.TIER_5.getTier(); }

    // ---- Logic ----------------------------------------------------------

    public boolean prerequisitesMet(Set<String> unlocked) {
        for (String req : prerequisites) if (!unlocked.contains(req)) return false;
        if (!anyPrerequisites.isEmpty() && anyPrerequisites.stream().noneMatch(unlocked::contains)) return false;
        return true;
    }

    /** Branch colours. */
    public int getBranchColor() { return colorOf(branch); }

    public static int colorOf(SkillBranch branch) {
        return switch (branch) {
            case CORE        -> 0xFFC9C3E6;
            case OFFENSE     -> 0xFFE0566B;
            case DEFENSE     -> 0xFF5B8DEF;
            case UTILITY     -> 0xFF5FD3A6;
            case ELEMENTAL_A -> 0xFFF08A3C;
            case ELEMENTAL_B -> 0xFF4FC3E8;
            case HYBRID      -> 0xFFB06BE8;
            case SPECIALIST  -> 0xFFE8D36B;
            case FORBIDDEN   -> 0xFFB3122E;
            case ASCENDED    -> 0xFFFFF1B8;
            case SYNERGY_AD  -> 0xFFE070D8;
        };
    }

    public int getTierColor() { return tierColor(tier); }

    public static int tierColor(SkillTier tier) {
        return switch (tier) {
            case TIER_1 -> 0xFF6E6889;
            case TIER_2 -> 0xFF5FB58A;
            case TIER_3 -> 0xFF5B7FE0;
            case TIER_4 -> 0xFFA86BE8;
            case TIER_5 -> 0xFFF2C14E;
            case TIER_6 -> 0xFFFFF1B8;
            case TIER_7 -> 0xFFFF9A6B;
            case TIER_8 -> 0xFFFF5C7A;
        };
    }

    // ---- Builder --------------------------------------------------------

    public static Builder builder(String id) { return new Builder(id); }

    public static class Builder {
        private final String id;
        private Component displayName   = Component.literal("Unnamed");
        private Component description   = Component.literal("");
        private String    icon          = "★";
        private SkillTier  tier          = SkillTier.TIER_1;
        private SkillBranch branch       = SkillBranch.CORE;
        private int       pointCost     = 1;
        private int       gridX         = 0;
        private int       gridY         = 0;
        private final List<String> prerequisites    = new ArrayList<>();
        private final List<String> anyPrerequisites = new ArrayList<>();
        private final List<String> exclusions       = new ArrayList<>();
        private final List<StatBonus> stats         = new ArrayList<>();
        private String    grantedAbility = "";
        private double    cooldownReduction = 0;

        public Builder(String id)                          { this.id = id; }

        /** Pass a lang key - will be wrapped in Component.translatable(). */
        public Builder name(String langKey)                { this.displayName = Component.translatable(langKey); return this; }
        /** Pass a lang key - will be wrapped in Component.translatable(). */
        public Builder desc(String langKey)                { this.description = Component.translatable(langKey); return this; }
        /** Shorthand for name(prefix + ".name").desc(prefix + ".desc"). */
        public Builder lang(String prefix)                 { return name(prefix + ".name").desc(prefix + ".desc"); }
        /** Pass a Component directly (e.g. Component.literal for hardcoded text). */

        public Builder icon(String i)                      { this.icon = i;          return this; }
        public Builder tier(SkillTier t)                   { this.tier = t;          return this; }
        public Builder branch(SkillBranch b)               { this.branch = b;        return this; }
        public Builder cost(int c)                         { this.pointCost = c;     return this; }
        public Builder pos(int x, int y)                   { this.gridX = x; this.gridY = y; return this; }
        public Builder requires(String... ids)             { prerequisites.addAll(Arrays.asList(ids)); return this; }
        /** OR-gate: at least one of the given nodes must be unlocked. */
        public Builder requiresAny(String... ids)          { anyPrerequisites.addAll(Arrays.asList(ids)); return this; }
        public Builder excludes(String... ids)             { exclusions.addAll(Arrays.asList(ids));    return this; }
        /**
         * Unlocking this node teaches an ability (by id). Register the ability with the tree's
         * owner via addUnlockableAbility(), then the player binds it in the Abilities tab.
         */
        public Builder grants(String abilityId)            { this.grantedAbility = abilityId; return this; }
        /** Flat attribute bonus applied while the node is unlocked. */
        public Builder stat(Holder<Attribute> attr, double amount)    { stats.add(StatBonus.add(attr, amount));     return this; }
        /** Percentage attribute bonus (of base value) applied while unlocked. */
        public Builder statPct(Holder<Attribute> attr, double amount) { stats.add(StatBonus.percent(attr, amount)); return this; }
        /** Every power's cooldown is {@code fraction} shorter while unlocked (stacks additively, see Skills.cooldownFactor). */
        public Builder cooldown(double fraction)                       { this.cooldownReduction = fraction; return this; }
        public SkillNode build()                           { return new SkillNode(this); }
    }
}
