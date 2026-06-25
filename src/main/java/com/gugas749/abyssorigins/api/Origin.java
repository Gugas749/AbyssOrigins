package com.gugas749.abyssorigins.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Origin {

    private final ResourceLocation id;
    private final String displayName;
    private final String description;
    private final ImpactLevel impactLevel;
    private final List<OriginAbility> abilities = new ArrayList<>();
    private final List<OriginPassive> passives  = new ArrayList<>();
    private ResourceType resourceType = null;

    // Stat multipliers — 1.0 = vanilla baseline
    private float healthModifier = 1.0f;
    private float speedModifier  = 1.0f;
    private float damageModifier = 1.0f;

    public Origin(ResourceLocation id, String displayName, String description,
                  ImpactLevel impactLevel) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.impactLevel = impactLevel;
    }

    // ---- Mutators -------------------------------------------------------

    public Origin setResourceType(ResourceType type)   { this.resourceType = type; return this; }
    public Origin setHealthModifier(float v)           { this.healthModifier = v;  return this; }
    public Origin setSpeedModifier(float v)            { this.speedModifier = v;   return this; }
    public Origin setDamageModifier(float v)           { this.damageModifier = v;  return this; }
    public Origin addAbility(OriginAbility ability)    { abilities.add(ability);   return this; }
    public Origin addPassive(OriginPassive passive)    { passives.add(passive);    return this; }

    // ---- Accessors -------------------------------------------------------

    public ResourceLocation getId()        { return id; }
    public String getDisplayName()         { return displayName; }
    public String getDescription()         { return description; }
    public ImpactLevel getImpactLevel()    { return impactLevel; }
    public ResourceType getResourceType()  { return resourceType; }
    public float getHealthModifier()       { return healthModifier; }
    public float getSpeedModifier()        { return speedModifier; }
    public float getDamageModifier()       { return damageModifier; }
    public List<OriginAbility> getAbilities() { return Collections.unmodifiableList(abilities); }
    public List<OriginPassive> getPassives()  { return Collections.unmodifiableList(passives); }
}
