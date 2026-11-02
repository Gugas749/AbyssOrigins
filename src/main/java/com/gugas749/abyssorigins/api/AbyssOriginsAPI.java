package com.gugas749.abyssorigins.api;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Public API for AbyssOrigins.
 *
 * Addon mods (or this mod itself) call registerOrigin() during FMLCommonSetupEvent.
 * All other systems read from here — nothing else needs to be touched.
 */
public class AbyssOriginsAPI {

    private static final Map<ResourceLocation, Origin> ORIGINS = new LinkedHashMap<>();

    // Runtime cache keyed by UUID — survives respawn because we key on UUID not entity
    private static final Map<UUID, Origin> PLAYER_ORIGINS = new HashMap<>();

    private AbyssOriginsAPI() {}

    // ---- Registration -----------------------------------------------

    public static void registerOrigin(Origin origin) {
        ORIGINS.put(origin.getId(), origin);
        AbilityRegistry.invalidate();
        Abyssorigins.LOGGER.info("[AbyssOrigins] Registered origin: {}", origin.getId());
    }

    // ---- Lookup -----------------------------------------------

    public static Origin getOrigin(ResourceLocation id) {
        return ORIGINS.get(id);
    }

    public static Collection<Origin> getAllOrigins() {
        return Collections.unmodifiableCollection(ORIGINS.values());
    }

    // ---- Player origin -----------------------------------------------

    /** Returns null if the player has no origin assigned. */
    public static Origin getPlayerOrigin(Player player) {
        return PLAYER_ORIGINS.get(player.getUUID());
    }

    public static boolean hasOrigin(Player player) {
        return PLAYER_ORIGINS.containsKey(player.getUUID());
    }

    /**
     * Sets (or clears) a player's origin.
     * Handles removing old passives, applying new ones, and attribute scaling.
     * Call only on the server side.
     */
    public static void setPlayerOrigin(Player player, Origin newOrigin) {
        Origin old = PLAYER_ORIGINS.get(player.getUUID());

        // Remove old origin effects
        if (old != null) {
            old.getPassives().forEach(p -> {
                try { p.onRemove(player); }
                catch (Exception e) { Abyssorigins.LOGGER.error("Error removing passive {}: {}", p.getId(), e.getMessage()); }
            });
        }

        // Update cache
        if (newOrigin != null) {
            PLAYER_ORIGINS.put(player.getUUID(), newOrigin);
        } else {
            PLAYER_ORIGINS.remove(player.getUUID());
        }

        // Apply new origin effects
        if (newOrigin != null) {
            newOrigin.getPassives().forEach(p -> {
                try { p.onEquip(player); }
                catch (Exception e) { Abyssorigins.LOGGER.error("Error equipping passive {}: {}", p.getId(), e.getMessage()); }
            });
        }

        Abyssorigins.LOGGER.info("[AbyssOrigins] Set origin {} for {}",
            newOrigin != null ? newOrigin.getId() : "null",
            player.getName().getString()
        );
    }

    /**
     * Client-side only: update local cache from a sync packet.
     * Does NOT trigger passives (server handles those).
     */
    public static void setPlayerOriginClient(Player player, Origin origin) {
        if (origin != null) {
            PLAYER_ORIGINS.put(player.getUUID(), origin);
        } else {
            PLAYER_ORIGINS.remove(player.getUUID());
        }
    }

    /** Called on player disconnect to free memory. */
    public static void unloadPlayer(UUID uuid) {
        PLAYER_ORIGINS.remove(uuid);
    }

    /** Called on client disconnect to prevent data bleeding between servers. */
    public static void clearClientCache() {
        PLAYER_ORIGINS.clear();
    }
}
