package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillEffectHandler;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

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
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
            if (origin == null) return;

            Optional<SkillTree> treeOpt = SkillTreeRegistry.getTree(origin.getId());
            if (treeOpt.isEmpty()) return;
            SkillTree tree = treeOpt.get();

            Optional<SkillNode> nodeOpt = tree.getNode(skillNodeId);
            if (nodeOpt.isEmpty()) return;
            SkillNode node = nodeOpt.get();

            PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

            // Already unlocked
            if (skills.hasSkill(skillNodeId)) return;

            // Check prerequisites
            for (String preReq : node.getPrereqs()) {
                if (!skills.hasSkill(preReq)) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c[AbyssOrigins] You must unlock prerequisites first."));
                    return;
                }
            }

            // Check skill points
            if (skills.getSkillPoints() < node.getPointCost()) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§c[AbyssOrigins] Not enough skill points. Need "
                        + node.getPointCost() + ", have " + skills.getSkillPoints() + "."));
                return;
            }

            // Unlock
            skills.spendSkillPoints(node.getPointCost());
            skills.unlockSkill(skillNodeId);

            // Apply effect
            if (!node.getEffectId().isEmpty()) {
                SkillEffectHandler.onSkillUnlocked(player, node.getEffectId());
            }

            // Sync
            ModPackets.syncPlayer(player);
            ModPackets.syncSkills(player);

            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§a[AbyssOrigins] Unlocked: §e" + node.getDisplayName()));
        });
    }
}
