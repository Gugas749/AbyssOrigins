package com.gugas749.abyssorigins.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * Legacy: the Eminence's old sovereign bindings. Every player now has origin slots in
 * PlayerAbilityData; this is only read once to carry an existing Eminence's bindings over.
 */
public class EminenceData {

    public static final int SLOTS = 5;

    public static final Codec<EminenceData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("bindings", List.of()).forGetter(d -> List.of(d.bindings)),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("cooldowns", Map.of()).forGetter(d -> d.cooldowns),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("cooldown_max", Map.of()).forGetter(d -> d.cooldownMax),
        Codec.BOOL.optionalFieldOf("initialised", false).forGetter(d -> d.initialised)
    ).apply(i, (binds, cds, maxes, init) -> {
        EminenceData d = new EminenceData();
        for (int s = 0; s < Math.min(SLOTS, binds.size()); s++) d.bindings[s] = binds.get(s);
        d.cooldowns.putAll(cds);
        d.cooldownMax.putAll(maxes);
        d.initialised = init;
        return d;
    }));

    private final String[] bindings = new String[SLOTS];
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private final Map<String, Integer> cooldownMax = new HashMap<>();
    private boolean initialised = false;

    public EminenceData() { Arrays.fill(bindings, ""); }

    public String binding(int slot)            { return slot >= 0 && slot < SLOTS ? bindings[slot] : ""; }
    public List<String> bindings()             { return List.of(bindings); }
    public boolean isInitialised()             { return initialised; }
    public void markInitialised()              { initialised = true; }

    /** Binds {@code abilityId} to {@code slot}; a power can only sit in one slot at a time. */
    public void bind(int slot, String abilityId) {
        if (slot < 0 || slot >= SLOTS) return;
        if (!abilityId.isEmpty())
            for (int s = 0; s < SLOTS; s++) if (bindings[s].equals(abilityId)) bindings[s] = "";
        bindings[slot] = abilityId;
    }

    public int cooldown(String id)    { return cooldowns.getOrDefault(id, 0); }
    public int cooldownMax(String id) { return Math.max(1, cooldownMax.getOrDefault(id, 1)); }

    public void startCooldown(String id, int ticks) {
        if (ticks <= 0) { cooldowns.remove(id); return; }
        cooldowns.put(id, ticks);
        cooldownMax.put(id, ticks);
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

    public void clearCooldowns() { cooldowns.clear(); cooldownMax.clear(); }
}
