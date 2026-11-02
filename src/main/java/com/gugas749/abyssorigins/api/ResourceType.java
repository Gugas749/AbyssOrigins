package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;

/**
 * Defines a custom resource bar for an origin (always 0-100).
 * Null resourceType = no bar (e.g. Human uses vanilla hunger/health only).
 *
 * Fixed drain/regen happens in OriginEventHandler. Conditional gain (sunlight,
 * darkness, combat...) is implemented by the owning origin's passives/hooks.
 */
public class ResourceType {

    private final String id;
    private final Component displayName;
    private final int maxAmount;
    /** Passive drain per tick (0 = no drain). */
    private final float drainPerTick;
    /** Passive regen per tick (0 = no regen). */
    private final float regenPerTick;
    /** ARGB color for the HUD bar. */
    private final int barColor;
    /** Value the resource is set to when the origin is first assigned. */
    private final float startingAmount;

    public ResourceType(String id, Component displayName, int maxAmount,
                        float drainPerTick, float regenPerTick, int barColor) {
        this(id, displayName, maxAmount, drainPerTick, regenPerTick, barColor, maxAmount);
    }

    public ResourceType(String id, Component displayName, int maxAmount,
                        float drainPerTick, float regenPerTick, int barColor, float startingAmount) {
        this.id = id;
        this.displayName = displayName;
        this.maxAmount = maxAmount;
        this.drainPerTick = drainPerTick;
        this.regenPerTick = regenPerTick;
        this.barColor = barColor;
        this.startingAmount = startingAmount;
    }

    public String getId()            { return id; }
    public Component getDisplayName(){ return displayName; }
    public int getMaxAmount()        { return maxAmount; }
    public float getDrainPerTick()   { return drainPerTick; }
    public float getRegenPerTick()   { return regenPerTick; }
    public int getBarColor()         { return barColor; }
    public float getStartingAmount() { return startingAmount; }

    // ---- Built-in factories ----------------------------------------------

    /**
     * Blood resource for vampires.
     * Drains at 0.5x the rate of vanilla hunger (hunger drains ~1 point per 80 ticks
     * at normal activity, so blood drains 1 point per 160 ticks passively).
     */
    public static ResourceType blood() {
        return new ResourceType(
            "blood",
            Component.translatable("resource.abyssorigins.blood"),
            100,
            1f / 160f,   // drain: 0.5x vanilla hunger rate
            0f,          // no passive regen — must drink blood
            0xFFAA0000   // dark red
        );
    }

    /** Demon wrath — starts empty, built by violence, smoulders away at 1 per second. */
    public static ResourceType wrath() {
        return new ResourceType("wrath", Component.translatable("resource.abyssorigins.wrath"),
            100, 1f / 20f, 0f, 0xFFFF6A1F, 0f);
    }

    /** Seraph radiance — gathered beneath the open sky (see SeraphOrigin). */
    public static ResourceType radiance() {
        return new ResourceType("radiance", Component.translatable("resource.abyssorigins.radiance"),
            100, 0f, 0f, 0xFFF6D77A);
    }

    /** Voidborn essence — seeps in constantly, faster in the End or below bedrock level. */
    public static ResourceType voidEssence() {
        return new ResourceType("void_essence", Component.translatable("resource.abyssorigins.void_essence"),
            100, 0f, 1f / 40f, 0xFF9B6BFF);
    }

    /** Eminence dominion — the weight of the crown; returns steadily (2 per second). */
    public static ResourceType dominion() {
        return new ResourceType("dominion", Component.translatable("resource.abyssorigins.dominion"),
            100, 0f, 1f / 10f, 0xFFF2C14E);
    }

    /** Shade umbra — pooled from darkness, bleeds away in bright light. */
    public static ResourceType umbra() {
        return new ResourceType("umbra", Component.translatable("resource.abyssorigins.umbra"),
            100, 0f, 0f, 0xFF5E7FB8);
    }
}
