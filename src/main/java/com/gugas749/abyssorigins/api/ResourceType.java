package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;

/**
 * Defines a custom resource bar for an origin.
 * Null resourceType = no bar (e.g. Human uses vanilla hunger/health only).
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

    public ResourceType(String id, Component displayName, int maxAmount,
                        float drainPerTick, float regenPerTick, int barColor) {
        this.id = id;
        this.displayName = displayName;
        this.maxAmount = maxAmount;
        this.drainPerTick = drainPerTick;
        this.regenPerTick = regenPerTick;
        this.barColor = barColor;
    }

    public String getId()            { return id; }
    public Component getDisplayName(){ return displayName; }
    public int getMaxAmount()        { return maxAmount; }
    public float getDrainPerTick()   { return drainPerTick; }
    public float getRegenPerTick()   { return regenPerTick; }
    public int getBarColor()         { return barColor; }

    // ---- Built-in factory ------------------------------------------------

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
}
