package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.ritual.ClientRitual;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → the transforming player: the stages of the Orb / Seed ritual that the client stages
 * (camera, choice menu). {@code origins} = the choices (CHOOSE) or the new origin (TRANSFORMED).
 */
public record RitualPacket(Stage stage, List<String> origins) implements CustomPacketPayload {

    public enum Stage { AWAKEN, CHOOSE, TRANSFORMED, CANCELLED }

    public static RitualPacket awaken()                   { return new RitualPacket(Stage.AWAKEN, List.of()); }
    public static RitualPacket choose(List<String> ids)   { return new RitualPacket(Stage.CHOOSE, ids); }
    public static RitualPacket transformed(String id)     { return new RitualPacket(Stage.TRANSFORMED, List.of(id)); }
    public static RitualPacket cancelled()                { return new RitualPacket(Stage.CANCELLED, List.of()); }

    public static final Type<RitualPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "ritual"));

    public static final StreamCodec<FriendlyByteBuf, RitualPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeEnum(p.stage); buf.writeCollection(p.origins, FriendlyByteBuf::writeUtf); },
        buf -> new RitualPacket(buf.readEnum(Stage.class), buf.readList(FriendlyByteBuf::readUtf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> ClientHandler.handle(this));
    }

    private static class ClientHandler {
        static void handle(RitualPacket pkt) {
            ClientRitual.handle(pkt.stage, pkt.origins);
        }
    }
}
