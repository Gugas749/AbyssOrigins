package com.gugas749.abyssorigins.entity;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mod entities. They only ever come from powers: no spawn eggs, no natural spawning. */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class ModEntities {

    private static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.ENTITY_TYPE, Abyssorigins.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DementorEntity>> DEMENTOR = ENTITIES.register("dementor",
        () -> EntityType.Builder.<DementorEntity>of(DementorEntity::new, MobCategory.MISC)
            .sized(0.9f, 2.8f)
            .eyeHeight(2.45f)
            .clientTrackingRange(10)
            .build("dementor"));

    /** The mire pool the Sovereign Scythe rises out of and sinks into. */
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssMireEntity>> ABYSS_MIRE = ENTITIES.register("abyss_mire",
        () -> EntityType.Builder.<AbyssMireEntity>of(AbyssMireEntity::new, MobCategory.MISC)
            .sized(2.4f, 0.2f)
            .clientTrackingRange(8)
            .updateInterval(20)
            .noSave()
            .fireImmune()
            .build("abyss_mire"));

    /** The Sovereign Scythe in flight. */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownScytheEntity>> THROWN_SCYTHE = ENTITIES.register("thrown_scythe",
        () -> EntityType.Builder.<ThrownScytheEntity>of(ThrownScytheEntity::new, MobCategory.MISC)
            .sized(1.0f, 0.5f)
            .clientTrackingRange(6)
            .updateInterval(2)
            .noSave()
            .fireImmune()
            .build("thrown_scythe"));

    /** Seraph's Judgement: the giant sword of light that falls onto the marked ground. */
    public static final DeferredHolder<EntityType<?>, EntityType<JudgementSwordEntity>> JUDGEMENT_SWORD = ENTITIES.register("judgement_sword",
        () -> EntityType.Builder.<JudgementSwordEntity>of(JudgementSwordEntity::new, MobCategory.MISC)
            .sized(1.0f, 2.0f)
            .clientTrackingRange(10)
            .updateInterval(20)
            .noSave()
            .fireImmune()
            .build("judgement_sword"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(DEMENTOR.get(), DementorEntity.createAttributes().build());
    }
}
