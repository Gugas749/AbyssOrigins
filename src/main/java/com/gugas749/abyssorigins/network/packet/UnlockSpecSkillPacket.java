package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record UnlockSpecSkillPacket(String skillNodeId) implements CustomPacketPayload {

    public static final Type<UnlockSpecSkillPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "unlock_spec_skill"));

    public static final StreamCodec<FriendlyByteBuf, UnlockSpecSkillPacket> STREAM_CODEC =
        StreamCodec.of((buf, p) -> buf.writeUtf(p.skillNodeId), buf -> new UnlockSpecSkillPacket(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());
            if (!data.hasSpec()) return;

            Specialization spec = SpecializationRegistry.get(data.getSpecId()).orElse(null);
            if (spec == null || spec.getSkillTree() == null) return;

            Optional<SkillNode> nodeOpt = spec.getSkillTree().getNode(skillNodeId);
            if (nodeOpt.isEmpty()) return;
            SkillNode node = nodeOpt.get();

            if (data.hasSkill(skillNodeId)) return;

            // Check level requirement
            if (data.getSpecLevel() < node.getTier().getLevelRequired()) {
                player.sendSystemMessage(Component.translatable(
                    "abyssorigins.message.skill_prereqs_missing"));
                return;
            }

            // Check prerequisites
            for (String req : node.getPrereqs()) {
                if (!data.hasSkill(req)) {
                    player.sendSystemMessage(Component.translatable(
                        "abyssorigins.message.skill_prereqs_missing"));
                    return;
                }
            }

            // Check points
            if (data.getSkillPoints() < node.getPointCost()) {
                player.sendSystemMessage(Component.translatable(
                    "abyssorigins.message.skill_not_enough_points",
                    String.valueOf(node.getPointCost()),
                    String.valueOf(data.getSkillPoints())));
                return;
            }

            data.spendSkillPoints(node.getPointCost());
            data.unlockSkill(skillNodeId);

            ModPackets.syncSpecialization(player);

            player.sendSystemMessage(Component.translatable(
                "abyssorigins.message.skill_unlocked",
                node.getDisplayName().getString()));
        });
    }
}
