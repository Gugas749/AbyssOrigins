package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig.HudPosition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * HUD Configuration Screen.
 * Ported from VeilOrigins' HudConfigScreen, wired to AbyssOriginsConfig.
 *
 * Open with: H key (registered in KeyBindings + KeyInputHandler).
 * Saves automatically on close via NeoForge's ModConfigSpec.
 */
public class HudConfigScreen extends Screen {

    private static final int BTN_H   = 20;
    private static final int PADDING = 5;

    private final Screen parent;

    // Toggle buttons — kept as fields so setAllToggles() can update their labels
    private Button showOriginHudBtn;
    private Button showResourceBarBtn;
    private Button showXpBarBtn;
    private Button showAbilityIndicatorsBtn;
    private Button showCooldownOverlaysBtn;
    private Button showKeybindHintsBtn;
    private Button showBloodNumericBtn;

    // Position cycle buttons
    private Button originInfoPosBtn;
    private Button abilityIndPosBtn;

    public HudConfigScreen(Screen parent) {
        super(Component.translatable("gui.abyssorigins.hud_config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int cx      = this.width / 2;
        int btnW    = 200;
        int y       = 50;

        // ---- Row 1 -------------------------------------------------------
        showOriginHudBtn = toggle(cx - btnW - 5, y, btnW,
            "abyssorigins.config.showOriginHud",
            AbyssOriginsConfig.showOriginHud,
            () -> {
                AbyssOriginsConfig.showOriginHud = !AbyssOriginsConfig.showOriginHud;
                AbyssOriginsConfig.SHOW_ORIGIN_HUD.set(AbyssOriginsConfig.showOriginHud);
                return AbyssOriginsConfig.showOriginHud;
            });
        addRenderableWidget(showOriginHudBtn);

        showResourceBarBtn = toggle(cx + 5, y, btnW,
            "abyssorigins.config.showResourceBar",
            AbyssOriginsConfig.showResourceBar,
            () -> {
                AbyssOriginsConfig.showResourceBar = !AbyssOriginsConfig.showResourceBar;
                AbyssOriginsConfig.SHOW_RESOURCE_BAR.set(AbyssOriginsConfig.showResourceBar);
                return AbyssOriginsConfig.showResourceBar;
            });
        addRenderableWidget(showResourceBarBtn);
        y += BTN_H + PADDING;

        // ---- Row 2 -------------------------------------------------------
        showXpBarBtn = toggle(cx - btnW - 5, y, btnW,
            "abyssorigins.config.showXpBar",
            AbyssOriginsConfig.showXpBar,
            () -> {
                AbyssOriginsConfig.showXpBar = !AbyssOriginsConfig.showXpBar;
                AbyssOriginsConfig.SHOW_XP_BAR.set(AbyssOriginsConfig.showXpBar);
                return AbyssOriginsConfig.showXpBar;
            });
        addRenderableWidget(showXpBarBtn);

        showAbilityIndicatorsBtn = toggle(cx + 5, y, btnW,
            "abyssorigins.config.showAbilityIndicators",
            AbyssOriginsConfig.showAbilityIndicators,
            () -> {
                AbyssOriginsConfig.showAbilityIndicators = !AbyssOriginsConfig.showAbilityIndicators;
                AbyssOriginsConfig.SHOW_ABILITY_INDICATORS.set(AbyssOriginsConfig.showAbilityIndicators);
                return AbyssOriginsConfig.showAbilityIndicators;
            });
        addRenderableWidget(showAbilityIndicatorsBtn);
        y += BTN_H + PADDING;

        // ---- Row 3 -------------------------------------------------------
        showCooldownOverlaysBtn = toggle(cx - btnW - 5, y, btnW,
            "abyssorigins.config.showCooldownOverlays",
            AbyssOriginsConfig.showCooldownOverlays,
            () -> {
                AbyssOriginsConfig.showCooldownOverlays = !AbyssOriginsConfig.showCooldownOverlays;
                AbyssOriginsConfig.SHOW_COOLDOWN_OVERLAYS.set(AbyssOriginsConfig.showCooldownOverlays);
                return AbyssOriginsConfig.showCooldownOverlays;
            });
        addRenderableWidget(showCooldownOverlaysBtn);

        showKeybindHintsBtn = toggle(cx + 5, y, btnW,
            "abyssorigins.config.showKeybindHints",
            AbyssOriginsConfig.showKeybindHints,
            () -> {
                AbyssOriginsConfig.showKeybindHints = !AbyssOriginsConfig.showKeybindHints;
                AbyssOriginsConfig.SHOW_KEYBIND_HINTS.set(AbyssOriginsConfig.showKeybindHints);
                return AbyssOriginsConfig.showKeybindHints;
            });
        addRenderableWidget(showKeybindHintsBtn);
        y += BTN_H + PADDING;

        // ---- Row 4 — blood numeric + spacer ------------------------------
        showBloodNumericBtn = toggle(cx - btnW - 5, y, btnW,
            "abyssorigins.config.showBloodNumeric",
            AbyssOriginsConfig.showBloodNumeric,
            () -> {
                AbyssOriginsConfig.showBloodNumeric = !AbyssOriginsConfig.showBloodNumeric;
                AbyssOriginsConfig.SHOW_BLOOD_NUMERIC.set(AbyssOriginsConfig.showBloodNumeric);
                return AbyssOriginsConfig.showBloodNumeric;
            });
        addRenderableWidget(showBloodNumericBtn);
        y += BTN_H + PADDING + 10;

        // ---- Position cycle buttons --------------------------------------
        originInfoPosBtn = addRenderableWidget(Button.builder(
            posLabel("abyssorigins.config.originInfoPosition", AbyssOriginsConfig.originInfoPosition),
            b -> {
                AbyssOriginsConfig.originInfoPosition = cyclePosition(AbyssOriginsConfig.originInfoPosition);
                AbyssOriginsConfig.ORIGIN_INFO_POSITION.set(AbyssOriginsConfig.originInfoPosition);
                b.setMessage(posLabel("abyssorigins.config.originInfoPosition", AbyssOriginsConfig.originInfoPosition));
            }).bounds(cx - btnW - 5, y, btnW, BTN_H).build());

        abilityIndPosBtn = addRenderableWidget(Button.builder(
            posLabel("abyssorigins.config.abilityIndicatorsPosition", AbyssOriginsConfig.abilityIndicatorsPosition),
            b -> {
                AbyssOriginsConfig.abilityIndicatorsPosition = cyclePosition(AbyssOriginsConfig.abilityIndicatorsPosition);
                AbyssOriginsConfig.ABILITY_INDICATORS_POSITION.set(AbyssOriginsConfig.abilityIndicatorsPosition);
                b.setMessage(posLabel("abyssorigins.config.abilityIndicatorsPosition", AbyssOriginsConfig.abilityIndicatorsPosition));
            }).bounds(cx + 5, y, btnW, BTN_H).build());
        y += BTN_H + PADDING + 15;

        // ---- Presets -----------------------------------------------------
        addRenderableWidget(Button.builder(
            Component.translatable("abyssorigins.config.enableAll").withStyle(ChatFormatting.GREEN),
            b -> setAll(true)).bounds(cx - 155, y, 150, BTN_H).build());

        addRenderableWidget(Button.builder(
            Component.translatable("abyssorigins.config.disableAll").withStyle(ChatFormatting.RED),
            b -> setAll(false)).bounds(cx + 5, y, 150, BTN_H).build());

        // ---- Done --------------------------------------------------------
        addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            b -> onClose()).bounds(cx - 100, this.height - 28, 200, BTN_H).build());
    }

    // ---- Rendering -------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics gfx, int mx, int my, float pt) {
        // no blur — draw our own semi-transparent overlay
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mx, int my, float pt) {
        // Background
        gfx.fill(0, 0, this.width, this.height, 0x88000000);

        // Widgets on top
        super.render(gfx, mx, my, pt);

        int cx = this.width / 2;

        // Title
        gfx.drawCenteredString(font,
            Component.translatable("gui.abyssorigins.hud_config"), cx, 15, 0xFFFFFF);

        // Subtitle
        gfx.drawCenteredString(font,
            Component.literal("Toggle HUD elements on or off").withStyle(ChatFormatting.GRAY),
            cx, 28, 0xFFFFFF);

        // Hint at bottom
        gfx.drawCenteredString(font,
            Component.literal("Changes are saved automatically").withStyle(ChatFormatting.DARK_GRAY),
            cx, this.height - 42, 0xFFFFFF);
    }

    // ---- Close -----------------------------------------------------------

    @Override
    public void onClose() {
        // Force save by re-setting all values (NeoForge auto-saves on set)
        AbyssOriginsConfig.SHOW_ORIGIN_HUD.set(AbyssOriginsConfig.showOriginHud);
        AbyssOriginsConfig.SHOW_RESOURCE_BAR.set(AbyssOriginsConfig.showResourceBar);
        AbyssOriginsConfig.SHOW_XP_BAR.set(AbyssOriginsConfig.showXpBar);
        AbyssOriginsConfig.SHOW_ABILITY_INDICATORS.set(AbyssOriginsConfig.showAbilityIndicators);
        AbyssOriginsConfig.SHOW_COOLDOWN_OVERLAYS.set(AbyssOriginsConfig.showCooldownOverlays);
        AbyssOriginsConfig.SHOW_KEYBIND_HINTS.set(AbyssOriginsConfig.showKeybindHints);
        AbyssOriginsConfig.SHOW_BLOOD_NUMERIC.set(AbyssOriginsConfig.showBloodNumeric);
        AbyssOriginsConfig.ORIGIN_INFO_POSITION.set(AbyssOriginsConfig.originInfoPosition);
        AbyssOriginsConfig.ABILITY_INDICATORS_POSITION.set(AbyssOriginsConfig.abilityIndicatorsPosition);
        Abyssorigins.LOGGER.info("[AbyssOrigins] HUD config saved.");
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return true; }

    // ---- Helpers ---------------------------------------------------------

    private Button toggle(int x, int y, int w, String key, boolean initial, ToggleAction action) {
        return Button.builder(toggleLabel(key, initial), btn -> {
            boolean next = action.toggle();
            btn.setMessage(toggleLabel(key, next));
        }).bounds(x, y, w, BTN_H).build();
    }

    private Component toggleLabel(String key, boolean on) {
        Component state = on
            ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
            : Component.literal("OFF").withStyle(ChatFormatting.RED);
        return Component.translatable(key).append(": ").append(state);
    }

    private Component posLabel(String key, HudPosition pos) {
        String posName = switch (pos) {
            case TOP_LEFT     -> "Top Left";
            case TOP_RIGHT    -> "Top Right";
            case BOTTOM_LEFT  -> "Bottom Left";
            case BOTTOM_RIGHT -> "Bottom Right";
        };
        return Component.translatable(key).append(": ")
            .append(Component.literal(posName).withStyle(ChatFormatting.YELLOW));
    }

    private HudPosition cyclePosition(HudPosition current) {
        return switch (current) {
            case TOP_LEFT     -> HudPosition.TOP_RIGHT;
            case TOP_RIGHT    -> HudPosition.BOTTOM_RIGHT;
            case BOTTOM_RIGHT -> HudPosition.BOTTOM_LEFT;
            case BOTTOM_LEFT  -> HudPosition.TOP_LEFT;
        };
    }

    private void setAll(boolean on) {
        AbyssOriginsConfig.showOriginHud         = on;
        AbyssOriginsConfig.showResourceBar       = on;
        AbyssOriginsConfig.showXpBar             = on;
        AbyssOriginsConfig.showAbilityIndicators = on;
        AbyssOriginsConfig.showCooldownOverlays  = on;
        AbyssOriginsConfig.showKeybindHints      = on;
        AbyssOriginsConfig.showBloodNumeric      = on;

        AbyssOriginsConfig.SHOW_ORIGIN_HUD.set(on);
        AbyssOriginsConfig.SHOW_RESOURCE_BAR.set(on);
        AbyssOriginsConfig.SHOW_XP_BAR.set(on);
        AbyssOriginsConfig.SHOW_ABILITY_INDICATORS.set(on);
        AbyssOriginsConfig.SHOW_COOLDOWN_OVERLAYS.set(on);
        AbyssOriginsConfig.SHOW_KEYBIND_HINTS.set(on);
        AbyssOriginsConfig.SHOW_BLOOD_NUMERIC.set(on);

        showOriginHudBtn.setMessage(toggleLabel("abyssorigins.config.showOriginHud", on));
        showResourceBarBtn.setMessage(toggleLabel("abyssorigins.config.showResourceBar", on));
        showXpBarBtn.setMessage(toggleLabel("abyssorigins.config.showXpBar", on));
        showAbilityIndicatorsBtn.setMessage(toggleLabel("abyssorigins.config.showAbilityIndicators", on));
        showCooldownOverlaysBtn.setMessage(toggleLabel("abyssorigins.config.showCooldownOverlays", on));
        showKeybindHintsBtn.setMessage(toggleLabel("abyssorigins.config.showKeybindHints", on));
        showBloodNumericBtn.setMessage(toggleLabel("abyssorigins.config.showBloodNumeric", on));
    }

    @FunctionalInterface
    private interface ToggleAction {
        boolean toggle();
    }
}
