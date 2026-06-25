package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent by the server to the client whenever a player's origin state changes.
 * Carries the full snapshot so the client can update its HUD and GUI correctly.
 */
public record SyncOriginPacket(
    String targetPlayerName,   // used client-side to find the entity
    String originId,           // empty string = no origin
    float resource,
    int cooldown1,
    int cooldown2,
    int level,
    int xp
) implements CustomPacketPayload {

    public static final Type<SyncOriginPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_origin"));

    public static final StreamCodec<FriendlyByteBuf, SyncOriginPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.targetPlayerName);
                buf.writeUtf(pkt.originId);
                buf.writeFloat(pkt.resource);
                buf.writeInt(pkt.cooldown1);
                buf.writeInt(pkt.cooldown2);
                buf.writeInt(pkt.level);
                buf.writeInt(pkt.xp);
            },
            buf -> new SyncOriginPacket(
                buf.readUtf(), buf.readUtf(), buf.readFloat(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()
            )
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientHandler.handle(this);
        }
    }

    // Isolated CLIENT handler — never loaded on server
    private static class ClientHandler {
        static void handle(SyncOriginPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            // Find the player entity by name
            mc.level.players().stream()
                .filter(p -> p.getName().getString().equals(pkt.targetPlayerName))
                .findFirst()
                .ifPresent(player -> {
                    // Update the client-side data attachment
                    PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
                    data.setResource(pkt.resource);
                    data.setCooldown1(pkt.cooldown1);
                    data.setCooldown2(pkt.cooldown2);
                    data.setOriginLevel(pkt.level);
                    data.setOriginXp(pkt.xp);

                    // Update API origin cache
                    if (pkt.originId.isEmpty()) {
                        data.setOriginId(null);
                        AbyssOriginsAPI.setPlayerOriginClient(player, null);
                    } else {
                        ResourceLocation id = ResourceLocation.parse(pkt.originId);
                        data.setOriginId(id);
                        Origin origin = AbyssOriginsAPI.getOrigin(id);
                        AbyssOriginsAPI.setPlayerOriginClient(player, origin);
                    }
                });
        }
    }
}
