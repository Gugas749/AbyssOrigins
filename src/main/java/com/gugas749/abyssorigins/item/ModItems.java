package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(Abyssorigins.MODID);

    public static final DeferredItem<Item> UMBRELLA = ITEMS.register(
        "umbrella", UmbrellaItem::new);

    public static final DeferredItem<Item> HUMAN_HEART = ITEMS.register(
        "human_heart", HumanHeartItem::new);

    public static final DeferredItem<Item> BLOOD_BOTTLE = ITEMS.register(
        "blood_bottle", BloodBottleItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
