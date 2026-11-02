package com.gugas749.abyssorigins.network.packet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.gugas749.abyssorigins.client.eminence.EminenceClientEffects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to trackers: this player kneels (tremble + lowered pose) for the given time. */
public record KneelPacket(int entityId, int ticks) implements CustomPacketPayload {

    public static final Type<KneelPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "kneel"));

    public static final StreamCodec<FriendlyByteBuf, KneelPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.entityId); buf.writeVarInt(p.ticks); },
        buf -> new KneelPacket(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> EminenceClientEffects.kneel(entityId, ticks));
    }
}
