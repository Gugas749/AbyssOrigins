package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.network.packet.SyncTimersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Tracks active sustained abilities (Bat Form, Occult Vision, Shadow Meld, Aegis...).
 *
 * Replaces the old persistent-NBT timers: those survived logout (leaving players
 * able to fly) and were invisible to the client. States here are always ended
 * through {@link Sustained#onEnd}, and every change is synced to the owning client
 * so the HUD and client-only visuals (Occult Vision reveal) know what is active.
 */
public final class AbilityStates {

    private static final class State {
        final Sustained handler;
        int remaining;
        final int max;
        State(Sustained handler, int ticks) { this.handler = handler; this.remaining = ticks; this.max = ticks; }
    }

    private static final Map<UUID, Map<String, State>> STATES = new HashMap<>();

    /** Duration used for "until toggled off" states. */
    public static final int INDEFINITE = Integer.MAX_VALUE / 2;

    /**
     * A timed effect that lives outside this class (it must outlast death or logout, say) but is
     * shown to its owner the same way: synced with these states, so the HUD and client visuals treat it alike.
     */
    @FunctionalInterface
    public interface TimerSource { void collect(ServerPlayer player, List<SyncTimersPacket.Entry> out); }

    private static final List<TimerSource> SOURCES = new ArrayList<>();

    /** Call during common setup. */
    public static void addTimerSource(TimerSource source) { SOURCES.add(source); }

    private AbilityStates() {}

    public static void start(ServerPlayer player, String id, int ticks, Sustained handler) {
        Map<String, State> map = STATES.computeIfAbsent(player.getUUID(), k -> new LinkedHashMap<>());
        State old = map.put(id, new State(handler, ticks));
        if (old != null) safeEnd(player, id, old);
        sync(player);
    }

    public static boolean isActive(ServerPlayer player, String id) {
        Map<String, State> map = STATES.get(player.getUUID());
        return map != null && map.containsKey(id);
    }

    public static int remaining(ServerPlayer player, String id) {
        Map<String, State> map = STATES.get(player.getUUID());
        State s = map == null ? null : map.get(id);
        return s == null ? 0 : s.remaining;
    }

    /** Ends the state now (toggle-off, interruption). No-op if not active. */
    public static void stop(ServerPlayer player, String id) {
        Map<String, State> map = STATES.get(player.getUUID());
        if (map == null) return;
        State s = map.remove(id);
        if (s != null) {
            safeEnd(player, id, s);
            sync(player);
        }
    }

    /** Ends everything — death, logout, origin or class change. */
    public static void stopAll(ServerPlayer player) {
        Map<String, State> map = STATES.remove(player.getUUID());
        if (map == null || map.isEmpty()) return;
        map.forEach((id, s) -> safeEnd(player, id, s));
        sync(player);
    }

    /** Called every server tick for each player. */
    public static void tick(ServerPlayer player) {
        Map<String, State> map = STATES.get(player.getUUID());
        if (map == null || map.isEmpty()) return;

        boolean changed = false;
        Iterator<Map.Entry<String, State>> it = map.entrySet().iterator();
        List<Map.Entry<String, State>> ended = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, State> e = it.next();
            State s = e.getValue();
            s.remaining--;
            try { s.handler.tickActive(player, s.remaining); }
            catch (Exception ex) { Abyssorigins.LOGGER.error("Sustained tick error {}: {}", e.getKey(), ex.getMessage()); }
            if (s.remaining <= 0) { it.remove(); ended.add(e); changed = true; }
        }
        // onEnd may start new states, so call it after iteration
        for (Map.Entry<String, State> e : ended) safeEnd(player, e.getKey(), e.getValue());
        if (changed) sync(player);
    }

    private static void safeEnd(ServerPlayer player, String id, State s) {
        try { s.handler.onEnd(player); }
        catch (Exception ex) { Abyssorigins.LOGGER.error("Sustained end error {}: {}", id, ex.getMessage()); }
    }

    public static void sync(ServerPlayer player) {
        Map<String, State> map = STATES.getOrDefault(player.getUUID(), Map.of());
        List<SyncTimersPacket.Entry> entries = new ArrayList<>();
        map.forEach((id, s) -> entries.add(new SyncTimersPacket.Entry(id, s.remaining, s.max)));
        for (TimerSource source : SOURCES) {
            try { source.collect(player, entries); }
            catch (Exception ex) { Abyssorigins.LOGGER.error("Timer source error: {}", ex.getMessage()); }
        }
        PacketDistributor.sendToPlayer(player, new SyncTimersPacket(entries));
    }
}
