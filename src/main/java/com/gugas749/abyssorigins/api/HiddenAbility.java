package com.gugas749.abyssorigins.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * A power outside normal progression: never part of an origin / class kit and never taught by a
 * skill node. Players only get one through a staff grant (command or profile GUI). It binds into
 * an origin slot, so it counts against the same slot cap, but it works
 * with or without an origin, costs no resource and ignores origin / class cooldown hooks.
 */
public abstract class HiddenAbility extends Ability {

    protected HiddenAbility(String id, String nameKey, String descKey, int cooldownSeconds) {
        super(id, nameKey, descKey, cooldownSeconds, 0);
    }

    /**
     * A follow-up press while the power is already in effect (close the portal, leave the pocket,
     * step in or out of the punishment). Runs before the cooldown check and never consumes one.
     * @return true if the press was handled here and activate() must not run.
     */
    public boolean recast(ServerPlayer player) { return false; }
}
