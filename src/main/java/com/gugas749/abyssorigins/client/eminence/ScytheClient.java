package com.gugas749.abyssorigins.client.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.origin.eminence.ScytheCombo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client wiring for the Sovereign Scythe: first-person transforms and counting every wielder's swings. */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class ScytheClient {

    private ScytheClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Player p : mc.level.players())
            if (ScytheCombo.swingStarted(p) && ScytheAnimations.holds(p)) ScytheCombo.advance(p);
    }

    @EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(ScytheFirstPerson.INSTANCE, ModItems.SOVEREIGN_SCYTHE.get());
        }
    }
}
