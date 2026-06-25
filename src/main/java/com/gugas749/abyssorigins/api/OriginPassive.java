package com.gugas749.abyssorigins.api;

import net.minecraft.world.entity.player.Player;

public abstract class OriginPassive {

    private final String id;
    private final String displayName;
    private final String description;

    public OriginPassive(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /** Called every server tick while the player has this origin. */
    public abstract void onTick(Player player);

    /** Called when the origin is assigned to the player. */
    public void onEquip(Player player) {}

    /** Called when the origin is removed from the player. */
    public void onRemove(Player player) {}
}
