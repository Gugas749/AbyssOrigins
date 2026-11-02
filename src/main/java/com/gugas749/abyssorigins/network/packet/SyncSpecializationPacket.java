package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SyncSpecializationPacket(
    String specId,            // empty = none
    int    specLevel,
    int    specXp,
    int    skillPoints,
    List<String> unlockedSkills
) implements CustomPacketPayload {

    public static final Type<SyncSpecializationPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_spec"));

    public static final StreamCodec<FriendlyByteBuf, SyncSpecializationPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.specId);
                buf.writeVarInt(p.specLevel);
                buf.writeVarInt(p.specXp);
                buf.writeVarInt(p.skillPoints);
                buf.writeVarInt(p.unlockedSkills.size());
                p.unlockedSkills.forEach(buf::writeUtf);
            },
            buf -> {
                String id  = buf.readUtf();
                int lvl    = buf.readVarInt();
                int xp     = buf.readVarInt();
                int pts    = buf.readVarInt();
                int n      = buf.readVarInt();
                List<String> skills = new ArrayList<>(n);
                for (int i = 0; i < n; i++) skills.add(buf.readUtf());
                return new SyncSpecializationPacket(id, lvl, xp, pts, skills);
            }
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(this);
    }

    private static class ClientHandler {
        static void handle(SyncSpecializationPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            PlayerSpecializationData data =
                mc.player.getData(SpecializationAttachments.PLAYER_SPEC.get());

            data.setSpecLevel(pkt.specLevel);
            data.setSpecXp(pkt.specXp);
            data.setSkillPoints(pkt.skillPoints);

            // REPLACE the unlocked set — the old handler only ever added, so respecs never showed.
            data.clearSkills();
            pkt.unlockedSkills.forEach(data::unlockSkill);

            if (!pkt.specId.isEmpty()) {
                ResourceLocation id = ResourceLocation.parse(pkt.specId);
                data.setSpecId(id);
                var spec = SpecializationRegistry.get(id).orElse(null);
                ClientSpecializationData.update(spec, pkt.specLevel, pkt.specXp, pkt.skillPoints, pkt.unlockedSkills);
            } else {
                data.setSpecId(null);
                ClientSpecializationData.clear();
            }
        }
    }
}
