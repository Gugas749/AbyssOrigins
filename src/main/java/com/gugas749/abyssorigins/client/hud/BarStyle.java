package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * A look for the origin resource bar. Art styles are pairs of textures in textures/gui/bars
 * (made by bars/extract_bars.py); each origin offers the ones drawn in its colours, with its
 * signature style first.
 *
 * @param texHeight height of the art in texture pixels (all art is {@link #TEX_WIDTH} wide)
 * @param fillStart texture column where the fill starts: everything left of it, like an end
 *                  cap, lights up as soon as there is any resource at all
 * @param fillEnd   texture column where the fill ends: everything right of it only lights up at 100%
 */
public record BarStyle(String id, Kind kind, int texHeight, int fillStart, int fillEnd) {

    public enum Kind { CLASSIC, DROPLETS, ART }

    public static final int TEX_WIDTH = 348;

    public static final BarStyle CLASSIC = new BarStyle("classic", Kind.CLASSIC, 0, 0, 0);
    public static final BarStyle DROPLETS = new BarStyle("droplets", Kind.DROPLETS, 0, 0, 0);

    private static final BarStyle
        HELLFORGED = art("hellforged", 37, 40, 312),
        CINDER     = art("cinder", 22, 20, 325),
        CROWNED    = art("crowned", 34, 8, 336),
        THORNED    = art("thorned", 19, 10, 336),
        SEGMENTED  = art("segmented", 19, 8, 337),
        GILDED     = art("gilded", 28, 20, 325),
        ASCENDANT  = art("ascendant", 25, 20, 330),
        RIFTSPINE  = art("riftspine", 28, 20, 330),
        HOLLOW     = art("hollow", 23, 16, 334),
        UMBRAL     = art("umbral", 23, 18, 330),
        TIDEWING   = art("tidewing", 38, 30, 305),
        CIRCUIT    = art("circuit", 22, 6, 341),
        SOVEREIGN  = art("sovereign", 34, 8, 336);

    private static final Map<String, List<BarStyle>> BY_ORIGIN = Map.of(
        "demon",    List.of(HELLFORGED, CINDER, CLASSIC),
        "vampire",  List.of(DROPLETS, CROWNED, THORNED, SEGMENTED),
        "seraph",   List.of(GILDED, ASCENDANT, CLASSIC),
        "voidborn", List.of(RIFTSPINE, HOLLOW, CLASSIC),
        "shade",    List.of(UMBRAL, TIDEWING, CIRCUIT, CLASSIC),
        "eminence", List.of(SOVEREIGN, GILDED, CLASSIC)
    );

    private static BarStyle art(String id, int texHeight, int fillStart, int fillEnd) {
        return new BarStyle(id, Kind.ART, texHeight, fillStart, fillEnd);
    }

    public static List<BarStyle> forOrigin(Origin origin) {
        boolean ours = origin.getId().getNamespace().equals(Abyssorigins.MODID);
        return ours ? BY_ORIGIN.getOrDefault(origin.getId().getPath(), List.of(CLASSIC)) : List.of(CLASSIC);
    }

    /** The style the player chose for this origin, or its signature one. */
    public static BarStyle current(Origin origin) {
        List<BarStyle> styles = forOrigin(origin);
        String chosen = AbyssOriginsConfig.barStyles.get(origin.getId().toString());
        for (BarStyle style : styles) if (style.id.equals(chosen)) return style;
        return styles.get(0);
    }

    public static void choose(Origin origin, BarStyle style) {
        AbyssOriginsConfig.barStyles.put(origin.getId().toString(), style.id);
    }

    public Component displayName() {
        return Component.translatable("gui.abyssorigins.bar_style." + id);
    }

    public ResourceLocation fullTexture() { return texture("full"); }

    public ResourceLocation emptyTexture() { return texture("empty"); }

    private ResourceLocation texture(String layer) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "textures/gui/bars/" + id + "_" + layer + ".png");
    }

    /** How many texture columns to show lit for a fill of 0..1. */
    public int litColumns(float pct) {
        if (pct <= 0f) return 0;
        if (pct >= 1f) return TEX_WIDTH;
        return fillStart + Math.round(pct * (fillEnd - fillStart));
    }
}
