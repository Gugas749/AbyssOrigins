package com.gugas749.abyssorigins.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Implemented by abilities that stay active for a duration (Bat Form, Occult Vision,
 * Shadow Meld...). The state itself is tracked by AbilityStates, synced to the
 * owning client for HUD timers, and always ended cleanly on death, logout or
 * origin/class change.
 */
public interface Sustained {

    /** Called every server tick while active. {@code remaining} counts down towards 0. */
    default void tickActive(ServerPlayer player, int remaining) {}

    /** Called exactly once when the state ends for any reason. */
    default void onEnd(ServerPlayer player) {}
}
