package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.ResourceType;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Powers;
import com.gugas749.abyssorigins.skilltree.registry.SkillEffectHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Player lifecycle for origins + the shared power tick (sustained abilities and
 * origin/class PowerHooks). Class-specific lifecycle is in SpecializationEventHandler.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class OriginEventHandler {

    private static final int SYNC_INTERVAL = 20;

    @SubscribeEvent(priority = EventPriority.HIGH)
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
        // Stats are rebuilt after the class handler restores the spec (see SpecializationEventHandler).
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) AbilityStates.stopAll(sp);
        AbyssOriginsAPI.unloadPlayer(event.getEntity().getUUID());
        PowerState.unload(event.getEntity().getUUID());
    }

    /** End sustained abilities before the body is gone (bat-form flight, decoys...). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) AbilityStates.stopAll(sp);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
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

    /** The cloned body has fresh attributes — rebuild stats and top up to the new max health. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        SkillEffectHandler.refresh(sp);
        sp.setHealth(sp.getMaxHealth());
        ModPackets.syncAll(sp);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ModPackets.syncAll(sp);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        // Shared power tick — runs for origin and class alike
        AbilityStates.tick(sp);
        Loadouts.data(sp).tick();
        Powers.forEach(sp, h -> h.tick(sp));

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        ResourceType resource = origin.getResourceType();
        if (resource != null) {
            if (resource.getDrainPerTick() > 0) {
                float mult = Powers.product(sp, h -> h.resourceDrainMultiplier(sp));
                data.drainResource(resource.getDrainPerTick() * mult);
                if (data.getResource() <= 0 && "blood".equals(resource.getId())) onBloodDepleted(sp);
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

    /** Only blood starvation hurts — empty wrath / radiance / umbra simply can't pay for abilities. */
    private static void onBloodDepleted(ServerPlayer player) {
        if (player.tickCount % 40 == 0) {
            player.hurt(player.damageSources().starve(), 1.0f);
        }
    }
}
