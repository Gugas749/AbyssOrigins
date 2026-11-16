package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.power.AuraToggles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: switch one of the player's own area passives on or off. */
public record TogglePassivePacket(String skillId, boolean on) implements CustomPacketPayload {

    public static final Type<TogglePassivePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "toggle_passive"));

    public static final StreamCodec<FriendlyByteBuf, TogglePassivePacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.skillId, 128); buf.writeBoolean(p.on); },
        buf -> new TogglePassivePacket(buf.readUtf(128), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** AuraToggles.set ignores ids that are not area passives. */
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) AuraToggles.set(player, skillId, on);
        });
    }
}
