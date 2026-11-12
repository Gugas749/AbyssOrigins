package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.Abyssorigins;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.UUID;
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

    /** Binds a summoned Sovereign Scythe to its Eminence and to the summon that made it. */
    public record ScytheBond(UUID owner, int generation) {
        public static final Codec<ScytheBond> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(ScytheBond::owner),
            Codec.INT.fieldOf("generation").forGetter(ScytheBond::generation)
        ).apply(i, ScytheBond::new));
        public static final StreamCodec<ByteBuf, ScytheBond> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ScytheBond::owner,
            ByteBufCodecs.VAR_INT, ScytheBond::generation,
            ScytheBond::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ScytheBond>> SCYTHE_BOND =
        COMPONENTS.registerComponentType("scythe_bond", b -> b
            .persistent(ScytheBond.CODEC)
            .networkSynchronized(ScytheBond.STREAM_CODEC));

    private ModDataComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
