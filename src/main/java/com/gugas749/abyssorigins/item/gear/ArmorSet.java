package com.gugas749.abyssorigins.item.gear;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * The six origin outfits. Three are forged armor, three are enchanted clothing
 * (lighter, more enchantable). Defense order: helmet, chestplate, leggings, boots.
 */
public enum ArmorSet {
    VANGUARD ("vanguard",  "human",    new int[]{3, 8, 6, 3}, 2f, 0.05f, 33, 12, false, () -> SoundEvents.ARMOR_EQUIP_IRON,      () -> Items.IRON_INGOT),
    NOCTURNE ("nocturne",  "vampire",  new int[]{2, 6, 5, 2}, 1f, 0f,    25, 20, true,  () -> SoundEvents.ARMOR_EQUIP_LEATHER,   () -> Items.BLACK_WOOL),
    INFERNAL ("infernal",  "demon",    new int[]{3, 7, 6, 3}, 2f, 0.05f, 35, 12, false, () -> SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Items.NETHER_BRICK),
    CHOIR    ("choir",     "seraph",   new int[]{2, 5, 4, 2}, 1f, 0f,    25, 22, true,  () -> SoundEvents.ARMOR_EQUIP_GOLD,      () -> Items.GOLD_INGOT),
    ABYSSAL  ("abyssal",   "voidborn", new int[]{3, 7, 6, 3}, 3f, 0.1f,  37, 14, false, () -> SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Items.OBSIDIAN),
    DUSKWEAVE("duskweave", "shade",    new int[]{2, 5, 4, 2}, 1f, 0f,    25, 20, true,  () -> SoundEvents.ARMOR_EQUIP_LEATHER,   () -> Items.PHANTOM_MEMBRANE);

    public final String id, origin;
    public final int[] defense;
    public final float toughness, knockbackResistance;
    public final int durabilityMultiplier, enchantability;
    public final boolean cloth;
    public final Supplier<Holder<SoundEvent>> equipSound;
    public final Supplier<Item> repair;

    ArmorSet(String id, String origin, int[] defense, float toughness, float kb, int durability, int enchantability,
             boolean cloth, Supplier<Holder<SoundEvent>> equipSound, Supplier<Item> repair) {
        this.id = id; this.origin = origin; this.defense = defense; this.toughness = toughness;
        this.knockbackResistance = kb; this.durabilityMultiplier = durability; this.enchantability = enchantability;
        this.cloth = cloth; this.equipSound = equipSound; this.repair = repair;
    }

    public static ArmorSet forOrigin(String origin) {
        for (ArmorSet s : values()) if (s.origin.equals(origin)) return s;
        return null;
    }
}
