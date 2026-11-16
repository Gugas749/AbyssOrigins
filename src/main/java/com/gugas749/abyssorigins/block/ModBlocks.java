package com.gugas749.abyssorigins.block;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks with no item form: only the hidden powers place them. */
public final class ModBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Abyssorigins.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Abyssorigins.MODID);

    /** The cross in the punishment realm (Punish Dimension). Unbreakable, never dropped. */
    public static final DeferredBlock<PunishCrossBlock> PUNISH_CROSS = BLOCKS.register("punish_cross",
        () -> new PunishCrossBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .strength(-1f, 3600000f)
            .noLootTable()
            .noOcclusion()
            .sound(SoundType.WOOD)
            .lightLevel(s -> 6)));                      // the two candles at its foot

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PunishCrossBlockEntity>> PUNISH_CROSS_BE =
        BLOCK_ENTITIES.register("punish_cross",
            () -> BlockEntityType.Builder.of(PunishCrossBlockEntity::new, PUNISH_CROSS.get()).build(null));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
