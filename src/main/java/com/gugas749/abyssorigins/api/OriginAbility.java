package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class OriginAbility {

    private final String    id;
    private final Component displayName;
    private final Component description;
    private final int       baseCooldownTicks;
    private final int       resourceCost;

    /**
     * @param nameKey  Lang key for the ability name
     * @param descKey  Lang key for the ability description
     */
    public OriginAbility(String id, String nameKey, String descKey,
                         int cooldownSeconds, int resourceCost) {
        this.id               = id;
        this.displayName      = Component.translatable(nameKey);
        this.description      = Component.translatable(descKey);
        this.baseCooldownTicks = cooldownSeconds * 20;
        this.resourceCost     = resourceCost;
    }

    public String    getId()               { return id; }
    public Component getDisplayName()      { return displayName; }
    public Component getDescription()      { return description; }
    public int       getBaseCooldownTicks(){ return baseCooldownTicks; }
    public int       getResourceCost()     { return resourceCost; }

    public abstract void    activate(Player player, Level level);
    public abstract boolean canActivate(Player player);
}
