package com.gugas749.abyssorigins.network.packet;

import net.minecraft.server.level.ServerPlayer;
import com.gugas749.abyssorigins.origin.eminence.PresenceSessions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Subject to server: the outcome of the resistance skill check. */
public record ResistResultPacket(boolean success) implements CustomPacketPayload {

    public static final Type<ResistResultPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "resist_result"));

    public static final StreamCodec<FriendlyByteBuf, ResistResultPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeBoolean(p.success); },
        buf -> new ResistResultPacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) PresenceSessions.onResult(player, success);
        });
    }
}
