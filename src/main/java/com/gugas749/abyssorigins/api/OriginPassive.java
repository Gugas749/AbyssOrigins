package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * A passive trait of an Origin, ticked every server tick.
 *
 * Passive instances are shared by every player with the origin — never keep
 * per-player state in fields. Gate periodic work on {@code player.tickCount}
 * and store per-player state in PowerState.
 */
public abstract class OriginPassive {

    private final String    id;
    private final Component displayName;
    private final Component description;
    private boolean hidden = false;

    /**
     * @param nameKey  Lang key for the passive name
     * @param descKey  Lang key for the passive description
     */
    public OriginPassive(String id, String nameKey, String descKey) {
        this.id          = id;
        this.displayName = Component.translatable(nameKey);
        this.description = Component.translatable(descKey);
    }

    /** Hidden passives are internal machinery and are not listed in the UI. */
    public OriginPassive hidden() { this.hidden = true; return this; }

    public String    getId()          { return id; }
    public Component getDisplayName() { return displayName; }
    public Component getDescription() { return description; }
    public boolean   isHidden()       { return hidden; }

    public abstract void onTick(Player player);
    public void onEquip(Player player)  {}
    public void onRemove(Player player) {}
}
