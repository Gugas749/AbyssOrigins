package com.gugas749.abyssorigins.data;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class OriginAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Abyssorigins.MODID);

    public static final Supplier<AttachmentType<PlayerOriginData>> PLAYER_ORIGIN =
        ATTACHMENT_TYPES.register("player_origin", () ->
            AttachmentType.builder(PlayerOriginData::new)
                .serialize(PlayerOriginData.CODEC)
                .copyOnDeath()   // preserve origin across death
                .build()
        );

    /** Granted abilities, the origin + class loadouts and per-ability cooldowns (every player). */
    public static final Supplier<AttachmentType<PlayerAbilityData>> ABILITIES =
        ATTACHMENT_TYPES.register("abilities", () ->
            AttachmentType.builder(PlayerAbilityData::new)
                .serialize(PlayerAbilityData.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
