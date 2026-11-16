package com.gugas749.abyssorigins.hiddenpower.realm;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Everything the hidden powers must remember across restarts, saved with the overworld
 * ({@code data/abyssorigins_realms.dat}):
 *
 *   plots        - which pocket-realm plot belongs to which player (one each, never reused)
 *   built        - plots whose starter platform has been built
 *   pocketReturn - where each player entered the pocket realm from (by power or portal)
 *   sessions     - active punishments, keyed by target (paused while the target is offline)
 *   observers    - where a punisher stepped into the punishment realm from
 */
public final class RealmData extends SavedData {

    private static final String NAME = "abyssorigins_realms";

    /** One active punishment. {@code remaining} only counts down while the target is online. */
    public static final class Session {
        public final UUID caster;
        public final int cell;
        public final Location targetReturn;
        public int remaining;

        public Session(UUID caster, int cell, Location targetReturn, int remaining) {
            this.caster = caster;
            this.cell = cell;
            this.targetReturn = targetReturn;
            this.remaining = remaining;
        }
    }

    private final Map<UUID, Integer> plots = new HashMap<>();
    private final Set<Integer> built = new HashSet<>();
    private int nextPlot = 0;
    private final Map<UUID, Location> pocketReturn = new HashMap<>();
    private final Map<UUID, Session> sessions = new LinkedHashMap<>();
    private final Map<UUID, Location> observers = new HashMap<>();

    public static RealmData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(RealmData::new, RealmData::load, null), NAME);
    }

    // ---- Pocket plots ------------------------------------------------------------------------

    /** The player's plot index, allocating the next free one on first use. */
    public int plot(UUID player) {
        Integer i = plots.get(player);
        if (i != null) return i;
        plots.put(player, nextPlot);
        setDirty();
        return nextPlot++;
    }

    public OptionalInt existingPlot(UUID player) {
        Integer i = plots.get(player);
        return i == null ? OptionalInt.empty() : OptionalInt.of(i);
    }

    public boolean isBuilt(int plot)  { return built.contains(plot); }
    public void markBuilt(int plot)   { if (built.add(plot)) setDirty(); }

    public Location pocketReturn(UUID p)                 { return pocketReturn.get(p); }
    public void setPocketReturn(UUID p, Location loc)    { pocketReturn.put(p, loc); setDirty(); }
    public Location takePocketReturn(UUID p)             { Location l = pocketReturn.remove(p); if (l != null) setDirty(); return l; }

    // ---- Punishment --------------------------------------------------------------------------

    public Session session(UUID target)                  { return sessions.get(target); }
    public Map<UUID, Session> sessions()                 { return Collections.unmodifiableMap(sessions); }

    /** The session {@code caster} started, as (target, session), or null. */
    public Map.Entry<UUID, Session> sessionBy(UUID caster) {
        for (Map.Entry<UUID, Session> e : sessions.entrySet()) if (e.getValue().caster.equals(caster)) return e;
        return null;
    }

    public void startSession(UUID target, Session s)     { sessions.put(target, s); setDirty(); }
    public Session endSession(UUID target)               { Session s = sessions.remove(target); if (s != null) setDirty(); return s; }

    /** Lowest cell index no active session is using. */
    public int freeCell() {
        Set<Integer> used = new HashSet<>();
        sessions.values().forEach(s -> used.add(s.cell));
        int c = 0;
        while (used.contains(c)) c++;
        return c;
    }

    public boolean isObserving(UUID p)                   { return observers.containsKey(p); }
    public void setObserving(UUID p, Location from)      { observers.put(p, from); setDirty(); }
    public Location takeObserverReturn(UUID p)           { Location l = observers.remove(p); if (l != null) setDirty(); return l; }

    /** Counting sessions down mutates them; flag the data so the remaining time is saved. */
    public void touch() { setDirty(); }

    // ---- Persistence -------------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next_plot", nextPlot);
        tag.put("plots", list(plots, (t, v) -> t.putInt("plot", v)));
        tag.putIntArray("built", built.stream().mapToInt(Integer::intValue).toArray());
        tag.put("pocket_return", list(pocketReturn, (t, v) -> t.put("loc", v.save())));
        tag.put("observers", list(observers, (t, v) -> t.put("loc", v.save())));
        tag.put("sessions", list(sessions, (t, s) -> {
            t.putUUID("caster", s.caster);
            t.putInt("cell", s.cell);
            t.putInt("remaining", s.remaining);
            t.put("return", s.targetReturn.save());
        }));
        return tag;
    }

    private static RealmData load(CompoundTag tag, HolderLookup.Provider registries) {
        RealmData d = new RealmData();
        d.nextPlot = tag.getInt("next_plot");
        read(tag, "plots", d.plots, t -> t.getInt("plot"));
        for (int i : tag.getIntArray("built")) d.built.add(i);
        read(tag, "pocket_return", d.pocketReturn, t -> Location.load(t.getCompound("loc")));
        read(tag, "observers", d.observers, t -> Location.load(t.getCompound("loc")));
        read(tag, "sessions", d.sessions, t -> {
            Location ret = Location.load(t.getCompound("return"));
            return ret == null || !t.hasUUID("caster") ? null
                : new Session(t.getUUID("caster"), t.getInt("cell"), ret, t.getInt("remaining"));
        });
        return d;
    }

    private static <V> ListTag list(Map<UUID, V> map, BiConsumer<CompoundTag, V> writer) {
        ListTag list = new ListTag();
        map.forEach((id, v) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            writer.accept(t, v);
            list.add(t);
        });
        return list;
    }

    private static <V> void read(CompoundTag tag, String key, Map<UUID, V> into, Function<CompoundTag, V> reader) {
        for (Tag raw : tag.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            if (!t.hasUUID("id")) continue;
            V v = reader.apply(t);
            if (v != null) into.put(t.getUUID("id"), v);
        }
    }
}
