package com.gugas749.abyssorigins.item.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The soul of a relic weapon: a right-click power with its own cooldown, plus an
 * on-hit passive. Only ever invoked for an attuned wielder (GearEvents / OriginWeaponItem).
 * Lang: abyssorigins.weapon.&lt;id&gt;.power(.desc) and .passive(.desc)
 */
public abstract class WeaponPower {

    private final String id;
    private final int cooldownTicks;

    protected WeaponPower(String id, int cooldownSeconds) {
        this.id = id;
        this.cooldownTicks = cooldownSeconds * 20;
    }

    public String id()          { return id; }
    public int cooldownTicks()  { return cooldownTicks; }

    /** Right-click power. Return false to abort without starting the cooldown. */
    public abstract boolean activate(ServerPlayer player, ItemStack stack);

    /** The wielder is about to damage {@code target} in melee with this weapon. */
    public void onHit(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {}

    /** The wielder is about to take damage while holding this weapon. */
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {}
}
