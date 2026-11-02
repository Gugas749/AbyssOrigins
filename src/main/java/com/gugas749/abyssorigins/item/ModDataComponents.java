package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Abyssorigins.MODID);

    /**
     * The origin a Seed of Origin transforms into, e.g.
     * {@code /give @p abyssorigins:origin_seed[abyssorigins:seed_origin="abyssorigins:vampire"]}
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SEED_ORIGIN =
        COMPONENTS.registerComponentType("seed_origin", b -> b
            .persistent(ResourceLocation.CODEC)
            .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private ModDataComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
