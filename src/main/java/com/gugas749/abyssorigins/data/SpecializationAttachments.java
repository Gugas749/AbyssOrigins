package com.gugas749.abyssorigins.data;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class SpecializationAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Abyssorigins.MODID);

    public static final Supplier<AttachmentType<PlayerSpecializationData>> PLAYER_SPEC =
        ATTACHMENT_TYPES.register("player_specialization", () ->
            AttachmentType.builder(PlayerSpecializationData::new)
                .serialize(PlayerSpecializationData.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
