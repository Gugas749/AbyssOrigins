package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public abstract class SpecializationPassive {

    private final String    id;
    private final Component displayName;
    private final Component description;

    public SpecializationPassive(String id, String nameKey, String descKey) {
        this.id          = id;
        this.displayName = Component.translatable(nameKey);
        this.description = Component.translatable(descKey);
    }

    public String    getId()          { return id; }
    public Component getDisplayName() { return displayName; }
    public Component getDescription() { return description; }

    public abstract void onTick(Player player);
    public void onEquip(Player player)  {}
    public void onRemove(Player player) {}
}
