package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.ClientOriginData;
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

/**
 * Cancels the vanilla hunger bar for vampires and renders a blood bar in its place.
 *
 * Sprites live at:
 *   assets/abyssorigins/textures/gui/sprites/origins/vampire_blood/hud_bar/blood_*.png
 *
 * The blitSprite path is the ResourceLocation WITHOUT the textures/ prefix and WITHOUT .png.
 * NeoForge's GuiSpriteManager resolves  "abyssorigins:origins/vampire_blood/hud_bar/blood_full"
 * to  assets/abyssorigins/textures/gui/sprites/origins/vampire_blood/hud_bar/blood_full.png
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class VampireHudHandler {

    private static final ResourceLocation BLOOD_EMPTY    = rl("blood_empty");
    private static final ResourceLocation BLOOD_FULL     = rl("blood_full");
    private static final ResourceLocation BLOOD_HALF     = rl("blood_half");
    private static final ResourceLocation BLOOD_LESS_HALF= rl("blood_less_half");
    private static final ResourceLocation BLOOD_WITHERED = rl("blood_withered");
    private static final ResourceLocation BLOOD_ABSORPTION = rl("blood_absorption");

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

        event.setCanceled(true);

        if (!mc.options.hideGui)
            renderBloodBar(event.getGuiGraphics(), mc, player);
    }

    private static void renderBloodBar(GuiGraphics gfx, Minecraft mc, Player player) {
        float blood = ClientOriginData.getResourceBar();
        float saturation = player.getFoodData().getSaturationLevel();

        boolean hasHungerEffect = player.hasEffect(MobEffects.HUNGER);
        boolean isCriticallyLow = blood < 20f;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int rightEdge = sw / 2 + 91;
        int y = sh - 39;

        // 0-100 → 0-20 blood points, saturation → 0-20 absorption points
        int bloodPoints = (int)(blood / 5f);
        int absorptionPoints = Math.min(20, (int)(saturation / 2.5f));

        for (int i = 9; i >= 0; i--) {
            int ix = rightEdge - (10 - i) * ICON_SPACING - ICON_SIZE;
            int iy = y;

            // Bounce when critically low
            if (isCriticallyLow && blood < 10f && (System.currentTimeMillis() / 250 + i) % 3 == 0)
                iy -= 1;

            // Background
            ResourceLocation bg = hasHungerEffect ? BLOOD_WITHERED : BLOOD_EMPTY;
            gfx.blitSprite(bg, ix, iy, ICON_SIZE, ICON_SIZE);

            // Fill
            int pts = bloodPoints - (i * 2);
            if (pts >= 2) {
                gfx.blitSprite(BLOOD_FULL, ix, iy, ICON_SIZE, ICON_SIZE);
            } else if (pts == 1) {
                gfx.blitSprite(BLOOD_HALF, ix, iy, ICON_SIZE, ICON_SIZE);
            } else if (pts == 0 && bloodPoints == i * 2 && blood > 0 && blood < 5f * (i * 2 + 1)) {
                // below half of this slot
                gfx.blitSprite(BLOOD_LESS_HALF, ix, iy, ICON_SIZE, ICON_SIZE);
            }

            // Absorption overlay
            if (saturation > 0 && (absorptionPoints - i * 2) >= 2)
                gfx.blitSprite(BLOOD_ABSORPTION, ix, iy, ICON_SIZE, ICON_SIZE);
        }

        // Numeric value
        String text = String.format("%.0f", blood);
        int textColor;
        if (hasHungerEffect)                                          textColor = 0xFF556600;
        else if (isCriticallyLow && (System.currentTimeMillis()/500)%2==0) textColor = 0xFFFF0000;
        else                                                           textColor = 0xFFAA0000;

        gfx.drawString(font, text, rightEdge + 4, y, textColor, true);

        if (saturation > 5)
            gfx.drawString(font, "+" + String.format("%.0f", saturation), rightEdge + 4, y - 10, 0xFFFFDD00, true);
    }
}
