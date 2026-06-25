package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
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

public record SyncSkillsPacket(List<String> unlockedSkills, int skillPoints)
    implements CustomPacketPayload {

    public static final Type<SyncSkillsPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "sync_skills"));

    public static final StreamCodec<FriendlyByteBuf, SyncSkillsPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> { buf.writeInt(p.unlockedSkills.size()); p.unlockedSkills.forEach(buf::writeUtf); buf.writeInt(p.skillPoints); },
            buf -> { int n = buf.readInt(); List<String> s = new ArrayList<>(n);
                     for (int i=0;i<n;i++) s.add(buf.readUtf());
                     return new SyncSkillsPacket(s, buf.readInt()); }
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(this);
    }

    private static class ClientHandler {
        static void handle(SyncSkillsPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Update data attachment
            PlayerSkillData data = mc.player.getData(SkillAttachments.PLAYER_SKILLS.get());
            pkt.unlockedSkills.forEach(data::unlockSkill);
            int delta = pkt.skillPoints - data.getSkillPoints();
            data.addSkillPoints(delta);

            // Feed HUD cache
            ClientOriginData.updateSkills(pkt.unlockedSkills, pkt.skillPoints);
        }
    }
}
