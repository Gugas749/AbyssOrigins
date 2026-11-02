package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.network.packet.SyncTimersPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client mirror of the local player's active sustained abilities (AbilityStates).
 * Drives HUD "active" slot glow and client-only visuals such as Occult Vision.
 */
public final class ClientTimers {

    private static final Map<String, int[]> ACTIVE = new HashMap<>(); // id -> {remaining, max}

    private ClientTimers() {}

    public static void replace(List<SyncTimersPacket.Entry> entries) {
        ACTIVE.clear();
        for (SyncTimersPacket.Entry e : entries) ACTIVE.put(e.id(), new int[]{ e.remaining(), Math.max(1, e.max()) });
    }

    public static void tick() {
        ACTIVE.values().forEach(v -> { if (v[0] > 0) v[0]--; });
    }

    public static boolean isActive(String id)  { return ACTIVE.containsKey(id); }
    public static int remaining(String id)     { int[] v = ACTIVE.get(id); return v == null ? 0 : v[0]; }

    /** 1.0 at start → 0.0 at expiry. Indefinite states report 1. */
    public static float fraction(String id) {
        int[] v = ACTIVE.get(id);
        if (v == null) return 0f;
        if (v[1] > 1_000_000) return 1f;
        return Math.max(0f, Math.min(1f, v[0] / (float) v[1]));
    }

    public static void clear() { ACTIVE.clear(); }
}
