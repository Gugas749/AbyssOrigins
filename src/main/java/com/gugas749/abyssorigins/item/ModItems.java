package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.item.gear.ArmorSet;
import com.gugas749.abyssorigins.item.gear.OriginArmorItem;
import com.gugas749.abyssorigins.item.gear.OriginWeaponItem;
import com.gugas749.abyssorigins.item.gear.WeaponPowers;
import com.gugas749.abyssorigins.item.ritual.OriginOrbItem;
import com.gugas749.abyssorigins.item.ritual.OriginSeedItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(Abyssorigins.MODID);

    public static final DeferredItem<Item> UMBRELLA = ITEMS.register(
        "umbrella", UmbrellaItem::new);

    public static final DeferredItem<Item> HUMAN_HEART = ITEMS.register(
        "human_heart", HumanHeartItem::new);

    // ---- Origin rituals: choose an origin (orb) or take a predefined one (seed) -------------
    public static final DeferredItem<Item> ORIGIN_ORB = ITEMS.register(
        "origin_orb", OriginOrbItem::new);
    public static final DeferredItem<Item> ORIGIN_SEED = ITEMS.register(
        "origin_seed", OriginSeedItem::new);

    // ---- Relic weapons (one per origin) -----------------------------------------------
    //                                                   origin      power                                dmg  speed  reach
    public static final DeferredItem<Item> OATHKEEPER = ITEMS.register("oathkeeper",
        () -> new OriginWeaponItem("human",    new WeaponPowers.Oathkeeper(),       4, -2.6f, 0.5));
    public static final DeferredItem<Item> SANGUINE_SCYTHE = ITEMS.register("sanguine_scythe",
        () -> new OriginWeaponItem("vampire",  new WeaponPowers.SanguineScythe(),   5, -3.0f, 1.0));
    public static final DeferredItem<Item> HELLBRAND_CLEAVER = ITEMS.register("hellbrand_cleaver",
        () -> new OriginWeaponItem("demon",    new WeaponPowers.HellbrandCleaver(), 6, -3.2f, 0.0));
    public static final DeferredItem<Item> DAWNSPEAR = ITEMS.register("dawnspear",
        () -> new OriginWeaponItem("seraph",   new WeaponPowers.Dawnspear(),        3, -2.5f, 1.5));
    public static final DeferredItem<Item> EVENT_HORIZON = ITEMS.register("event_horizon",
        () -> new OriginWeaponItem("voidborn", new WeaponPowers.EventHorizon(),     6, -3.1f, 0.5));
    public static final DeferredItem<Item> SHADOWFANG = ITEMS.register("shadowfang",
        () -> new OriginWeaponItem("shade",    new WeaponPowers.Shadowfang(),       1, -1.6f, 0.0));

    public static final List<DeferredItem<Item>> WEAPONS =
        List.of(OATHKEEPER, SANGUINE_SCYTHE, HELLBRAND_CLEAVER, DAWNSPEAR, EVENT_HORIZON, SHADOWFANG);

    // ---- Origin outfits: <set>_helmet / _chestplate / _leggings / _boots ----------------

    public static final Map<ArmorSet, List<DeferredItem<Item>>> ARMOR = new EnumMap<>(ArmorSet.class);

    static {
        ArmorItem.Type[] types = { ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS };
        for (ArmorSet set : ArmorSet.values()) {
            List<DeferredItem<Item>> pieces = new ArrayList<>();
            for (ArmorItem.Type type : types)
                pieces.add(ITEMS.register(set.id + "_" + type.getName(), () -> new OriginArmorItem(set, type)));
            ARMOR.put(set, pieces);
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
