package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class Origin {

    /** A short strength / weakness line shown on the Origin Codex screen. */
    public record Trait(Component text, boolean positive) {}

    private final ResourceLocation id;
    private final Component        displayName;
    private final Component        description;
    private final ImpactLevel      impactLevel;
    private final List<OriginAbility> abilities = new ArrayList<>();
    private final List<OriginAbility> unlockable = new ArrayList<>();
    private final List<OriginPassive> passives  = new ArrayList<>();
    private final List<StatBonus>     baseStats = new ArrayList<>();
    private final List<Trait>         traits    = new ArrayList<>();
    private ResourceType resourceType = null;
    private PowerHooks   hooks        = PowerHooks.NONE;

    private float healthModifier = 1.0f;
    private float speedModifier  = 1.0f;
    private float damageModifier = 1.0f;

    private boolean hidden = false;
    private int    themeColor = 0xFFB9A7FF;
    private String sigil      = "◆";

    /**
     * @param nameKey  Lang key — e.g. "abyssorigins.origin.human.name"
     * @param descKey  Lang key — e.g. "abyssorigins.origin.human.description"
     */
    public Origin(ResourceLocation id, String nameKey, String descKey, ImpactLevel impactLevel) {
        this.id          = id;
        this.displayName = Component.translatable(nameKey);
        this.description = Component.translatable(descKey);
        this.impactLevel = impactLevel;
    }

    public Origin setResourceType(ResourceType type)  { this.resourceType = type; return this; }
    public Origin setHealthModifier(float v)          { this.healthModifier = v;  return this; }
    public Origin setSpeedModifier(float v)           { this.speedModifier = v;   return this; }
    public Origin setDamageModifier(float v)          { this.damageModifier = v;  return this; }
    /** Part of the default kit: known from the start and laid into the first free slots. */
    public Origin addAbility(OriginAbility ability)   { abilities.add(ability);   return this; }
    /**
     * A power this origin owns but does not start with — learned through a skill node
     * ({@code SkillNode.Builder.grants}) or granted by staff, then bound in the Abilities tab.
     */
    public Origin addUnlockableAbility(OriginAbility ability) { unlockable.add(ability); return this; }
    public Origin addPassive(OriginPassive passive)   { passives.add(passive);    return this; }
    public Origin addStat(StatBonus bonus)            { baseStats.add(bonus);     return this; }
    public Origin setHooks(PowerHooks hooks)          { this.hooks = hooks;       return this; }
    public Origin setThemeColor(int argb)             { this.themeColor = argb;   return this; }
    public Origin setSigil(String glyph)              { this.sigil = glyph;       return this; }
    /** Hidden origins are left out of listings and suggestions; they can only be granted by exact id. */
    public Origin setHidden(boolean hidden)           { this.hidden = hidden;     return this; }
    /** @param langKey trait text; positive = strength (green), otherwise weakness (red). */
    public Origin addTrait(String langKey, boolean positive) {
        traits.add(new Trait(Component.translatable(langKey), positive)); return this;
    }

    public ResourceLocation      getId()           { return id; }
    public String                getPath()         { return id.getPath(); }
    public Component             getDisplayName()  { return displayName; }
    public Component             getDescription()  { return description; }
    public ImpactLevel           getImpactLevel()  { return impactLevel; }
    public ResourceType          getResourceType() { return resourceType; }
    public float                 getHealthModifier(){ return healthModifier; }
    public float                 getSpeedModifier() { return speedModifier; }
    public float                 getDamageModifier(){ return damageModifier; }
    public PowerHooks            getHooks()        { return hooks; }
    public int                   getThemeColor()   { return themeColor; }
    public String                getSigil()        { return sigil; }
    public boolean               isHidden()        { return hidden; }
    /** The default kit (see {@link #addAbility}). */
    public List<OriginAbility>   getAbilities()    { return Collections.unmodifiableList(abilities); }
    public List<OriginAbility>   getUnlockableAbilities() { return Collections.unmodifiableList(unlockable); }
    public List<OriginPassive>   getPassives()     { return Collections.unmodifiableList(passives); }
    public List<StatBonus>       getBaseStats()    { return Collections.unmodifiableList(baseStats); }
    public List<Trait>           getTraits()       { return Collections.unmodifiableList(traits); }
}
