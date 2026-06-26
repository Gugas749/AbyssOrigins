package com.gugas749.abyssorigins.network;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.packet.*;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPackets {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(Abyssorigins.MODID).versioned("1.0");

        reg.playToClient(SyncOriginPacket.TYPE,         SyncOriginPacket.STREAM_CODEC,         SyncOriginPacket::handle);
        reg.playToClient(SyncSkillsPacket.TYPE,         SyncSkillsPacket.STREAM_CODEC,         SyncSkillsPacket::handle);
        reg.playToClient(SyncSpecializationPacket.TYPE, SyncSpecializationPacket.STREAM_CODEC, SyncSpecializationPacket::handle);

        reg.playToServer(ActivateAbilityPacket.TYPE,    ActivateAbilityPacket.STREAM_CODEC,    ActivateAbilityPacket::handle);
        reg.playToServer(ActivateSpecAbilityPacket.TYPE,ActivateSpecAbilityPacket.STREAM_CODEC,ActivateSpecAbilityPacket::handle);
        reg.playToServer(UnlockSkillPacket.TYPE,        UnlockSkillPacket.STREAM_CODEC,        UnlockSkillPacket::handle);
        reg.playToServer(UnlockSpecSkillPacket.TYPE,    UnlockSpecSkillPacket.STREAM_CODEC,    UnlockSpecSkillPacket::handle);
    }

    public static void syncPlayer(ServerPlayer player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PacketDistributor.sendToPlayer(player, new SyncOriginPacket(
            player.getName().getString(),
            origin != null ? origin.getId().toString() : "",
            data.getResource(), data.getCooldown1(), data.getCooldown2(),
            data.getOriginLevel(), data.getOriginXp()
        ));
    }

    public static void syncSkills(ServerPlayer player) {
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        PacketDistributor.sendToPlayer(player,
            new SyncSkillsPacket(skills.getUnlockedSkills(), skills.getSkillPoints()));
    }

    public static void syncSpecialization(ServerPlayer player) {
        PlayerSpecializationData data =
            player.getData(SpecializationAttachments.PLAYER_SPEC.get());

        Specialization spec = data.hasSpec()
            ? SpecializationRegistry.get(data.getSpecId()).orElse(null)
            : null;

        int maxCd = spec != null && !spec.getAbilities().isEmpty()
            ? spec.getAbilities().get(0).getBaseCooldownTicks()
            : 1;

        PacketDistributor.sendToPlayer(player, new SyncSpecializationPacket(
            data.hasSpec() ? data.getSpecId().toString() : "",
            data.getSpecLevel(),
            data.getSkillPoints(),
            data.getUnlockedSkills(),
            data.getAbilityCooldown(),
            maxCd
        ));
    }

    public static void syncAll(ServerPlayer player) {
        syncPlayer(player);
        syncSkills(player);
        syncSpecialization(player);
    }

    public static void syncPlayerToAll(ServerPlayer player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncOriginPacket(
            player.getName().getString(),
            origin != null ? origin.getId().toString() : "",
            data.getResource(), data.getCooldown1(), data.getCooldown2(),
            data.getOriginLevel(), data.getOriginXp()
        ));
    }
}
