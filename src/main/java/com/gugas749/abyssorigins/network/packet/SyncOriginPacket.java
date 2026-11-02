package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Server → owning client: origin, resource and progression (cooldowns travel in SyncAbilitiesPacket). */
public record SyncOriginPacket(
    UUID   target,
    String originId,
    float  resource,
    int    level,
    int    xp
) implements CustomPacketPayload {

    public static final Type<SyncOriginPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_origin"));

    public static final StreamCodec<FriendlyByteBuf, SyncOriginPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> { UUIDUtil.STREAM_CODEC.encode(buf, p.target); buf.writeUtf(p.originId);
                          buf.writeFloat(p.resource);
                          buf.writeVarInt(p.level); buf.writeVarInt(p.xp); },
            buf -> new SyncOriginPacket(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readFloat(),
                                        buf.readVarInt(), buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(this);
    }

    private static class ClientHandler {
        static void handle(SyncOriginPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            Player player = mc.level.getPlayerByUUID(pkt.target);
            if (player == null) return;

            // Update data attachment
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            data.setResource(pkt.resource);
            data.setOriginLevel(pkt.level);
            data.setOriginXp(pkt.xp);

            // Resolve origin
            Origin origin = null;
            if (!pkt.originId.isEmpty()) {
                ResourceLocation id = ResourceLocation.parse(pkt.originId);
                data.setOriginId(id);
                origin = AbyssOriginsAPI.getOrigin(id);
            } else {
                data.setOriginId(null);
            }
            AbyssOriginsAPI.setPlayerOriginClient(player, origin);

            // Feed the HUD cache — only ever with our own data
            if (player == mc.player) {
                ClientOriginData.updateOrigin(origin, pkt.resource, pkt.level, pkt.xp);
            }
        }
    }
}
