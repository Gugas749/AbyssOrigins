package com.gugas749.abyssorigins.power;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Transient per-player scratch storage for powers: stack counters, internal
 * (proc) cooldowns, "next hit" flags. Server-side only, cleared on logout.
 *
 * Passive and hook objects are shared singletons, so anything per-player must
 * live here rather than in a field.
 */
public final class PowerState {

    private static final Map<UUID, Map<String, Long>> STATE = new HashMap<>();

    private PowerState() {}

    private static Map<String, Long> of(Player p) {
        return STATE.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
    }

    // ---- Counters ---------------------------------------------------------

    public static long get(Player p, String key)          { return of(p).getOrDefault(key, 0L); }
    public static int  getInt(Player p, String key)       { return (int) get(p, key); }
    public static void set(Player p, String key, long v)  { of(p).put(key, v); }
    public static void clear(Player p, String key)        { of(p).remove(key); }

    // ---- Internal cooldowns (game-time based) -----------------------------

    /** True if the proc keyed {@code key} is off cooldown. */
    public static boolean ready(Player p, String key) {
        return p.level().getGameTime() >= get(p, "cd." + key);
    }

    /** Puts the proc on cooldown for {@code ticks}. */
    public static void trigger(Player p, String key, int ticks) {
        set(p, "cd." + key, p.level().getGameTime() + ticks);
    }

    /** ready() + trigger() in one call. Returns true if the proc fired. */
    public static boolean tryTrigger(Player p, String key, int ticks) {
        if (!ready(p, key)) return false;
        trigger(p, key, ticks);
        return true;
    }

    // ---- Timed flags --------------------------------------------------------

    /** Marks a flag active until {@code ticks} from now (e.g. "next hit empowered"). */
    public static void flag(Player p, String key, int ticks) {
        set(p, "flag." + key, p.level().getGameTime() + ticks);
    }

    public static boolean flagged(Player p, String key) {
        return p.level().getGameTime() < get(p, "flag." + key);
    }

    /** Returns true and clears the flag if it was active. */
    public static boolean consume(Player p, String key) {
        boolean was = flagged(p, key);
        clear(p, "flag." + key);
        return was;
    }

    public static void unload(UUID id) { STATE.remove(id); }
}
