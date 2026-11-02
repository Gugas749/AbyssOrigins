package com.gugas749.abyssorigins.network.packet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.gugas749.abyssorigins.client.eminence.EminenceClientEffects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to subject: begin the resistance skill check. */
public record ResistStartPacket(long seed, int ticks) implements CustomPacketPayload {

    public static final Type<ResistStartPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "resist_start"));

    public static final StreamCodec<FriendlyByteBuf, ResistStartPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeLong(p.seed); buf.writeVarInt(p.ticks); },
        buf -> new ResistStartPacket(buf.readLong(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> EminenceClientEffects.openResist(seed, ticks));
    }
}
