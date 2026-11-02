package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.network.packet.IdentityPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Which origin each known player has, as broadcast by the server. */
public final class ClientIdentities {

    private static final Map<UUID, ResourceLocation> ORIGINS = new HashMap<>();

    private ClientIdentities() {}

    public static void apply(List<IdentityPacket.Entry> entries) {
        for (IdentityPacket.Entry e : entries) {
            if (e.originId().isEmpty()) ORIGINS.remove(e.player());
            else ORIGINS.put(e.player(), ResourceLocation.parse(e.originId()));
        }
    }

    public static Origin originOf(UUID player) {
        ResourceLocation id = ORIGINS.get(player);
        return id == null ? null : AbyssOriginsAPI.getOrigin(id);
    }

    public static void clear() { ORIGINS.clear(); }
}
