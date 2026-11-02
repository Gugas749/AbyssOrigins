package com.gugas749.abyssorigins.data;

import com.gugas749.abyssorigins.api.SlotGroup;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * Every player's powers in one place, the same for every Origin and Class:
 *
 *  granted        — abilities given directly (command, staff GUI, items), including hidden ones.
 *                   They persist across origin / class changes; whether they can be used depends
 *                   on the player's current loadouts (see Loadouts.known).
 *  slots          — the ORIGIN and CLASS loadouts: one ability id per slot ("" = empty).
 *  *_loadout      — which origin / class the slots were laid out for, so a change (or a save from
 *                   before loadouts existed) re-seeds them with the new default kit.
 *  cooldowns      — per ability id, not per slot, so rebinding a power can never skip its cooldown.
 */
public class PlayerAbilityData {

    public static final Codec<PlayerAbilityData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("granted", List.of()).forGetter(d -> List.copyOf(d.granted)),
        Codec.STRING.listOf().optionalFieldOf("origin_slots", List.of()).forGetter(d -> d.slots(SlotGroup.ORIGIN)),
        Codec.STRING.listOf().optionalFieldOf("class_slots", List.of()).forGetter(d -> d.slots(SlotGroup.CLASS)),
        Codec.STRING.optionalFieldOf("origin_loadout", "").forGetter(d -> d.originLoadout),
        Codec.STRING.optionalFieldOf("class_loadout", "").forGetter(d -> d.classLoadout),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("cooldowns", Map.of()).forGetter(d -> d.cooldowns),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("cooldown_max", Map.of()).forGetter(d -> d.cooldownMax)
    ).apply(i, (granted, oSlots, cSlots, oLoad, cLoad, cds, maxes) -> {
        PlayerAbilityData d = new PlayerAbilityData();
        d.granted.addAll(granted);
        d.setSlots(SlotGroup.ORIGIN, oSlots);
        d.setSlots(SlotGroup.CLASS, cSlots);
        d.originLoadout = oLoad;
        d.classLoadout  = cLoad;
        d.cooldowns.putAll(cds);
        d.cooldownMax.putAll(maxes);
        return d;
    }));

    private final Set<String> granted = new LinkedHashSet<>();
    private final EnumMap<SlotGroup, String[]> slots = new EnumMap<>(SlotGroup.class);
    private String originLoadout = "";
    private String classLoadout  = "";
    private final Map<String, Integer> cooldowns   = new HashMap<>();
    private final Map<String, Integer> cooldownMax = new HashMap<>();

    public PlayerAbilityData() {
        for (SlotGroup g : SlotGroup.values()) {
            String[] arr = new String[g.size()];
            Arrays.fill(arr, "");
            slots.put(g, arr);
        }
    }

    // ---- Granted ----------------------------------------------------------------------

    public Set<String> granted()             { return Collections.unmodifiableSet(granted); }
    public boolean isGranted(String id)      { return granted.contains(id); }
    /** @return true if newly granted. */
    public boolean grant(String id)          { return granted.add(id); }
    /** @return true if it was granted. */
    public boolean revoke(String id)         { return granted.remove(id); }

    // ---- Slots ---------------------------------------------------------------------------

    public String slot(SlotGroup g, int i)   { return g.valid(i) ? slots.get(g)[i] : ""; }
    public List<String> slots(SlotGroup g)   { return List.of(slots.get(g)); }

    /** Binds {@code id} to slot {@code i}; a power sits in at most one slot of a loadout. */
    public void bind(SlotGroup g, int i, String id) {
        if (!g.valid(i)) return;
        String[] arr = slots.get(g);
        if (!id.isEmpty()) for (int s = 0; s < arr.length; s++) if (arr[s].equals(id)) arr[s] = "";
        arr[i] = id;
    }

    public int slotOf(SlotGroup g, String id) {
        String[] arr = slots.get(g);
        for (int s = 0; s < arr.length; s++) if (arr[s].equals(id)) return s;
        return -1;
    }

    public void clearSlots(SlotGroup g)      { Arrays.fill(slots.get(g), ""); }

    public void setSlots(SlotGroup g, List<String> ids) {
        String[] arr = slots.get(g);
        Arrays.fill(arr, "");
        for (int s = 0; s < Math.min(arr.length, ids.size()); s++) arr[s] = ids.get(s) == null ? "" : ids.get(s);
    }

    public String loadoutOwner(SlotGroup g)  { return g == SlotGroup.ORIGIN ? originLoadout : classLoadout; }
    public void setLoadoutOwner(SlotGroup g, String owner) {
        if (g == SlotGroup.ORIGIN) originLoadout = owner; else classLoadout = owner;
    }

    // ---- Cooldowns (per ability id) ---------------------------------------------------------

    public int cooldown(String id)    { return cooldowns.getOrDefault(id, 0); }
    public int cooldownMax(String id) { return Math.max(1, cooldownMax.getOrDefault(id, 1)); }
    public Map<String, Integer> cooldowns() { return Collections.unmodifiableMap(cooldowns); }

    public void startCooldown(String id, int ticks) {
        if (ticks <= 0) { cooldowns.remove(id); cooldownMax.remove(id); return; }
        cooldowns.put(id, ticks);
        cooldownMax.put(id, ticks);
    }

    /** Shortens a running cooldown (refund mechanics); never starts one. */
    public void reduceCooldown(String id, int ticks) {
        Integer cd = cooldowns.get(id);
        if (cd == null) return;
        if (cd - ticks <= 0) { cooldowns.remove(id); cooldownMax.remove(id); }
        else cooldowns.put(id, cd - ticks);
    }

    /** Client mirror: replace one cooldown with the server's view (remaining + full length). */
    public void setCooldown(String id, int remaining, int max) {
        if (remaining <= 0) { cooldowns.remove(id); cooldownMax.remove(id); return; }
        cooldowns.put(id, remaining);
        cooldownMax.put(id, Math.max(1, max));
    }

    public void clearCooldowns(Collection<String> ids) {
        ids.forEach(cooldowns::remove);
        ids.forEach(cooldownMax::remove);
    }

    public void tick() {
        if (cooldowns.isEmpty()) return;
        Iterator<Map.Entry<String, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getValue() <= 1) { it.remove(); cooldownMax.remove(e.getKey()); }
            else e.setValue(e.getValue() - 1);
        }
    }

    // ---- Client sync -------------------------------------------------------------------------

    /** Replaces everything with a synced copy (client side). */
    public void replace(Collection<String> granted, List<String> originSlots, List<String> classSlots,
                        Map<String, int[]> cooldowns) {
        this.granted.clear();
        this.granted.addAll(granted);
        setSlots(SlotGroup.ORIGIN, originSlots);
        setSlots(SlotGroup.CLASS, classSlots);
        this.cooldowns.clear();
        this.cooldownMax.clear();
        cooldowns.forEach((id, v) -> setCooldown(id, v[0], v[1]));
    }
}
