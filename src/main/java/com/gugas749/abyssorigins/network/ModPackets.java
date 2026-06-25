package com.gugas749.abyssorigins.network;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.packet.ActivateAbilityPacket;
import com.gugas749.abyssorigins.network.packet.SyncOriginPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPackets {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(Abyssorigins.MODID).versioned("1.0");

        reg.playToClient(
            SyncOriginPacket.TYPE,
            SyncOriginPacket.STREAM_CODEC,
            SyncOriginPacket::handle
        );

        reg.playToServer(
            ActivateAbilityPacket.TYPE,
            ActivateAbilityPacket.STREAM_CODEC,
            ActivateAbilityPacket::handle
        );
    }

    /** Sync a player's full origin state to themselves (called after any state change). */
    public static void syncPlayer(ServerPlayer player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        SyncOriginPacket pkt = new SyncOriginPacket(
            player.getName().getString(),
            origin != null ? origin.getId().toString() : "",
            data.getResource(),
            data.getCooldown1(),
            data.getCooldown2(),
            data.getOriginLevel(),
            data.getOriginXp()
        );

        PacketDistributor.sendToPlayer(player, pkt);
    }

    /** Sync a player's state to all players in range (useful for HUD of other players). */
    public static void syncPlayerToAll(ServerPlayer player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        SyncOriginPacket pkt = new SyncOriginPacket(
            player.getName().getString(),
            origin != null ? origin.getId().toString() : "",
            data.getResource(),
            data.getCooldown1(),
            data.getCooldown2(),
            data.getOriginLevel(),
            data.getOriginXp()
        );

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, pkt);
    }
}
