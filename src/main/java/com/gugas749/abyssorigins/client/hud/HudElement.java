package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abysscore.api.gui.hud.AbyssHud;
import com.gugas749.abysscore.api.gui.hud.AbyssHudElement;
import com.gugas749.abysscore.api.gui.hud.HudBounds;
import com.gugas749.abysscore.api.gui.hud.HudBox;
import com.gugas749.abysscore.api.gui.hud.HudPlacement;
import com.gugas749.abysscore.api.gui.hud.HudSetting;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.gui.AbyssTheme;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig.ElementLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of the Abyss HUD a player can move, resize, hide and configure in AbyssCore's HUD
 * editor ({@link OriginsHud}: /abyssoriginshud or the H key). Each one draws itself
 * ({@link OriginHudOverlay}) and lists its own settings for its gear popup; where it sits is
 * stored in {@link AbyssOriginsConfig}.
 */
public enum HudElement implements AbyssHudElement {
    PLATE("plate"),
    RESOURCE("resource"),
    ABILITIES("abilities");

    private static final int STYLE_PREVIEW_WIDTH = 100, STYLE_PREVIEW_HEIGHT = 12;

    private final String key;

    HudElement(String key) {
        this.key = key;
    }

    @Override
    public String id() {
        return key;
    }

    @Override
    public Component displayName() {
        return Component.translatable("gui.abyssorigins.hud_layout.element." + key);
    }

    /** Stored position and size (the config's live values). */
    public ElementLayout layout() {
        return switch (this) {
            case PLATE -> AbyssOriginsConfig.PLATE_LAYOUT;
            case RESOURCE -> AbyssOriginsConfig.RESOURCE_LAYOUT;
            case ABILITIES -> AbyssOriginsConfig.ABILITIES_LAYOUT;
        };
    }

    public boolean isVisible() {
        return switch (this) {
            case PLATE -> AbyssOriginsConfig.showOriginHud;
            case RESOURCE -> AbyssOriginsConfig.showResourceBar;
            case ABILITIES -> AbyssOriginsConfig.showAbilityIndicators;
        };
    }

    public void setVisible(boolean visible) {
        switch (this) {
            case PLATE -> AbyssOriginsConfig.showOriginHud = visible;
            case RESOURCE -> AbyssOriginsConfig.showResourceBar = visible;
            case ABILITIES -> AbyssOriginsConfig.showAbilityIndicators = visible;
        }
    }

    /** Only with an origin; humans have no resource, so no bar to place. */
    @Override
    public boolean isAvailable() {
        Origin origin = OriginHudOverlay.currentOrigin();
        return origin != null && (this != RESOURCE || origin.getResourceType() != null);
    }

    @Override
    public HudBounds bounds(boolean preview) {
        Origin origin = OriginHudOverlay.currentOrigin();
        if (origin == null) return HudBounds.ofSize(0, 0);
        int[] size = OriginHudOverlay.size(this, origin, preview);
        return HudBounds.ofSize(size[0], size[1]);
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean preview) {
        Origin origin = OriginHudOverlay.currentOrigin();
        if (origin != null) OriginHudOverlay.drawElement(g, this, origin, preview);
    }

    /** Every element starts in its AUTO spot (see {@link #autoBox}). */
    @Override
    public HudPlacement defaultPlacement() {
        return HudPlacement.auto();
    }

    @Override
    public HudBox autoBox(AbyssHud hud, float w, float h, boolean preview) {
        int sw = AbyssHud.screenWidth(), sh = AbyssHud.screenHeight();
        return switch (this) {
            case PLATE -> new HudBox(4, 4, w, h);
            case ABILITIES -> new HudBox(sw - w - 6, sh - h - 6, w, h);
            case RESOURCE -> {
                Origin origin = OriginHudOverlay.currentOrigin();
                if (origin != null && BarStyle.current(origin).kind() == BarStyle.Kind.DROPLETS) {
                    yield new HudBox(sw / 2 + 91 - ResourceBarRenderer.DROPLET_ROW_WIDTH, sh - 39, w, h);   // the hunger bar's place
                }
                HudBox plate = hud.box(PLATE, preview);   // docked to the plate, wherever it went
                float below = plate.bottom() + 1;
                yield new HudBox(plate.x(), below + h <= sh ? below : plate.y() - h - 1, w, h);
            }
        };
    }

    /** What this element's gear popup shows, under AbyssCore's Visible / Reset / Size. */
    @Override
    public List<HudSetting> settings() {
        List<HudSetting> settings = new ArrayList<>();
        switch (this) {
            case PLATE -> settings.add(HudSetting.toggle(Component.translatable("abyssorigins.config.showXpBar"),
                () -> AbyssOriginsConfig.showXpBar, on -> AbyssOriginsConfig.showXpBar = on));
            case RESOURCE -> {
                settings.add(HudSetting.toggle(Component.translatable("abyssorigins.config.showResourceValue"),
                    () -> AbyssOriginsConfig.showResourceValue, on -> AbyssOriginsConfig.showResourceValue = on));
                Origin origin = OriginHudOverlay.currentOrigin();
                if (origin != null && origin.getResourceType() != null) {
                    // One row per style this origin offers, each previewing the real bar
                    settings.add(HudSetting.choice(null, BarStyle.forOrigin(origin), () -> BarStyle.current(origin),
                        style -> BarStyle.choose(origin, style), BarStyle::displayName,
                        (g, style, x, y, w, h) -> ResourceBarRenderer.drawPreview(g, style, origin.getResourceType(),
                            x, y, w, h, previewResource() / 100f),
                        STYLE_PREVIEW_WIDTH, STYLE_PREVIEW_HEIGHT));
                }
            }
            case ABILITIES -> {
                settings.add(HudSetting.toggle(Component.translatable("abyssorigins.config.showCooldownOverlays"),
                    () -> AbyssOriginsConfig.showCooldownOverlays, on -> AbyssOriginsConfig.showCooldownOverlays = on));
                settings.add(HudSetting.toggle(Component.translatable("abyssorigins.config.showKeybindHints"),
                    () -> AbyssOriginsConfig.showKeybindHints, on -> AbyssOriginsConfig.showKeybindHints = on));
            }
        }
        return settings;
    }

    /** The popup's "Reset This" (placement is reset by AbyssCore): this element's details and bar style. */
    @Override
    public void resetSettings() {
        switch (this) {
            case PLATE -> AbyssOriginsConfig.showXpBar = true;
            case RESOURCE -> {
                AbyssOriginsConfig.showResourceValue = true;
                Origin origin = OriginHudOverlay.currentOrigin();
                if (origin != null) AbyssOriginsConfig.barStyles.remove(origin.getId().toString());
            }
            case ABILITIES -> {
                AbyssOriginsConfig.showCooldownOverlays = true;
                AbyssOriginsConfig.showKeybindHints = true;
            }
        }
    }

    /** The bar's fill sweeps while editing, so its full and empty looks are both on show. 0..100. */
    static float previewResource() {
        return 15f + 85f * AbyssTheme.pulse(8f);
    }
}
