package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.ClientOriginData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class VampireHudHandler {

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Origin origin = ClientOriginData.getOrigin();
        if (origin == null) origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        if (!origin.getId().getPath().equals("vampire")) return;

        // Showing hunger for vampires makes no sense since it's locked at 20.
        // Their blood is the HUD's resource bar instead (see ResourceBarRenderer).
        event.setCanceled(true);
    }
}
