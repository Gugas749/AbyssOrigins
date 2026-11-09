package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.data.OriginAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server → owning client: granted abilities, both loadouts, every running cooldown and the switched-off area passives. */
public record SyncAbilitiesPacket(List<String> granted, List<String> originSlots, List<String> classSlots,
                                  List<Cooldown> cooldowns, List<String> passivesOff) implements CustomPacketPayload {

    public record Cooldown(String id, int remaining, int max) {}

    public static final Type<SyncAbilitiesPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_abilities"));

    public static final StreamCodec<FriendlyByteBuf, SyncAbilitiesPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeCollection(p.granted, FriendlyByteBuf::writeUtf);
            buf.writeCollection(p.originSlots, FriendlyByteBuf::writeUtf);
            buf.writeCollection(p.classSlots, FriendlyByteBuf::writeUtf);
            buf.writeCollection(p.cooldowns, (b, c) -> { b.writeUtf(c.id); b.writeVarInt(c.remaining); b.writeVarInt(c.max); });
            buf.writeCollection(p.passivesOff, FriendlyByteBuf::writeUtf);
        },
        buf -> new SyncAbilitiesPacket(
            buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(b -> new Cooldown(b.readUtf(), b.readVarInt(), b.readVarInt())),
            buf.readList(FriendlyByteBuf::readUtf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() -> ClientHandler.handle(this));
    }

    private static class ClientHandler {
        static void handle(SyncAbilitiesPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Map<String, int[]> cds = new HashMap<>();
            for (Cooldown c : pkt.cooldowns) cds.put(c.id, new int[]{ c.remaining, c.max });
            mc.player.getData(OriginAttachments.ABILITIES.get()).replace(pkt.granted, pkt.originSlots, pkt.classSlots, cds, pkt.passivesOff);
        }
    }
}
