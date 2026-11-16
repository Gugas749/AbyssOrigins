package com.gugas749.abyssorigins.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * Stored state of the Eminence's Sovereign Scythe.
 *
 *  stored     - the last known state of the scythe (enchantments, name...). Kept up to date while it is
 *               carried, so whatever happens to the live copy, the next summon brings it back as it was.
 *               Empty until the first summon (a new scythe is created then).
 *  generation - bumped by every summon. Each live copy carries the generation it was summoned with
 *               (ScytheBond); an older copy found anywhere is deleted.
 */
public class ScytheData {

    public static final Codec<ScytheData> CODEC = RecordCodecBuilder.create(i -> i.group(
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("stored", ItemStack.EMPTY).forGetter(d -> d.stored),
        Codec.INT.optionalFieldOf("generation", 0).forGetter(d -> d.generation)
    ).apply(i, (stored, gen) -> {
        ScytheData d = new ScytheData();
        d.stored = stored;
        d.generation = gen;
        return d;
    }));

    private ItemStack stored = ItemStack.EMPTY;
    private int generation;

    public ItemStack stored()            { return stored; }
    public void store(ItemStack stack)   { this.stored = stack.copyWithCount(1); }
    public int generation()              { return generation; }
    public int nextGeneration()          { return ++generation; }
}
