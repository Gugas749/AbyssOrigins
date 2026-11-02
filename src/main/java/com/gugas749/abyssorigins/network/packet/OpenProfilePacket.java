package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.admin.ProfileSnapshot;
import com.gugas749.abyssorigins.client.gui.admin.ProfileScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → ADMIN client: a player's profile. {@code open} = from the command (open the screen);
 * otherwise a refresh after an edit, applied only if that profile is still on screen.
 */
public record OpenProfilePacket(ProfileSnapshot snapshot, boolean open) implements CustomPacketPayload {

    public static final Type<OpenProfilePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "open_profile"));

    public static final StreamCodec<FriendlyByteBuf, OpenProfilePacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { ProfileSnapshot.STREAM_CODEC.encode(buf, p.snapshot); buf.writeBoolean(p.open); },
        buf -> new OpenProfilePacket(ProfileSnapshot.STREAM_CODEC.decode(buf), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> ClientHandler.handle(this));
    }

    private static class ClientHandler {
        static void handle(OpenProfilePacket pkt) {
            ProfileScreen.receive(pkt.snapshot, pkt.open);
        }
    }
}
