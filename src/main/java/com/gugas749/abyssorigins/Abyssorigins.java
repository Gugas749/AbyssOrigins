package com.gugas749.abyssorigins;

import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.hiddenpower.Portals;
import com.gugas749.abyssorigins.item.ModCreativeTab;
import com.gugas749.abyssorigins.item.ModDataComponents;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.item.gear.ModArmorMaterials;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.registry.OriginRegistry;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abysscore.api.overlay.AbyssOverlaySpec;
import com.gugas749.abysscore.api.overlay.AbyssScreenOverlay;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(Abyssorigins.MODID)
public class Abyssorigins {

    public static final String MODID = "abyssorigins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Abyssorigins(IEventBus modEventBus, ModContainer modContainer) {
        AbyssOriginsConfig.migrateOldFile();
        modContainer.registerConfig(ModConfig.Type.CLIENT, AbyssOriginsConfig.CLIENT_SPEC, AbyssOriginsConfig.CLIENT_FILE);

        OriginAttachments.register(modEventBus);
        SkillAttachments.register(modEventBus);
        SpecializationAttachments.register(modEventBus);
        ModEffects.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTab.register(modEventBus);

        // Presence: kneeling dims the world (not to black); the awed hold glints gold at the edges
        AbyssScreenOverlay.bindEffect(ModEffects.KNEELING, AbyssOverlaySpec.tint(0x07040C, 0.34f).fade(20, 30));
        AbyssScreenOverlay.bindEffect(ModEffects.AWED, AbyssOverlaySpec.vignette(0xC89A2E, 0.45f).fade(10, 15));
        // Punishment: a crucified player's sight closes in, blood-dark at the edges
        AbyssScreenOverlay.bindEffect(ModEffects.CRUCIFIED, AbyssOverlaySpec.vignette(0x3A0000, 0.6f).fade(10, 20));

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPackets);

        LOGGER.info("[AbyssOrigins] Initialized.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            OriginRegistry.registerAll();
            SpecializationRegistry.registerAll();
            AbilityStates.addTimerSource(Portals::timers);   // an open portal's time left, on its creator's HUD
        });
        LOGGER.info("[AbyssOrigins] Origins and classes registered.");
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        ModPackets.register(event);
    }
}
