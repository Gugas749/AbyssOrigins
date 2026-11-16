package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abysscore.api.gui.hud.AbyssHud;
import com.gugas749.abysscore.api.gui.hud.AbyssHudElement;
import com.gugas749.abysscore.api.gui.hud.HudPlacement;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.gui.AbyssTheme;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig.ElementLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Abyss HUD on AbyssCore's shared HUD API: AbyssCore draws the elements where they belong and
 * runs the layout editor (/abyssoriginshud or the H key, with a gear popup per element); this class
 * connects it to {@link AbyssOriginsConfig}, whose live values the editor changes. Done/Esc writes
 * them to config/abyssorigins/client.toml, Cancel reloads them from it.
 *
 * Positions use {@link com.gugas749.abysscore.api.gui.hud.HudPositioning#ALIGNED}: the convention
 * the saved anchor/offset numbers were always written in.
 */
public final class OriginsHud extends AbyssHud {

    public static final OriginsHud INSTANCE = new OriginsHud();

    private static final int FALLBACK_ACCENT = AbyssTheme.MIST;

    private OriginsHud() {}

    @Override
    public Component title() {
        return Component.translatable("gui.abyssorigins.hud_layout");
    }

    @Override
    public List<HudElement> elements() {
        return List.of(HudElement.values());
    }

    // ---- Storage: AbyssOriginsConfig's live values ---------------------------------

    @Override
    public HudPlacement placement(AbyssHudElement element) {
        HudElement e = (HudElement) element;
        ElementLayout layout = e.layout();
        return new HudPlacement(layout.anchor.toCore(), layout.offsetX, layout.offsetY, layout.scale, e.isVisible());
    }

    @Override
    public void setPlacement(AbyssHudElement element, HudPlacement placement) {
        HudElement e = (HudElement) element;
        ElementLayout layout = e.layout();
        layout.anchor = AbyssOriginsConfig.HudAnchor.fromCore(placement.anchor());
        layout.offsetX = placement.offsetX();
        layout.offsetY = placement.offsetY();
        layout.scale = placement.scale();
        e.setVisible(placement.visible());
    }

    @Override
    public void save() {
        AbyssOriginsConfig.save();
    }

    @Override
    public void revert() {
        AbyssOriginsConfig.reload();
    }

    /** As before: every element shown in its default spot and style, bar styles of every origin cleared. */
    @Override
    public void resetAll() {
        AbyssOriginsConfig.resetToDefaults();
    }

    @Override
    protected Component resetMessage() {
        return Component.translatable("gui.abyssorigins.hud_layout.reset_done");
    }

    // ---- Editor ----------------------------------------------------------------------

    @Override
    public float minScale() {
        return AbyssOriginsConfig.MIN_SCALE;
    }

    @Override
    public float maxScale() {
        return AbyssOriginsConfig.MAX_SCALE;
    }

    /** The origin's own colour. */
    @Override
    public int accentColor() {
        Origin origin = OriginHudOverlay.currentOrigin();
        return origin != null ? origin.getThemeColor() : FALLBACK_ACCENT;
    }

    /** Editor backdrop: a light fog with the origin's mist. */
    @Override
    public void renderEditorBackdrop(GuiGraphics g, int width, int height) {
        g.fillGradient(0, 0, width, height, 0x600A0818, 0x70100C1E);
        AbyssTheme.mist(g, width, height, accentColor(), 0.7f);
    }

    @Override
    @Nullable
    public Component editorUnavailableReason() {
        return OriginHudOverlay.currentOrigin() == null ? Component.translatable("gui.abyssorigins.hud_layout.no_origin") : null;
    }

    @Override
    protected boolean shouldRender() {
        return super.shouldRender() && OriginHudOverlay.currentOrigin() != null;
    }

    /** While editing, a config file reload must not wipe the edit in progress. */
    @Override
    protected void onEditorOpened() {
        AbyssOriginsConfig.editing = true;
    }

    @Override
    protected void onEditorClosed() {
        AbyssOriginsConfig.editing = false;
    }
}
