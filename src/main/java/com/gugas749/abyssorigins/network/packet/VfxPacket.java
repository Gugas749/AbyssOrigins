package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.vfx.VfxManager;
import com.gugas749.abyssorigins.vfx.VfxSpec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: play one 3D effect. */
public record VfxPacket(VfxSpec spec) implements CustomPacketPayload {

    public static final Type<VfxPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "vfx"));

    public static final StreamCodec<FriendlyByteBuf, VfxPacket> STREAM_CODEC =
        StreamCodec.of((buf, p) -> p.spec.write(buf), buf -> new VfxPacket(VfxSpec.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> ClientHandler.play(spec));
    }

    /** Isolates the client-only class reference from dedicated servers. */
    private static final class ClientHandler {
        static void play(VfxSpec spec) { VfxManager.spawn(spec); }
    }
}
