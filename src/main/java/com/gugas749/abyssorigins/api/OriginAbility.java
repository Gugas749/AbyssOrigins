package com.gugas749.abyssorigins.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class OriginAbility {

    private final String id;
    private final String displayName;
    private final String description;
    private final int baseCooldownTicks;
    private final int resourceCost;

    public OriginAbility(String id, String displayName, String description,
                         int cooldownSeconds, int resourceCost) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.baseCooldownTicks = cooldownSeconds * 20;
        this.resourceCost = resourceCost;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getBaseCooldownTicks() { return baseCooldownTicks; }
    public int getResourceCost()   { return resourceCost; }

    /** Server-side activation. Called after canActivate() passes. */
    public abstract void activate(Player player, Level level);

    /** Whether the player currently meets all conditions to use this ability. */
    public abstract boolean canActivate(Player player);
}
