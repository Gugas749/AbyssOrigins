package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.ResourceType;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Abyssorigins.MODID)
public class OriginEventHandler {

    private static final int SYNC_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        ResourceLocation savedId = data.getOriginId();

        if (savedId != null) {
            Origin origin = AbyssOriginsAPI.getOrigin(savedId);
            if (origin != null) {
                AbyssOriginsAPI.setPlayerOrigin(player, origin);
                // Grant starting points if this is effectively a new assignment
                XpEventHandler.grantStartingPoints(player);
                Abyssorigins.LOGGER.info("[AbyssOrigins] Restored origin {} for {}",
                    savedId, player.getName().getString());
            } else {
                Abyssorigins.LOGGER.warn("[AbyssOrigins] Unknown saved origin {} for {} — clearing",
                    savedId, player.getName().getString());
                data.setOriginId(null);
            }
        }

        ModPackets.syncPlayer(player);
        ModPackets.syncSkills(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AbyssOriginsAPI.unloadPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        PlayerOriginData newData = sp.getData(OriginAttachments.PLAYER_ORIGIN.get());
        ResourceLocation savedId = newData.getOriginId();

        if (savedId != null) {
            Origin origin = AbyssOriginsAPI.getOrigin(savedId);
            if (origin != null) {
                AbyssOriginsAPI.setPlayerOrigin(sp, origin);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        data.tickCooldown1();
        data.tickCooldown2();

        ResourceType resource = origin.getResourceType();
        if (resource != null) {
            if (resource.getDrainPerTick() > 0) {
                data.drainResource(resource.getDrainPerTick());
                if (data.getResource() <= 0) onResourceDepleted(sp);
            }
            if (resource.getRegenPerTick() > 0) {
                data.addResource(resource.getRegenPerTick());
            }
        }

        origin.getPassives().forEach(passive -> {
            try { passive.onTick(player); }
            catch (Exception e) {
                Abyssorigins.LOGGER.error("Error ticking passive {} for {}: {}",
                    passive.getId(), player.getName().getString(), e.getMessage());
            }
        });

        if (player.tickCount % SYNC_INTERVAL == 0) {
            ModPackets.syncPlayer(sp);
        }
    }

    private static void onResourceDepleted(ServerPlayer player) {
        if (player.tickCount % 40 == 0) {
            player.hurt(player.damageSources().starve(), 1.0f);
        }
    }
}
