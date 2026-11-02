package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.item.ritual.OriginSeedItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Abyssorigins.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ABYSSORIGINS_TAB =
        CREATIVE_TABS.register("abyssorigins_tab", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.abyssorigins"))
                .icon(() -> new ItemStack(ModItems.HUMAN_HEART.get()))
                .displayItems((params, output) -> {
                    output.accept(ModItems.UMBRELLA.get());
                    output.accept(ModItems.HUMAN_HEART.get());
                    output.accept(ModItems.ORIGIN_ORB.get());
                    AbyssOriginsAPI.getAllOrigins().stream().filter(o -> !o.isHidden())
                        .forEach(o -> output.accept(OriginSeedItem.of(o)));
                    ModItems.WEAPONS.forEach(w -> output.accept(w.get()));
                    ModItems.ARMOR.values().forEach(set -> set.forEach(piece -> output.accept(piece.get())));
                })
                .build()
        );

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}
