package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared base for every activatable power — origin abilities and class abilities both
 * extend this. Ids must be globally unique: they key the AbilityRegistry, loadouts and cooldowns.
 *
 * Activation flow lives in AbilityController: cooldown check → resource check →
 * canActivate() → activate() → resource + cooldown are consumed only if activate()
 * returned true. Abilities that also implement {@link Sustained} can be toggled off
 * early by pressing the key again, even while on cooldown.
 */
public abstract class Ability {

    private final String    id;
    private final Component displayName;
    private final Component description;
    private final int       baseCooldownTicks;
    private final int       resourceCost;

    private String icon  = "✦";
    private int    color = 0xFFE6E1F5;

    protected Ability(String id, String nameKey, String descKey, int cooldownSeconds, int resourceCost) {
        this.id                = id;
        this.displayName       = Component.translatable(nameKey);
        this.description       = Component.translatable(descKey);
        this.baseCooldownTicks = cooldownSeconds * 20;
        this.resourceCost      = resourceCost;
    }

    // ---- Presentation ----------------------------------------------------

    /** Single BMP glyph shown in the HUD slot and info screen. */
    public Ability icon(String glyph) { this.icon = glyph; return this; }
    /** ARGB accent colour for the HUD slot. */
    public Ability color(int argb)    { this.color = argb; return this; }

    public String    getId()               { return id; }
    public Component getDisplayName()      { return displayName; }
    public Component getDescription()      { return description; }
    public int       getBaseCooldownTicks(){ return baseCooldownTicks; }
    public int       getResourceCost()     { return resourceCost; }
    public String    getIcon()             { return icon; }
    public int       getColor()            { return color; }

    // ---- Behaviour (server side only) -------------------------------------

    /** Effective resource cost — override to let skills discount it. */
    public int resourceCost(ServerPlayer player) { return resourceCost; }

    /** Effective cooldown before hook multipliers — override for skill-based reductions. */
    public int cooldownTicks(ServerPlayer player) { return baseCooldownTicks; }

    public boolean canActivate(ServerPlayer player) { return true; }

    /**
     * Perform the ability.
     * @return false to abort — no cooldown or resource is consumed (e.g. no target found).
     */
    public abstract boolean activate(ServerPlayer player);
}
