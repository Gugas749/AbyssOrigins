package com.gugas749.abyssorigins.config;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side config for AbyssOrigins, stored in config/abyssorigins/client.toml.
 * Registered in Abyssorigins.java as ModConfig.Type.CLIENT.
 *
 * The public non-final fields are the live values the HUD reads. The layout editor
 * (/abyssoriginshud or the H key) changes only those, so {@link #save()} commits an edit
 * and {@link #reload()} throws it away.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class AbyssOriginsConfig {

    /** Relative to the config folder; every config file of this mod lives in that subfolder. */
    public static final String CLIENT_FILE = Abyssorigins.MODID + "/client.toml";
    private static final String OLD_CLIENT_FILE = Abyssorigins.MODID + "-client.toml";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- HUD toggles ---------------------------------------------------

    private static final ModConfigSpec.BooleanValue SHOW_ORIGIN_HUD;
    private static final ModConfigSpec.BooleanValue SHOW_RESOURCE_BAR;
    private static final ModConfigSpec.BooleanValue SHOW_XP_BAR;
    private static final ModConfigSpec.BooleanValue SHOW_ABILITY_INDICATORS;
    private static final ModConfigSpec.BooleanValue SHOW_COOLDOWN_OVERLAYS;
    private static final ModConfigSpec.BooleanValue SHOW_KEYBIND_HINTS;
    private static final ModConfigSpec.BooleanValue SHOW_RESOURCE_VALUE;

    // ---- Bar styles -----------------------------------------------------

    private static final ModConfigSpec.ConfigValue<List<? extends String>> BAR_STYLES;

    // ---- Spec -----------------------------------------------------------

    /** Where an element sits; offsets are measured from this point of the screen. AUTO = the default spot. */
    public enum HudAnchor {
        AUTO(0, 0),
        TOP_LEFT(0, 0), TOP_CENTER(0.5f, 0), TOP_RIGHT(1, 0),
        CENTER_LEFT(0, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1, 0.5f),
        BOTTOM_LEFT(0, 1), BOTTOM_CENTER(0.5f, 1), BOTTOM_RIGHT(1, 1);

        public final float fx, fy;

        HudAnchor(float fx, float fy) {
            this.fx = fx;
            this.fy = fy;
        }

        /** The anchor for the screen third a point falls in (coordinates as 0..1 of the screen). */
        public static HudAnchor nearest(float x, float y) {
            int column = x < 1 / 3f ? 0 : x < 2 / 3f ? 1 : 2;
            int row = y < 1 / 3f ? 0 : y < 2 / 3f ? 1 : 2;
            return values()[1 + row * 3 + column];
        }
    }

    /** Position and size of one HUD element. */
    public static final class ElementLayout {
        private final ModConfigSpec.EnumValue<HudAnchor> anchorValue;
        private final ModConfigSpec.IntValue offsetXValue, offsetYValue;
        private final ModConfigSpec.DoubleValue scaleValue;

        public HudAnchor anchor = HudAnchor.AUTO;
        public int offsetX, offsetY;
        public float scale = 1f;

        private ElementLayout(String name) {
            BUILDER.push(name);
            anchorValue = BUILDER.comment("Screen point the offsets are measured from. AUTO keeps the default spot.")
                .defineEnum("anchor", HudAnchor.AUTO);
            offsetXValue = BUILDER.defineInRange("offsetX", 0, -8192, 8192);
            offsetYValue = BUILDER.defineInRange("offsetY", 0, -8192, 8192);
            scaleValue = BUILDER.defineInRange("scale", 1.0, MIN_SCALE, MAX_SCALE);
            BUILDER.pop();
        }

        private void load() {
            anchor = anchorValue.get();
            offsetX = offsetXValue.get();
            offsetY = offsetYValue.get();
            scale = scaleValue.get().floatValue();
        }

        private void store() {
            anchorValue.set(anchor);
            offsetXValue.set(offsetX);
            offsetYValue.set(offsetY);
            scaleValue.set((double) scale);
        }

        public void reset() {
            anchor = HudAnchor.AUTO;
            offsetX = offsetY = 0;
            scale = 1f;
        }
    }

    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.0f;

    public static final ElementLayout PLATE_LAYOUT, RESOURCE_LAYOUT, ABILITIES_LAYOUT;

    public static final ModConfigSpec CLIENT_SPEC;

    static {
        BUILDER.push("HUD");
        BUILDER.comment("Everything here can be changed in-game with /abyssoriginshud (or the H key).");

        SHOW_ORIGIN_HUD = BUILDER
            .comment("Show the origin info panel (name, level, XP bar).")
            .translation("abyssorigins.config.showOriginHud")
            .define("showOriginHud", true);

        SHOW_RESOURCE_BAR = BUILDER
            .comment("Show the origin resource bar (blood, wrath, umbra...).")
            .translation("abyssorigins.config.showResourceBar")
            .define("showResourceBar", true);

        SHOW_XP_BAR = BUILDER
            .comment("Show the origin XP bar inside the origin info panel.")
            .translation("abyssorigins.config.showXpBar")
            .define("showXpBar", true);

        SHOW_ABILITY_INDICATORS = BUILDER
            .comment("Show the ability cooldown boxes.")
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

        SHOW_RESOURCE_VALUE = BUILDER
            .comment("Show the number next to the resource bar.")
            .translation("abyssorigins.config.showResourceValue")
            .define("showResourceValue", true);

        BAR_STYLES = BUILDER
            .comment("The resource bar style chosen for each origin, as origin=style.")
            .defineListAllowEmpty("barStyles", List.of(), () -> "", entry -> entry instanceof String s && s.contains("="));

        BUILDER.push("layout");
        PLATE_LAYOUT = new ElementLayout("plate");
        RESOURCE_LAYOUT = new ElementLayout("resource");
        ABILITIES_LAYOUT = new ElementLayout("abilities");
        BUILDER.pop();

        BUILDER.pop();
        CLIENT_SPEC = BUILDER.build();
    }

    // ---- Live values -----------------------------------------------------

    public static boolean showOriginHud          = true;
    public static boolean showResourceBar        = true;
    public static boolean showXpBar              = true;
    public static boolean showAbilityIndicators  = true;
    public static boolean showCooldownOverlays   = true;
    public static boolean showKeybindHints       = true;
    public static boolean showResourceValue      = true;
    /** Origin id to chosen style id; an origin that isn't listed uses its signature style. */
    public static final Map<String, String> barStyles = new HashMap<>();

    /** Set while the layout editor is open, so a reload can't wipe the edit in progress. */
    public static boolean editing;
    private static volatile boolean saving;

    @SubscribeEvent
    static void onConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != CLIENT_SPEC) return;
        if (event instanceof ModConfigEvent.Loading) {
            reload();
            Abyssorigins.LOGGER.info("[AbyssOrigins] Client config loaded.");
        } else if (event instanceof ModConfigEvent.Reloading && !saving) {
            // The file changed, which every save also triggers about half a second later. This
            // arrives on a file watcher thread, and the live values belong to the game thread.
            Minecraft.getInstance().execute(() -> {
                if (!editing) reload();
            });
        }
    }

    /** Resets the live values to what the config file holds. */
    public static void reload() {
        showOriginHud         = SHOW_ORIGIN_HUD.get();
        showResourceBar       = SHOW_RESOURCE_BAR.get();
        showXpBar             = SHOW_XP_BAR.get();
        showAbilityIndicators = SHOW_ABILITY_INDICATORS.get();
        showCooldownOverlays  = SHOW_COOLDOWN_OVERLAYS.get();
        showKeybindHints      = SHOW_KEYBIND_HINTS.get();
        showResourceValue     = SHOW_RESOURCE_VALUE.get();
        PLATE_LAYOUT.load();
        RESOURCE_LAYOUT.load();
        ABILITIES_LAYOUT.load();
        barStyles.clear();
        for (String entry : BAR_STYLES.get()) {
            int split = entry.lastIndexOf('=');
            barStyles.put(entry.substring(0, split), entry.substring(split + 1));
        }
    }

    /** Writes the live values to the config file. */
    public static void save() {
        SHOW_ORIGIN_HUD.set(showOriginHud);
        SHOW_RESOURCE_BAR.set(showResourceBar);
        SHOW_XP_BAR.set(showXpBar);
        SHOW_ABILITY_INDICATORS.set(showAbilityIndicators);
        SHOW_COOLDOWN_OVERLAYS.set(showCooldownOverlays);
        SHOW_KEYBIND_HINTS.set(showKeybindHints);
        SHOW_RESOURCE_VALUE.set(showResourceValue);
        PLATE_LAYOUT.store();
        RESOURCE_LAYOUT.store();
        ABILITIES_LAYOUT.store();
        BAR_STYLES.set(barStyles.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).sorted().toList());
        saving = true;
        try {
            CLIENT_SPEC.save();
        } finally {
            saving = false;
        }
    }

    /** Back to a stock HUD: every element shown, in its default spot and style. Live values only. */
    public static void resetToDefaults() {
        showOriginHud = showResourceBar = showXpBar = showAbilityIndicators = true;
        showCooldownOverlays = showKeybindHints = showResourceValue = true;
        PLATE_LAYOUT.reset();
        RESOURCE_LAYOUT.reset();
        ABILITIES_LAYOUT.reset();
        barStyles.clear();
    }

    /** Carries a config from before the mod had its own folder over to the new file. */
    public static void migrateOldFile() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path oldFile = configDir.resolve(OLD_CLIENT_FILE);
        Path newFile = configDir.resolve(CLIENT_FILE);
        if (!Files.exists(oldFile) || Files.exists(newFile)) return;
        try {
            Files.createDirectories(newFile.getParent());
            Files.move(oldFile, newFile);
            Abyssorigins.LOGGER.info("[AbyssOrigins] Moved {} to {}.", OLD_CLIENT_FILE, CLIENT_FILE);
        } catch (IOException e) {
            Abyssorigins.LOGGER.warn("[AbyssOrigins] Could not move {} to {}.", OLD_CLIENT_FILE, CLIENT_FILE, e);
        }
    }
}
