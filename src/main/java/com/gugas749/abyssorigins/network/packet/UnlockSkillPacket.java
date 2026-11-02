package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.skilltree.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UnlockSkillPacket(String skillNodeId) implements CustomPacketPayload {

    public static final Type<UnlockSkillPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "unlock_skill"));

    public static final StreamCodec<FriendlyByteBuf, UnlockSkillPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeUtf(pkt.skillNodeId),
            buf -> new UnlockSkillPacket(buf.readUtf())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) SkillService.unlockOriginSkill(player, skillNodeId);
        });
    }
}
