package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.ResourceType;
import com.gugas749.abyssorigins.client.gui.AbyssTheme;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Draws the origin resource bar in any {@link BarStyle}, with its top-left corner at (0, 0). */
public final class ResourceBarRenderer {

    /** One texture pixel per screen pixel at GUI scale 3. */
    private static final float ART_SCALE = 1 / 3f;
    public static final int ART_WIDTH = Math.round(BarStyle.TEX_WIDTH * ART_SCALE);
    public static final int DROPLET_ROW_WIDTH = 81, DROPLET_ROW_HEIGHT = 9;
    private static final int CLASSIC_WIDTH = 136, CLASSIC_HEIGHT = 12;
    private static final int VALUE_GAP = 4;
    private static final String WIDEST_VALUE = "100";

    private static final ResourceLocation BLOOD_EMPTY      = bloodSprite("blood_empty");
    private static final ResourceLocation BLOOD_FULL       = bloodSprite("blood_full");
    private static final ResourceLocation BLOOD_HALF       = bloodSprite("blood_half");
    private static final ResourceLocation BLOOD_LESS_HALF  = bloodSprite("blood_less_half");
    private static final ResourceLocation BLOOD_WITHERED   = bloodSprite("blood_withered");
    private static final ResourceLocation BLOOD_ABSORPTION = bloodSprite("blood_absorption");
    private static final int DROP_SIZE = 9, DROP_SPACING = 8;

    private ResourceBarRenderer() {}

    private static ResourceLocation bloodSprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "origins/vampire_blood/hud_bar/" + name);
    }

    public static int artHeight(BarStyle style) {
        return Mth.ceil(style.texHeight() * ART_SCALE);
    }

    /** Unscaled size as {width, height}; stays fixed as the value changes, so anchored bars don't twitch. */
    public static int[] size(BarStyle style, Font font, boolean showValue) {
        int value = showValue ? VALUE_GAP + font.width(WIDEST_VALUE) : 0;
        return switch (style.kind()) {
            case CLASSIC -> new int[]{CLASSIC_WIDTH, CLASSIC_HEIGHT};
            case DROPLETS -> new int[]{DROPLET_ROW_WIDTH + value, DROPLET_ROW_HEIGHT};
            case ART -> new int[]{ART_WIDTH + value, Math.max(artHeight(style), showValue ? font.lineHeight : 0)};
        };
    }

    /**
     * A style drawn for real, shrunk to fit (x, y, w, h): the rows of the bar style choice in the
     * resource bar's settings popup.
     * @param pct the fill to show, 0..1
     */
    public static void drawPreview(GuiGraphics g, BarStyle style, ResourceType res, int x, int y, int w, int h, float pct) {
        int[] natural = switch (style.kind()) {
            case ART -> new int[]{ART_WIDTH, artHeight(style)};
            case DROPLETS -> new int[]{DROPLET_ROW_WIDTH, DROPLET_ROW_HEIGHT};
            case CLASSIC -> new int[]{w, 4};
        };
        float fit = Math.min(w / (float) natural[0], h / (float) natural[1]);
        g.pose().pushPose();
        g.pose().translate(x, y + (h - natural[1] * fit) / 2f, 0);
        g.pose().scale(fit, fit, 1f);
        switch (style.kind()) {
            case ART -> drawArt(g, style, pct);
            case DROPLETS -> drawDroplets(g, pct * 100f);
            case CLASSIC -> AbyssTheme.bar(g, 0, 0, natural[0], natural[1], pct, res.getBarColor(), true);
        }
        g.pose().popPose();
    }

    /** @param value the resource, 0..100 */
    public static void draw(GuiGraphics g, Font font, BarStyle style, ResourceType res, int accent, float value, boolean showValue) {
        int[] size = size(style, font, showValue);
        String number = String.valueOf(Math.round(value));
        switch (style.kind()) {
            case CLASSIC -> {
                AbyssTheme.panel(g, 0, 0, CLASSIC_WIDTH, CLASSIC_HEIGHT, accent, 1f);
                int barWidth = CLASSIC_WIDTH - (showValue ? 34 : 12);
                AbyssTheme.bar(g, 6, 4, barWidth, 4, value / 100f, res.getBarColor(), true);
                if (showValue) {
                    g.drawString(font, number, CLASSIC_WIDTH - 5 - font.width(number), 2, res.getBarColor(), true);
                }
            }
            case DROPLETS -> {
                drawDroplets(g, value);
                if (showValue) {
                    g.drawString(font, number, DROPLET_ROW_WIDTH + VALUE_GAP, 1, bloodNumberColour(value), true);
                }
            }
            case ART -> {
                int artTop = (size[1] - artHeight(style)) / 2;
                g.pose().pushPose();
                g.pose().translate(0, artTop, 0);
                drawArt(g, style, value / 100f);
                g.pose().popPose();
                if (showValue) {
                    g.drawString(font, number, ART_WIDTH + VALUE_GAP, (size[1] - font.lineHeight) / 2 + 1, res.getBarColor(), true);
                }
            }
        }
    }

    /** The art alone, {@link #ART_WIDTH} wide with its top-left at (0, 0). */
    public static void drawArt(GuiGraphics g, BarStyle style, float pct) {
        int lit = style.litColumns(pct);
        int w = BarStyle.TEX_WIDTH, h = style.texHeight();
        var textures = Minecraft.getInstance().getTextureManager();
        textures.getTexture(style.emptyTexture()).setFilter(true, false);   // smooth at every GUI scale
        textures.getTexture(style.fullTexture()).setFilter(true, false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        g.pose().scale(ART_SCALE, ART_SCALE, 1f);
        if (lit < w) g.blit(style.emptyTexture(), 0, 0, 0, 0, w, h, w, h);
        if (lit > 0) g.blit(style.fullTexture(), 0, 0, 0, 0, lit, h, w, h);
        if (lit > 3 && lit < w) {   // brighter leading edge on the fill
            float[] tint = RenderSystem.getShaderColor().clone();
            RenderSystem.setShaderColor(tint[0] * 1.7f, tint[1] * 1.7f, tint[2] * 1.7f, tint[3]);
            g.blit(style.fullTexture(), lit - 3, 0, lit - 3, 0, 3, h, w, h);
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
        }
        g.pose().popPose();
        RenderSystem.disableBlend();
    }

    /** The vampire's row of blood drops, drawn like the hunger bar it replaces. */
    public static void drawDroplets(GuiGraphics g, float blood) {
        Player player = Minecraft.getInstance().player;
        float saturation = player != null ? player.getFoodData().getSaturationLevel() : 0f;
        boolean withered = player != null && player.hasEffect(MobEffects.HUNGER);

        int bloodPoints = (int) (blood / 5f);
        int absorptionPoints = Math.min(20, (int) (saturation / 2.5f));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 9; i >= 0; i--) {
            int ix = i * DROP_SPACING;
            int iy = blood < 10f && (System.currentTimeMillis() / 250 + i) % 3 == 0 ? -1 : 0;

            g.blitSprite(withered ? BLOOD_WITHERED : BLOOD_EMPTY, ix, iy, DROP_SIZE, DROP_SIZE);

            int pts = bloodPoints - i * 2;
            if (pts >= 2) {
                g.blitSprite(BLOOD_FULL, ix, iy, DROP_SIZE, DROP_SIZE);
            } else if (pts == 1) {
                g.blitSprite(BLOOD_HALF, ix, iy, DROP_SIZE, DROP_SIZE);
            } else if (pts == 0 && blood > 0 && blood < 5f) {
                g.blitSprite(BLOOD_LESS_HALF, ix, iy, DROP_SIZE, DROP_SIZE);
            }

            if (saturation > 0 && absorptionPoints - i * 2 >= 2) {
                g.blitSprite(BLOOD_ABSORPTION, ix, iy, DROP_SIZE, DROP_SIZE);
            }
        }
        RenderSystem.disableBlend();
    }

    private static int bloodNumberColour(float blood) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(MobEffects.HUNGER)) return 0xFF556600;
        if (blood < 20f && (System.currentTimeMillis() / 500) % 2 == 0) return 0xFFFF0000;
        return 0xFFAA0000;
    }
}
