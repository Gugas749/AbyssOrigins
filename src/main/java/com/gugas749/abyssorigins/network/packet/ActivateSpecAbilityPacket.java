package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.power.AbilityController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: activate whatever is bound to class slot {@code slot} (0-7). */
public record ActivateSpecAbilityPacket(int slot) implements CustomPacketPayload {

    public static final Type<ActivateSpecAbilityPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "activate_spec_ability"));

    public static final StreamCodec<FriendlyByteBuf, ActivateSpecAbilityPacket> STREAM_CODEC =
        StreamCodec.of((buf, p) -> buf.writeInt(p.slot), buf -> new ActivateSpecAbilityPacket(buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) AbilityController.activate(player, SlotGroup.CLASS, slot);
        });
    }
}
