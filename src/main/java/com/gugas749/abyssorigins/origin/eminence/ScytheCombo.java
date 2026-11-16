package com.gugas749.abyssorigins.origin.eminence;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Sovereign Scythe's three-swing chain: Reaping Sweep -> Sovereign Strike -> Harvest, then around again.
 * A pause longer than {@link #WINDOW} starts over at the sweep.
 *
 * Server and every client count swings the same way - a fresh main-hand swing is seen the first tick after
 * {@code swing()} ({@code swinging && swingTime == 0}) - so the stage the server deals damage for is the one
 * every client animates, without a packet. A melee hit reaches the server just before its swing, so damage
 * code reads {@link #peek} (the swing about to be counted); animation code reads {@link #stage}.
 */
public final class ScytheCombo {

    public static final int SWEEP = 0, STRIKE = 1, HARVEST = 2;
    /** Ticks allowed between swings to keep the chain going. */
    public static final int WINDOW = 30;

    private static final class State {
        long last = Long.MIN_VALUE / 2;
        int stage = HARVEST;
    }

    private static final Map<LivingEntity, State> SERVER = new WeakHashMap<>(), CLIENT = new WeakHashMap<>();

    private ScytheCombo() {}

    private static Map<LivingEntity, State> side(LivingEntity e) {
        return e.level().isClientSide() ? CLIENT : SERVER;
    }

    /** True on the one tick a new main-hand swing becomes visible. */
    public static boolean swingStarted(LivingEntity e) {
        return e.swinging && e.swingTime == 0 && e.swingingArm == InteractionHand.MAIN_HAND;
    }

    /** The stage the next swing will be. */
    public static int peek(LivingEntity e) {
        State s = side(e).get(e);
        long now = e.level().getGameTime();
        if (s == null || now - s.last > WINDOW) return SWEEP;
        return (s.stage + 1) % 3;
    }

    /** Counts a swing; returns its stage. */
    public static int advance(LivingEntity e) {
        int stage = peek(e);
        State s = side(e).computeIfAbsent(e, k -> new State());
        s.stage = stage;
        s.last = e.level().getGameTime();
        return stage;
    }

    /** Stage of the latest swing. */
    public static int stage(LivingEntity e) {
        State s = side(e).get(e);
        return s == null ? SWEEP : s.stage;
    }

    /** Game time the latest swing started (very old if never). */
    public static long started(LivingEntity e) {
        State s = side(e).get(e);
        return s == null ? Long.MIN_VALUE / 2 : s.last;
    }
}
