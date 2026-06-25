package com.gugas749.abyssorigins.config;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side config for AbyssOrigins.
 * Registered in Abyssorigins.java as ModConfig.Type.CLIENT.
 *
 * Stored in: config/abyssorigins-client.toml
 * Accessible in-game via the H key (HudConfigScreen).
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbyssOriginsConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- HUD toggles ---------------------------------------------------

    public static final ModConfigSpec.BooleanValue SHOW_ORIGIN_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_RESOURCE_BAR;
    public static final ModConfigSpec.BooleanValue SHOW_XP_BAR;
    public static final ModConfigSpec.BooleanValue SHOW_ABILITY_INDICATORS;
    public static final ModConfigSpec.BooleanValue SHOW_COOLDOWN_OVERLAYS;
    public static final ModConfigSpec.BooleanValue SHOW_KEYBIND_HINTS;

    // ---- HUD positioning -----------------------------------------------

    public enum HudPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public static final ModConfigSpec.EnumValue<HudPosition> ORIGIN_INFO_POSITION;
    public static final ModConfigSpec.EnumValue<HudPosition> ABILITY_INDICATORS_POSITION;

    // ---- Blood bar ------------------------------------------------------

    public static final ModConfigSpec.BooleanValue SHOW_BLOOD_NUMERIC;

    // ---- Spec -----------------------------------------------------------

    public static final ModConfigSpec CLIENT_SPEC;

    static {
        BUILDER.push("HUD");
        BUILDER.comment("Toggle and position individual HUD elements.");

        SHOW_ORIGIN_HUD = BUILDER
            .comment("Show the origin info panel (name, level, XP bar) in the top-left.")
            .translation("abyssorigins.config.showOriginHud")
            .define("showOriginHud", true);

        SHOW_RESOURCE_BAR = BUILDER
            .comment("Show the origin resource bar (blood bar, etc.) in the hotbar area.")
            .translation("abyssorigins.config.showResourceBar")
            .define("showResourceBar", true);

        SHOW_XP_BAR = BUILDER
            .comment("Show the origin XP bar inside the origin info panel.")
            .translation("abyssorigins.config.showXpBar")
            .define("showXpBar", true);

        SHOW_ABILITY_INDICATORS = BUILDER
            .comment("Show ability cooldown boxes on the right side of the screen.")
            .translation("abyssorigins.config.showAbilityIndicators")
            .define("showAbilityIndicators", true);

        SHOW_COOLDOWN_OVERLAYS = BUILDER
            .comment("Show the cooldown fill animation inside ability boxes.")
            .translation("abyssorigins.config.showCooldownOverlays")
            .define("showCooldownOverlays", true);

        SHOW_KEYBIND_HINTS = BUILDER
            .comment("Show [R] / [V] keybind hints below ability boxes.")
            .translation("abyssorigins.config.showKeybindHints")
            .define("showKeybindHints", true);

        ORIGIN_INFO_POSITION = BUILDER
            .comment("Screen corner for the origin info panel.")
            .translation("abyssorigins.config.originInfoPosition")
            .defineEnum("originInfoPosition", HudPosition.TOP_LEFT);

        ABILITY_INDICATORS_POSITION = BUILDER
            .comment("Screen corner for ability cooldown indicators.")
            .translation("abyssorigins.config.abilityIndicatorsPosition")
            .defineEnum("abilityIndicatorsPosition", HudPosition.BOTTOM_RIGHT);

        SHOW_BLOOD_NUMERIC = BUILDER
            .comment("Show the numeric blood value next to the blood bar.")
            .translation("abyssorigins.config.showBloodNumeric")
            .define("showBloodNumeric", true);

        BUILDER.pop();
        CLIENT_SPEC = BUILDER.build();
    }

    // ---- Cached runtime values -----------------------------------------

    public static boolean showOriginHud          = true;
    public static boolean showResourceBar        = true;
    public static boolean showXpBar              = true;
    public static boolean showAbilityIndicators  = true;
    public static boolean showCooldownOverlays   = true;
    public static boolean showKeybindHints       = true;
    public static boolean showBloodNumeric       = true;
    public static HudPosition originInfoPosition         = HudPosition.TOP_LEFT;
    public static HudPosition abilityIndicatorsPosition  = HudPosition.BOTTOM_RIGHT;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != CLIENT_SPEC) return;

        showOriginHud         = SHOW_ORIGIN_HUD.get();
        showResourceBar       = SHOW_RESOURCE_BAR.get();
        showXpBar             = SHOW_XP_BAR.get();
        showAbilityIndicators = SHOW_ABILITY_INDICATORS.get();
        showCooldownOverlays  = SHOW_COOLDOWN_OVERLAYS.get();
        showKeybindHints      = SHOW_KEYBIND_HINTS.get();
        showBloodNumeric      = SHOW_BLOOD_NUMERIC.get();
        originInfoPosition         = ORIGIN_INFO_POSITION.get();
        abilityIndicatorsPosition  = ABILITY_INDICATORS_POSITION.get();

        Abyssorigins.LOGGER.info("[AbyssOrigins] Client config loaded.");
    }
}
