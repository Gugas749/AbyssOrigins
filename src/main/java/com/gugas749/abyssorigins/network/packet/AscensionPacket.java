package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.ascension.AscensionTitle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to the ascending player: they started ascending into {@code stage} of the origin {@code originId}.
 * Sent with the ceremony's AbyssCore cinematic (ascension.AscensionCinematic); the client draws the new
 * title over it (AscensionTitle).
 */
public record AscensionPacket(String originId, int stage) implements CustomPacketPayload {

    public static final Type<AscensionPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "ascension"));

    public static final StreamCodec<FriendlyByteBuf, AscensionPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.originId); buf.writeVarInt(p.stage); },
        buf -> new AscensionPacket(buf.readUtf(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> AscensionTitle.show(originId, stage));
    }
}
