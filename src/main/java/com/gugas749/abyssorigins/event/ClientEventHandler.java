package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        AbyssOriginsAPI.clearClientCache();
        Abyssorigins.LOGGER.debug("[AbyssOrigins] Cleared client origin cache on disconnect.");
    }
}
