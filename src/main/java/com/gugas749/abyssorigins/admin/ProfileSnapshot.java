package com.gugas749.abyssorigins.admin;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerAbilityData;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Everything the staff profile shows about one player, captured on the server and sent
 * only to ADMINs. Read-only on the client: every edit goes back through ProfileService.
 */
public record ProfileSnapshot(
    UUID uuid, String name,
    String originId, float resource, int originLevel, int originXp, int originPoints, List<String> originSkills,
    String classId, int classLevel, int classXp, int classPoints, List<String> classSkills,
    List<String> granted, List<String> originSlots, List<String> classSlots
) {

    public static final StreamCodec<FriendlyByteBuf, ProfileSnapshot> STREAM_CODEC = StreamCodec.of(
        (buf, s) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, s.uuid);
            buf.writeUtf(s.name);
            buf.writeUtf(s.originId);
            buf.writeFloat(s.resource);
            buf.writeVarInt(s.originLevel);
            buf.writeVarInt(s.originXp);
            buf.writeVarInt(s.originPoints);
            buf.writeCollection(s.originSkills, FriendlyByteBuf::writeUtf);
            buf.writeUtf(s.classId);
            buf.writeVarInt(s.classLevel);
            buf.writeVarInt(s.classXp);
            buf.writeVarInt(s.classPoints);
            buf.writeCollection(s.classSkills, FriendlyByteBuf::writeUtf);
            buf.writeCollection(s.granted, FriendlyByteBuf::writeUtf);
            buf.writeCollection(s.originSlots, FriendlyByteBuf::writeUtf);
            buf.writeCollection(s.classSlots, FriendlyByteBuf::writeUtf);
        },
        buf -> new ProfileSnapshot(
            UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(),
            buf.readUtf(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readUtf),
            buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf))
    );

    public static ProfileSnapshot of(ServerPlayer p) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(p);
        PlayerOriginData od = p.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PlayerSkillData skills = p.getData(SkillAttachments.PLAYER_SKILLS.get());
        PlayerSpecializationData sd = p.getData(SpecializationAttachments.PLAYER_SPEC.get());
        PlayerAbilityData ad = Loadouts.data(p);
        return new ProfileSnapshot(
            p.getUUID(), p.getGameProfile().getName(),
            origin != null ? origin.getId().toString() : "", od.getResource(), od.getOriginLevel(), od.getOriginXp(),
            skills.getSkillPoints(), List.copyOf(skills.getUnlockedSkills()),
            sd.hasSpec() ? sd.getSpecId().toString() : "", sd.getSpecLevel(), sd.getSpecXp(), sd.getSkillPoints(),
            List.copyOf(sd.getUnlockedSkills()),
            List.copyOf(ad.granted()), ad.slots(SlotGroup.ORIGIN), ad.slots(SlotGroup.CLASS));
    }

    // ---- Lookups (either side) -------------------------------------------------------------

    public Origin origin() {
        ResourceLocation id = originId.isEmpty() ? null : ResourceLocation.tryParse(originId);
        return id == null ? null : AbyssOriginsAPI.getOrigin(id);
    }

    public Specialization spec() {
        ResourceLocation id = classId.isEmpty() ? null : ResourceLocation.tryParse(classId);
        return id == null ? null : SpecializationRegistry.get(id).orElse(null);
    }

    public List<String> slots(SlotGroup g) { return g == SlotGroup.ORIGIN ? originSlots : classSlots; }

    /** Every ability this player knows in a group, with how — same maths the server uses. */
    public Map<String, Loadouts.Source> known(SlotGroup g) {
        return Loadouts.known(g, origin(), spec(), granted, originSkills, classSkills);
    }
}
