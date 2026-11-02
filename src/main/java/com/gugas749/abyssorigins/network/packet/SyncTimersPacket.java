package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.ClientTimers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server → owning client: the full set of the player's active sustained abilities. */
public record SyncTimersPacket(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(String id, int remaining, int max) {}

    public static final Type<SyncTimersPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_timers"));

    public static final StreamCodec<FriendlyByteBuf, SyncTimersPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entries.size());
                for (Entry e : p.entries) { buf.writeUtf(e.id); buf.writeVarInt(e.remaining); buf.writeVarInt(e.max); }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Entry> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(new Entry(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
                return new SyncTimersPacket(list);
            }
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> ClientTimers.replace(entries));
    }
}
