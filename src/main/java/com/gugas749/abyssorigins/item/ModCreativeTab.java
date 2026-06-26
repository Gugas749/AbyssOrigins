package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.Abyssorigins;
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
                .icon(() -> new ItemStack(ModItems.BLOOD_BOTTLE.get()))
                .displayItems((params, output) -> {
                    output.accept(ModItems.UMBRELLA.get());
                    output.accept(ModItems.HUMAN_HEART.get());
                    output.accept(ModItems.BLOOD_BOTTLE.get());
                })
                .build()
        );

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}
