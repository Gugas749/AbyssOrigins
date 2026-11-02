package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.ClientIdentities;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server → clients: which origin / class other players have. Only identities are
 * shared (never resources, cooldowns or skills) — used for Occult Vision aura colours.
 */
public record IdentityPacket(List<Entry> entries) implements CustomPacketPayload {

    /** Empty strings mean "none". */
    public record Entry(UUID player, String originId, String classId) {}

    public static final Type<IdentityPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "identity"));

    public static final StreamCodec<FriendlyByteBuf, IdentityPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entries.size());
                for (Entry e : p.entries) {
                    UUIDUtil.STREAM_CODEC.encode(buf, e.player);
                    buf.writeUtf(e.originId);
                    buf.writeUtf(e.classId);
                }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Entry> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++)
                    list.add(new Entry(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readUtf()));
                return new IdentityPacket(list);
            }
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> ClientIdentities.apply(entries));
    }
}
