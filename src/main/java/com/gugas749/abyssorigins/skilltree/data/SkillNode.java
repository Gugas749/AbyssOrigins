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
        SYNERGY_AD, SYNERGY_AU, SYNERGY_DU
    }

    /**
     * Tier gates. Levels are deliberately reachable: origin XP comes from kills and
     * advancements (see XpEventHandler), so tier 5 lands around level 15. Tier 6 (Apex)
     * is the late-game row past the capstones, where trees grow new selectable powers.
     */
    public enum SkillTier {
        TIER_1(1, 0), TIER_2(2, 3), TIER_3(3, 6), TIER_4(4, 10), TIER_5(5, 15), TIER_6(6, 20);

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
    private final List<String> anyPrerequisites;
    private final List<String> exclusions;
    private final List<StatBonus> stats;
    private final String    effectId;
    private final String    grantedAbility;

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
        this.effectId     = b.effectId;
        this.grantedAbility = b.grantedAbility;
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
    /** At least one of these must be unlocked (OR-gate). Empty = no OR requirement. */
    public List<String> getAnyPrerequisites() { return anyPrerequisites; }
    public List<String> getExclusions() { return exclusions; }
    public List<StatBonus> getStats()   { return stats; }
    public String    getEffectId()      { return effectId; }
    /** Ability id this node teaches (it then appears in the Abilities tab), or "" for none. */
    public String    getGrantedAbility(){ return grantedAbility; }
    public boolean   grantsAbility()    { return !grantedAbility.isEmpty(); }
    /** Capstones (tier 5) and the Apex row beyond them get the grand treatment. */
    public boolean   isCapstone()       { return tier.getTier() >= SkillTier.TIER_5.getTier(); }

    // ---- Logic ----------------------------------------------------------

    public boolean prerequisitesMet(Set<String> unlocked) {
        for (String req : prerequisites) if (!unlocked.contains(req)) return false;
        if (!anyPrerequisites.isEmpty() && anyPrerequisites.stream().noneMatch(unlocked::contains)) return false;
        return true;
    }

    public boolean canUnlock(Set<String> unlocked, Set<String> excluded, int playerLevel) {
        if (playerLevel < tier.getLevelRequired()) return false;
        if (excluded.contains(id)) return false;
        return prerequisitesMet(unlocked);
    }

    /** Abyss palette — each branch reads as a distinct "school". */
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
            case SYNERGY_AU  -> 0xFFE8A86B;
            case SYNERGY_DU  -> 0xFF6BE8E0;
        };
    }

    public int getTierColor() {
        return switch (tier) {
            case TIER_1 -> 0xFF6E6889;
            case TIER_2 -> 0xFF5FB58A;
            case TIER_3 -> 0xFF5B7FE0;
            case TIER_4 -> 0xFFA86BE8;
            case TIER_5 -> 0xFFF2C14E;
            case TIER_6 -> 0xFFFFF1B8;
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
        private String    effectId      = "";
        private String    grantedAbility = "";

        public Builder(String id)                          { this.id = id; this.effectId = id; }

        /** Pass a lang key — will be wrapped in Component.translatable(). */
        public Builder name(String langKey)                { this.displayName = Component.translatable(langKey); return this; }
        /** Pass a lang key — will be wrapped in Component.translatable(). */
        public Builder desc(String langKey)                { this.description = Component.translatable(langKey); return this; }
        /** Shorthand for name(prefix + ".name").desc(prefix + ".desc"). */
        public Builder lang(String prefix)                 { return name(prefix + ".name").desc(prefix + ".desc"); }
        /** Pass a Component directly (e.g. Component.literal for hardcoded text). */
        public Builder nameComponent(Component c)          { this.displayName = c; return this; }
        public Builder descComponent(Component c)          { this.description = c; return this; }

        public Builder icon(String i)                      { this.icon = i;          return this; }
        public Builder tier(SkillTier t)                   { this.tier = t;          return this; }
        public Builder branch(SkillBranch b)               { this.branch = b;        return this; }
        public Builder cost(int c)                         { this.pointCost = c;     return this; }
        public Builder pos(int x, int y)                   { this.gridX = x; this.gridY = y; return this; }
        public Builder requires(String... ids)             { prerequisites.addAll(Arrays.asList(ids)); return this; }
        /** OR-gate: at least one of the given nodes must be unlocked. */
        public Builder requiresAny(String... ids)          { anyPrerequisites.addAll(Arrays.asList(ids)); return this; }
        public Builder excludes(String... ids)             { exclusions.addAll(Arrays.asList(ids));    return this; }
        public Builder effect(String e)                    { this.effectId = e;      return this; }
        /**
         * Unlocking this node teaches an ability (by id). Register the ability with the tree's
         * owner via addUnlockableAbility(), then the player binds it in the Abilities tab.
         */
        public Builder grants(String abilityId)            { this.grantedAbility = abilityId; return this; }
        /** Flat attribute bonus applied while the node is unlocked. */
        public Builder stat(Holder<Attribute> attr, double amount)    { stats.add(StatBonus.add(attr, amount));     return this; }
        /** Percentage attribute bonus (of base value) applied while unlocked. */
        public Builder statPct(Holder<Attribute> attr, double amount) { stats.add(StatBonus.percent(attr, amount)); return this; }
        public SkillNode build()                           { return new SkillNode(this); }
    }
}
