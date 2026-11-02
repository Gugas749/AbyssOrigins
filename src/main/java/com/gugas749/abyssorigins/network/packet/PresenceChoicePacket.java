package com.gugas749.abyssorigins.network.packet;

import net.minecraft.server.level.ServerPlayer;
import com.gugas749.abyssorigins.origin.eminence.PresenceSessions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Subject to server: kneel (true) or resist (false). */
public record PresenceChoicePacket(boolean kneel) implements CustomPacketPayload {

    public static final Type<PresenceChoicePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "presence_choice"));

    public static final StreamCodec<FriendlyByteBuf, PresenceChoicePacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeBoolean(p.kneel); },
        buf -> new PresenceChoicePacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) PresenceSessions.onChoice(player, kneel);
        });
    }
}
