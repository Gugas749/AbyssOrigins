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

    // How often (in ticks) to sync resource to client during passive drain
    private static final int SYNC_INTERVAL = 20; // once per second

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        ResourceLocation savedId = data.getOriginId();

        if (savedId != null) {
            Origin origin = AbyssOriginsAPI.getOrigin(savedId);
            if (origin != null) {
                // Restore to API cache and re-apply passives
                AbyssOriginsAPI.setPlayerOrigin(player, origin);
                Abyssorigins.LOGGER.info("[AbyssOrigins] Restored origin {} for {}",
                    savedId, player.getName().getString());
            } else {
                Abyssorigins.LOGGER.warn("[AbyssOrigins] Unknown saved origin {} for {} — clearing",
                    savedId, player.getName().getString());
                data.setOriginId(null);
            }
        }

        // Always sync state to client on join
        ModPackets.syncPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AbyssOriginsAPI.unloadPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Preserve data across death (attachment has copyOnDeath, but we also
        // need to re-apply passives because the new entity is fresh)
        if (!event.isWasDeath()) return;

        PlayerOriginData newData = event.getEntity().getData(OriginAttachments.PLAYER_ORIGIN.get());
        ResourceLocation savedId = newData.getOriginId();

        if (savedId != null && event.getEntity() instanceof ServerPlayer sp) {
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

        // Tick cooldowns
        data.tickCooldown1();
        data.tickCooldown2();

        // Passive resource drain
        ResourceType resource = origin.getResourceType();
        if (resource != null && resource.getDrainPerTick() > 0) {
            data.drainResource(resource.getDrainPerTick());

            // If resource hits 0, apply starvation-like effect
            if (data.getResource() <= 0) {
                onResourceDepleted(sp, origin);
            }
        }

        // Passive resource regen
        if (resource != null && resource.getRegenPerTick() > 0) {
            data.addResource(resource.getRegenPerTick());
        }

        // Tick passives
        origin.getPassives().forEach(passive -> {
            try { passive.onTick(player); }
            catch (Exception e) {
                Abyssorigins.LOGGER.error("Error ticking passive {} for {}: {}",
                    passive.getId(), player.getName().getString(), e.getMessage());
            }
        });

        // Sync to client once per second
        if (player.tickCount % SYNC_INTERVAL == 0) {
            ModPackets.syncPlayer(sp);
        }
    }

    private static void onResourceDepleted(ServerPlayer player, Origin origin) {
        // Default: hurt player for running out of their resource
        // Specific origins can override this behaviour in their passives
        if (player.tickCount % 40 == 0) { // every 2 seconds
            player.hurt(player.damageSources().starve(), 1.0f);
        }
    }
}
