package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.power.Loadouts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: bind a known power to one of the player's own slots (empty id unbinds). */
public record BindAbilityPacket(SlotGroup group, int slot, String abilityId) implements CustomPacketPayload {

    public static final Type<BindAbilityPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "bind_ability"));

    public static final StreamCodec<FriendlyByteBuf, BindAbilityPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeEnum(p.group); buf.writeVarInt(p.slot); buf.writeUtf(p.abilityId, 128); },
        buf -> new BindAbilityPacket(buf.readEnum(SlotGroup.class), buf.readVarInt(), buf.readUtf(128))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Validation (known, fits the slot) happens in Loadouts.bind — nothing here is trusted. */
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) Loadouts.bind(player, group, slot, abilityId);
        });
    }
}
