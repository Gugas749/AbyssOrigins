package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.ClientOriginData;
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

public record SyncOriginPacket(
    String targetPlayerName,
    String originId,
    float  resource,
    int    cooldown1,
    int    cooldown2,
    int    level,
    int    xp
) implements CustomPacketPayload {

    public static final Type<SyncOriginPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_origin"));

    public static final StreamCodec<FriendlyByteBuf, SyncOriginPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.targetPlayerName); buf.writeUtf(p.originId);
                          buf.writeFloat(p.resource); buf.writeInt(p.cooldown1);
                          buf.writeInt(p.cooldown2);  buf.writeInt(p.level); buf.writeInt(p.xp); },
            buf -> new SyncOriginPacket(buf.readUtf(), buf.readUtf(), buf.readFloat(),
                                        buf.readInt(),  buf.readInt(),  buf.readInt(), buf.readInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(this);
    }

    private static class ClientHandler {
        static void handle(SyncOriginPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            mc.level.players().stream()
                .filter(p -> p.getName().getString().equals(pkt.targetPlayerName))
                .findFirst()
                .ifPresent(player -> {
                    // Update data attachment
                    PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
                    data.setResource(pkt.resource);
                    data.setCooldown1(pkt.cooldown1);
                    data.setCooldown2(pkt.cooldown2);
                    data.setOriginLevel(pkt.level);
                    data.setOriginXp(pkt.xp);

                    // Resolve origin
                    Origin origin = null;
                    if (!pkt.originId.isEmpty()) {
                        ResourceLocation id = ResourceLocation.parse(pkt.originId);
                        data.setOriginId(id);
                        origin = AbyssOriginsAPI.getOrigin(id);
                        AbyssOriginsAPI.setPlayerOriginClient(player, origin);
                    } else {
                        data.setOriginId(null);
                        AbyssOriginsAPI.setPlayerOriginClient(player, null);
                    }

                    // Feed the HUD cache
                    ClientOriginData.updateOrigin(origin, pkt.resource,
                        pkt.cooldown1, pkt.cooldown2, pkt.level, pkt.xp);
                });
        }
    }
}
