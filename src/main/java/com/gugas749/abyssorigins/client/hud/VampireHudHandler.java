package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class VampireHudHandler {

    private static final ResourceLocation BLOOD_EMPTY     = rl("blood_empty");
    private static final ResourceLocation BLOOD_FULL      = rl("blood_full");
    private static final ResourceLocation BLOOD_HALF      = rl("blood_half");
    private static final ResourceLocation BLOOD_LESS_HALF = rl("blood_less_half");
    private static final ResourceLocation BLOOD_WITHERED  = rl("blood_withered");
    private static final ResourceLocation BLOOD_ABSORPTION= rl("blood_absorption");

    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID,
            "origins/vampire_blood/hud_bar/" + name);
    }

    private static final int ICON_SIZE    = 9;
    private static final int ICON_SPACING = 8;

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

        // Always cancel vanilla hunger bar for vampires regardless of config
        // (showing hunger for vampires makes no sense since it's locked at 20)
        event.setCanceled(true);

        // But only render blood bar if config allows it
        if (!mc.options.hideGui && AbyssOriginsConfig.showResourceBar) {
            renderBloodBar(event.getGuiGraphics(), mc, player);
        }
    }

    private static void renderBloodBar(GuiGraphics gfx, Minecraft mc, Player player) {
        float blood     = ClientOriginData.getResourceBar();
        float saturation= player.getFoodData().getSaturationLevel();
        boolean withered= player.hasEffect(MobEffects.HUNGER);
        boolean critical= blood < 20f;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int rightEdge = sw / 2 + 91;
        int y = sh - 39;

        int bloodPoints     = (int)(blood / 5f);
        int absorptionPoints= Math.min(20, (int)(saturation / 2.5f));

        for (int i = 9; i >= 0; i--) {
            int ix = rightEdge - (10 - i) * ICON_SPACING - ICON_SIZE;
            int iy = y;

            if (critical && blood < 10f && (System.currentTimeMillis() / 250 + i) % 3 == 0)
                iy -= 1;

            gfx.blitSprite(withered ? BLOOD_WITHERED : BLOOD_EMPTY, ix, iy, ICON_SIZE, ICON_SIZE);

            int pts = bloodPoints - (i * 2);
            if (pts >= 2) {
                gfx.blitSprite(BLOOD_FULL, ix, iy, ICON_SIZE, ICON_SIZE);
            } else if (pts == 1) {
                gfx.blitSprite(BLOOD_HALF, ix, iy, ICON_SIZE, ICON_SIZE);
            } else if (pts == 0 && blood > 0 && blood < 5f) {
                gfx.blitSprite(BLOOD_LESS_HALF, ix, iy, ICON_SIZE, ICON_SIZE);
            }

            if (saturation > 0 && (absorptionPoints - i * 2) >= 2)
                gfx.blitSprite(BLOOD_ABSORPTION, ix, iy, ICON_SIZE, ICON_SIZE);
        }

        // Numeric value — only if config says so
        if (AbyssOriginsConfig.showBloodNumeric) {
            String text = String.format("%.0f", blood);
            int textColor;
            if (withered)                                                    textColor = 0xFF556600;
            else if (critical && (System.currentTimeMillis() / 500) % 2 == 0) textColor = 0xFFFF0000;
            else                                                              textColor = 0xFFAA0000;
            gfx.drawString(font, text, rightEdge + 4, y, textColor, true);
        }
    }
}
