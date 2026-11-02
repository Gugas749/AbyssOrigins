package com.gugas749.abyssorigins.network;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.data.PlayerAbilityData;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.packet.*;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public class ModPackets {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(Abyssorigins.MODID).versioned("3.0");

        reg.playToClient(SyncOriginPacket.TYPE,         SyncOriginPacket.STREAM_CODEC,         SyncOriginPacket::handle);
        reg.playToClient(SyncSkillsPacket.TYPE,         SyncSkillsPacket.STREAM_CODEC,         SyncSkillsPacket::handle);
        reg.playToClient(SyncSpecializationPacket.TYPE, SyncSpecializationPacket.STREAM_CODEC, SyncSpecializationPacket::handle);
        reg.playToClient(SyncTimersPacket.TYPE,         SyncTimersPacket.STREAM_CODEC,         SyncTimersPacket::handle);
        reg.playToClient(IdentityPacket.TYPE,           IdentityPacket.STREAM_CODEC,           IdentityPacket::handle);
        reg.playToClient(RevealPacket.TYPE,             RevealPacket.STREAM_CODEC,             RevealPacket::handle);
        reg.playToClient(VfxPacket.TYPE,                VfxPacket.STREAM_CODEC,                VfxPacket::handle);
        reg.playToClient(SyncAbilitiesPacket.TYPE,      SyncAbilitiesPacket.STREAM_CODEC,      SyncAbilitiesPacket::handle);
        reg.playToClient(OpenProfilePacket.TYPE,        OpenProfilePacket.STREAM_CODEC,        OpenProfilePacket::handle);
        reg.playToClient(RitualPacket.TYPE,             RitualPacket.STREAM_CODEC,             RitualPacket::handle);
        reg.playToClient(QuakePacket.TYPE,              QuakePacket.STREAM_CODEC,              QuakePacket::handle);
        reg.playToClient(PresencePromptPacket.TYPE,     PresencePromptPacket.STREAM_CODEC,     PresencePromptPacket::handle);
        reg.playToClient(ResistStartPacket.TYPE,        ResistStartPacket.STREAM_CODEC,        ResistStartPacket::handle);
        reg.playToClient(KneelPacket.TYPE,              KneelPacket.STREAM_CODEC,              KneelPacket::handle);

        reg.playToServer(ActivateAbilityPacket.TYPE,    ActivateAbilityPacket.STREAM_CODEC,    ActivateAbilityPacket::handle);
        reg.playToServer(ActivateSpecAbilityPacket.TYPE,ActivateSpecAbilityPacket.STREAM_CODEC,ActivateSpecAbilityPacket::handle);
        reg.playToServer(UnlockSkillPacket.TYPE,        UnlockSkillPacket.STREAM_CODEC,        UnlockSkillPacket::handle);
        reg.playToServer(UnlockSpecSkillPacket.TYPE,    UnlockSpecSkillPacket.STREAM_CODEC,    UnlockSpecSkillPacket::handle);
        reg.playToServer(BindAbilityPacket.TYPE,        BindAbilityPacket.STREAM_CODEC,        BindAbilityPacket::handle);
        reg.playToServer(ProfileEditPacket.TYPE,        ProfileEditPacket.STREAM_CODEC,        ProfileEditPacket::handle);
        reg.playToServer(ChooseOriginPacket.TYPE,       ChooseOriginPacket.STREAM_CODEC,       ChooseOriginPacket::handle);
        reg.playToServer(PresenceChoicePacket.TYPE,     PresenceChoicePacket.STREAM_CODEC,     PresenceChoicePacket::handle);
        reg.playToServer(ResistResultPacket.TYPE,       ResistResultPacket.STREAM_CODEC,       ResistResultPacket::handle);
    }

    public static void syncPlayer(ServerPlayer player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PacketDistributor.sendToPlayer(player, new SyncOriginPacket(
            player.getUUID(),
            origin != null ? origin.getId().toString() : "",
            data.getResource(), data.getOriginLevel(), data.getOriginXp()
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

        PacketDistributor.sendToPlayer(player, new SyncSpecializationPacket(
            data.hasSpec() ? data.getSpecId().toString() : "",
            data.getSpecLevel(),
            data.getSpecXp(),
            data.getSkillPoints(),
            data.getUnlockedSkills()
        ));
    }

    /** Granted abilities, both loadouts and every running cooldown. */
    public static void syncAbilities(ServerPlayer player) {
        PlayerAbilityData d = Loadouts.data(player);
        List<SyncAbilitiesPacket.Cooldown> cds = new ArrayList<>();
        d.cooldowns().forEach((id, ticks) -> cds.add(new SyncAbilitiesPacket.Cooldown(id, ticks, d.cooldownMax(id))));
        PacketDistributor.sendToPlayer(player, new SyncAbilitiesPacket(
            List.copyOf(d.granted()), d.slots(SlotGroup.ORIGIN), d.slots(SlotGroup.CLASS), cds));
    }

    public static void syncAll(ServerPlayer player) {
        syncPlayer(player);
        syncAbilities(player);
        syncSkills(player);
        syncSpecialization(player);
        AbilityStates.sync(player);
    }

    // ---- Identity broadcast (origin/class of other players) ---------------

    private static IdentityPacket.Entry identityOf(ServerPlayer p) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(p);
        PlayerSpecializationData spec = p.getData(SpecializationAttachments.PLAYER_SPEC.get());
        return new IdentityPacket.Entry(p.getUUID(),
            origin != null ? origin.getId().toString() : "",
            spec.hasSpec() ? spec.getSpecId().toString() : "");
    }

    /** Tell everyone about {@code player}'s current origin and class. */
    public static void broadcastIdentity(ServerPlayer player) {
        PacketDistributor.sendToAllPlayers(new IdentityPacket(List.of(identityOf(player))));
    }

    /** Send the identities of every online player to {@code player} (on login). */
    public static void sendAllIdentities(ServerPlayer player) {
        List<IdentityPacket.Entry> all = new ArrayList<>();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) all.add(identityOf(p));
        PacketDistributor.sendToPlayer(player, new IdentityPacket(all));
    }
}
