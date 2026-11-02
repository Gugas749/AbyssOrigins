package com.gugas749.abyssorigins.network.packet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.gugas749.abyssorigins.client.eminence.EminenceClientEffects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to nearby clients: the ground shakes around a point (camera tremor scaled by distance). */
public record QuakePacket(double x, double y, double z, float radius, int ticks, float strength) implements CustomPacketPayload {

    public static final Type<QuakePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "quake"));

    public static final StreamCodec<FriendlyByteBuf, QuakePacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z); buf.writeFloat(p.radius); buf.writeVarInt(p.ticks); buf.writeFloat(p.strength); },
        buf -> new QuakePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readVarInt(), buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> EminenceClientEffects.quake(x, y, z, radius, ticks, strength));
    }
}
