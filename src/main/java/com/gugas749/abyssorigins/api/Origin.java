package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class Origin {

    private final ResourceLocation id;
    private final Component        displayName;
    private final Component        description;
    private final ImpactLevel      impactLevel;
    private final List<OriginAbility> abilities = new ArrayList<>();
    private final List<OriginPassive> passives  = new ArrayList<>();
    private ResourceType resourceType = null;

    private float healthModifier = 1.0f;
    private float speedModifier  = 1.0f;
    private float damageModifier = 1.0f;

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
    public Origin addAbility(OriginAbility ability)   { abilities.add(ability);   return this; }
    public Origin addPassive(OriginPassive passive)   { passives.add(passive);    return this; }

    public ResourceLocation      getId()           { return id; }
    public Component             getDisplayName()  { return displayName; }
    public Component             getDescription()  { return description; }
    public ImpactLevel           getImpactLevel()  { return impactLevel; }
    public ResourceType          getResourceType() { return resourceType; }
    public float                 getHealthModifier(){ return healthModifier; }
    public float                 getSpeedModifier() { return speedModifier; }
    public float                 getDamageModifier(){ return damageModifier; }
    public List<OriginAbility>   getAbilities()    { return Collections.unmodifiableList(abilities); }
    public List<OriginPassive>   getPassives()     { return Collections.unmodifiableList(passives); }
}
