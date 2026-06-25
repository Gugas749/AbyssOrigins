package com.gugas749.abyssorigins.skilltree.data;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class SkillAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Abyssorigins.MODID);

    public static final Supplier<AttachmentType<PlayerSkillData>> PLAYER_SKILLS =
        ATTACHMENT_TYPES.register("player_skills", () ->
            AttachmentType.builder(PlayerSkillData::new)
                .serialize(PlayerSkillData.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
