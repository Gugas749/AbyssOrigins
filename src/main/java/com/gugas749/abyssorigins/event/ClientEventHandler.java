package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.client.ClientIdentities;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.ClientTimers;
import com.gugas749.abyssorigins.client.ascension.AscensionTitle;
import com.gugas749.abyssorigins.client.hud.AbyssVignetteOverlay;
import com.gugas749.abyssorigins.client.hud.OriginHudOverlay;
import com.gugas749.abyssorigins.client.reveal.RevealClient;
import com.gugas749.abyssorigins.client.ritual.ClientRitual;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.item.ritual.OriginSeedItem;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.gugas749.abysscore.api.vfx.client.AbyssVfxClient;
import com.gugas749.abyssorigins.client.vfx.AscensionEffect;
import com.gugas749.abyssorigins.client.vfx.AtomicEffect;
import com.gugas749.abyssorigins.vfx.OriginVfx;
import com.gugas749.abyssorigins.block.ModBlocks;
import com.gugas749.abyssorigins.client.block.PunishCrossRenderer;
import com.gugas749.abyssorigins.client.entity.DementorRenderer;
import com.gugas749.abyssorigins.entity.ModEntities;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        AbyssOriginsAPI.clearClientCache();
        ClientOriginData.clear();
        ClientSpecializationData.clear();
        ClientTimers.clear();
        com.gugas749.abyssorigins.client.eminence.EminenceClientEffects.clear();
        AscensionTitle.clear();
        ClientIdentities.clear();
        RevealClient.clear();
        ClientRitual.clear();
        Abyssorigins.LOGGER.debug("[AbyssOrigins] Cleared client caches on disconnect.");
    }

    @EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
    public static class ModEvents {
        /** AbyssOrigins' own effect types on AbyssCore's VFX engine (the built-in ones need nothing). */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                AbyssVfxClient.register(OriginVfx.ATOMIC, AtomicEffect::new);
                AbyssVfxClient.register(OriginVfx.ASCENSION, AscensionEffect::new);
            });
        }

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            // Vignette sits under the vanilla HUD so hearts/hotbar stay readable
            event.registerBelow(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "abyss_vignette"),
                new AbyssVignetteOverlay());
            event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "origin_hud"),
                new OriginHudOverlay()
            );
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.DEMENTOR.get(), DementorRenderer::new);
            event.registerEntityRenderer(ModEntities.ABYSS_MIRE.get(), com.gugas749.abyssorigins.client.entity.AbyssMireRenderer::new);
            event.registerEntityRenderer(ModEntities.THROWN_SCYTHE.get(), com.gugas749.abyssorigins.client.entity.ThrownScytheRenderer::new);
            event.registerEntityRenderer(ModEntities.JUDGEMENT_SWORD.get(), com.gugas749.abyssorigins.client.entity.JudgementSwordRenderer::new);
            event.registerBlockEntityRenderer(ModBlocks.PUNISH_CROSS_BE.get(), PunishCrossRenderer::new);
        }

        /** Seeds are tinted with their origin's colour (layer 0; the glint layer stays untinted). */
        @SubscribeEvent
        public static void onItemColors(RegisterColorHandlersEvent.Item event) {
            event.register((stack, layer) -> layer == 0 ? OriginSeedItem.tint(stack) : -1, ModItems.ORIGIN_SEED.get());
        }
    }
}
