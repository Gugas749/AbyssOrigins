package com.gugas749.abyssorigins.client.vfx;

import com.gugas749.abyssorigins.Abyssorigins;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Loads the three AbyssOrigins VFX core shaders (assets/abyssorigins/shaders/core). */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class VfxShaders {

    public static ShaderInstance band;
    public static ShaderInstance field;
    public static ShaderInstance voidMatter;

    private VfxShaders() {}

    public static boolean ready() { return band != null && field != null && voidMatter != null; }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), rl("abyss_band"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> band = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), rl("abyss_field"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> field = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), rl("abyss_void"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> voidMatter = s);
        } catch (Exception e) {
            Abyssorigins.LOGGER.error("[AbyssOrigins] Failed to load VFX shaders — 3D ability effects disabled", e);
        }
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path);
    }
}
