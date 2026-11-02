package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.item.ritual.OriginRitual;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the origin picked in the Orb's choice menu ("" = let the power fade).
 * Only honoured while that player's orb is awaiting a choice (see OriginRitual.choose).
 */
public record ChooseOriginPacket(String originId) implements CustomPacketPayload {

    public static final Type<ChooseOriginPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "choose_origin"));

    public static final StreamCodec<FriendlyByteBuf, ChooseOriginPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> buf.writeUtf(p.originId, 256),
        buf -> new ChooseOriginPacket(buf.readUtf(256))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) OriginRitual.choose(player, originId);
        });
    }
}
