package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.reveal.RevealClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → ONE client: outline an entity for a while, in a colour, for this viewer only.
 *
 * This is how marks (Hunter's Mark, Mark for Death) become visible without the vanilla
 * Glowing effect, which is a world-wide entity flag every player would see.
 */
public record RevealPacket(int entityId, int ticks, int color) implements CustomPacketPayload {

    public static final Type<RevealPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "reveal"));

    public static final StreamCodec<FriendlyByteBuf, RevealPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entityId); buf.writeVarInt(p.ticks); buf.writeInt(p.color); },
            buf -> new RevealPacket(buf.readVarInt(), buf.readVarInt(), buf.readInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> RevealClient.mark(entityId, ticks, color));
    }

    /** Outline {@code target} for {@code viewer} only. */
    public static void send(ServerPlayer viewer, Entity target, int ticks, int rgb) {
        PacketDistributor.sendToPlayer(viewer, new RevealPacket(target.getId(), ticks, rgb & 0xFFFFFF));
    }
}
