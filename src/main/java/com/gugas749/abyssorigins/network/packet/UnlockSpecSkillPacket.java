package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.skilltree.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UnlockSpecSkillPacket(String skillNodeId) implements CustomPacketPayload {

    public static final Type<UnlockSpecSkillPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "unlock_spec_skill"));

    public static final StreamCodec<FriendlyByteBuf, UnlockSpecSkillPacket> STREAM_CODEC =
        StreamCodec.of((buf, p) -> buf.writeUtf(p.skillNodeId), buf -> new UnlockSpecSkillPacket(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) SkillService.unlockClassSkill(player, skillNodeId);
        });
    }
}
