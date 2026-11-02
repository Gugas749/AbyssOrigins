package com.gugas749.abyssorigins.network.packet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.gugas749.abyssorigins.client.eminence.EminenceClientEffects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to subject: the Eminence's Presence demands an answer, kneel or resist. */
public record PresencePromptPacket(String casterName, int ticks) implements CustomPacketPayload {

    public static final Type<PresencePromptPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "presence_prompt"));

    public static final StreamCodec<FriendlyByteBuf, PresencePromptPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.casterName); buf.writeVarInt(p.ticks); },
        buf -> new PresencePromptPacket(buf.readUtf(64), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> EminenceClientEffects.openPrompt(casterName, ticks));
    }
}
