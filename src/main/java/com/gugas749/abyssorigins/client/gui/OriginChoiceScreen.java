package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.ResourceType;
import com.gugas749.abyssorigins.client.ritual.ClientRitual;
import com.gugas749.abyssorigins.client.vfx.VfxManager;
import com.gugas749.abyssorigins.item.ritual.OriginRitual;
import com.gugas749.abyssorigins.network.packet.ChooseOriginPacket;
import com.gugas749.abyssorigins.network.packet.RitualPacket;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * The Orb's choice: origins down the left, the highlighted one described on the right, and the
 * player — seen from the front, wreathed in the awakening aura — in the open centre. Selecting an
 * origin recolours that aura locally as a preview; "Embrace" makes it real for everyone.
 * Closing the menu any other way lets the power fade and returns the orb (server side).
 */
public class OriginChoiceScreen extends Screen {

    private static final int CARD_H = 38, CARD_GAP = 4, TOP = 46;

    private final List<Origin> origins;
    private final long opened = Util.getMillis();
    /** The HUD, hotbar and chat are hidden while choosing; restored on close. */
    private Boolean hudWasHidden = null;
    private Origin selected, hovered;
    private boolean resolved = false;
    private int previewColor = -1;
    private double scroll = 0;
    private int[] embraceRect = null;
    private List<FormattedCharSequence> tooltip;

    public OriginChoiceScreen(List<Origin> origins) {
        super(Component.translatable("gui.abyssorigins.ritual.title"));
        this.origins = origins;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}

    @Override
    protected void init() {
        if (minecraft != null && hudWasHidden == null) {
            hudWasHidden = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
        }
    }

    // ---- Layout --------------------------------------------------------------------------------

    private int listX()  { return 12; }
    private int listW()  { return Math.min(200, (int) (width * 0.32f)); }
    private int detailW(){ return Math.min(230, (int) (width * 0.34f)); }
    private int detailX(){ return width - detailW() - 12; }
    private int bottom() { return height - 44; }
    private int contentH() { return origins.size() * (CARD_H + CARD_GAP); }
    private int maxScroll() { return Math.max(0, contentH() - (bottom() - TOP)); }
    private int cardY(int i) { return TOP + i * (CARD_H + CARD_GAP) - (int) scroll; }

    // ---- Render ----------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        tooltip = null;
        float fade = easeOut((Util.getMillis() - opened) / 450f);
        Origin focus = hovered != null ? hovered : selected;
        int accent = focus != null ? focus.getThemeColor() : 0xFF000000 | OriginRitual.NEUTRAL;

        // Keep the centre open: the player and their aura are the point of this screen
        vignette(g, width, height, accent & 0xFFFFFF, 90, 0.85f * fade);
        g.fillGradient(0, 0, width, 40, alpha(0xC0060410, fade), alpha(0x00060410, fade));

        float ts = 1.5f;
        Component title = Component.translatable("gui.abyssorigins.ritual.title");
        scaledText(g, font, title, width / 2f - font.width(title) * ts / 2f, 9, ts, alpha(lighten(accent, 0.35f), fade), true);
        centered(g, font, Component.translatable("gui.abyssorigins.ritual.subtitle").withStyle(ChatFormatting.ITALIC),
            width / 2, 25, alpha(TEXT_DIM, fade));

        drawCards(g, mx, my, fade);
        drawDetail(g, focus, fade);
        drawEmbrace(g, mx, my, fade);

        centered(g, font, Component.translatable("gui.abyssorigins.ritual.hint"), width / 2, height - 12, alpha(TEXT_FAINT, fade));
        super.render(g, mx, my, pt);
        if (tooltip != null) g.renderTooltip(font, tooltip, mx, my);
    }

    private void drawCards(GuiGraphics g, int mx, int my, float fade) {
        int x = listX(), w = listW();
        hovered = null;
        scroll = Mth.clamp(scroll, 0, maxScroll());
        g.enableScissor(x - 4, TOP - 4, x + w + 4, bottom() + 2);
        for (int i = 0; i < origins.size(); i++) {
            Origin o = origins.get(i);
            int y = cardY(i);
            if (y + CARD_H < TOP - 4 || y > bottom()) continue;
            float f = easeOut((Util.getMillis() - opened - i * 60L) / 380f) * fade;
            if (f <= 0.01f) continue;
            int dx = (int) ((1 - f) * -16);
            boolean hover = my >= TOP && my <= bottom() && inside(mx, my, x, y, w, CARD_H);
            if (hover) hovered = o;
            drawCard(g, o, x + dx, y, w, hover, o == selected, f);
        }
        g.disableScissor();
    }

    private void drawCard(GuiGraphics g, Origin o, int x, int y, int w, boolean hover, boolean sel, float fade) {
        int c = o.getThemeColor();
        if (sel) glow(g, x + 18, y + CARD_H / 2f, 30, c, 0.5f * fade);
        g.fillGradient(x, y, x + w, y + CARD_H, alpha(sel ? 0xF0221A3A : hover ? 0xE81C1630 : 0xD8120E22, fade), alpha(0xD80A0814, fade));
        border(g, x, y, w, CARD_H, alpha(sel ? c : hover ? mix(EDGE, c, 0.6f) : EDGE, fade));
        if (sel) g.fill(x, y, x + 2, y + CARD_H, alpha(c, fade));

        diamondRing(g, x + 18, y + CARD_H / 2f, 11, 1.3f, alpha(c, fade), alpha(0xFF0B0816, fade));
        centered(g, font, o.getSigil(), x + 18, y + CARD_H / 2 - 4, alpha(c, fade));
        g.drawString(font, o.getDisplayName(), x + 36, y + 6, alpha(sel ? lighten(c, 0.3f) : TEXT, fade), true);

        int impact = o.getImpactLevel().ordinal();
        int ic = impact == 0 ? GOOD : impact == 1 ? GOLD : BAD;
        for (int i = 0; i < 3; i++) diamond(g, x + w - 26 + i * 8, y + 10, 2.5f, alpha(i <= impact ? ic : EDGE, fade));

        List<FormattedCharSequence> desc = font.split(o.getDescription(), w - 44);
        if (!desc.isEmpty()) g.drawString(font, desc.get(0), x + 36, y + 20, alpha(TEXT_DIM, fade), false);
    }

    private void drawDetail(GuiGraphics g, Origin o, float fade) {
        int x = detailX(), w = detailW(), y = TOP, h = bottom() - TOP;
        if (o == null) {
            panel(g, x, y, w, 40, 0xFF000000 | OriginRitual.NEUTRAL, fade);
            centered(g, font, Component.translatable("gui.abyssorigins.ritual.pick"), x + w / 2, y + 16, alpha(TEXT_DIM, fade));
            return;
        }
        int c = o.getThemeColor();
        panel(g, x, y, w, h, c, fade);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int ix = x + 10, iw = w - 20, iy = y + 10;

        glow(g, ix + 14, iy + 14, 26, c, (0.45f + 0.2f * pulse(3f)) * fade);
        diamondRing(g, ix + 14, iy + 14, 13, 1.5f, alpha(c, fade), alpha(0xFF0B0816, fade));
        centered(g, font, o.getSigil(), ix + 14, iy + 10, alpha(c, fade));
        scaledText(g, font, o.getDisplayName(), ix + 34, iy + 4, 1.3f, alpha(lighten(c, 0.3f), fade), true);
        Component impact = Component.translatable("gui.abyssorigins.ritual.impact." + o.getImpactLevel().name().toLowerCase());
        g.drawString(font, impact, ix + 34, iy + 18, alpha(TEXT_FAINT, fade), false);
        iy += 34;

        iy = wrapped(g, font, o.getDescription(), ix, iy, iw, alpha(TEXT_DIM, fade), 4) + 6;

        if (!o.getTraits().isEmpty()) {
            iy += header(g, font, ix, iy, iw, Component.translatable("gui.abyssorigins.traits"), c, fade);
            for (Origin.Trait t : o.getTraits()) {
                g.drawString(font, t.positive() ? "+" : "−", ix + 1, iy, alpha(t.positive() ? GOOD : BAD, fade), false);
                iy = wrapped(g, font, t.text(), ix + 10, iy, iw - 10, alpha(t.positive() ? TEXT : mix(TEXT, BAD, 0.35f), fade), 2) + 2;
            }
            iy += 4;
        }

        if (!o.getAbilities().isEmpty()) {
            iy += header(g, font, ix, iy, iw, Component.translatable("gui.abyssorigins.abilities"), c, fade);
            for (OriginAbility a : o.getAbilities()) {
                diamond(g, ix + 5, iy + 4, 4, alpha(darken(a.getColor(), 0.35f), fade));
                centered(g, font, a.getIcon(), ix + 5, iy, alpha(a.getColor(), fade));
                g.drawString(font, a.getDisplayName(), ix + 14, iy, alpha(TEXT, fade), false);
                iy += 11;
            }
            iy += 4;
        }

        ResourceType rt = o.getResourceType();
        if (rt != null) {
            diamond(g, ix + 5, iy + 4, 3, alpha(rt.getBarColor(), fade));
            g.drawString(font, Component.translatable("gui.abyssorigins.ritual.resource", rt.getDisplayName()), ix + 14, iy, alpha(TEXT_DIM, fade), false);
        }
        g.disableScissor();
    }

    private void drawEmbrace(GuiGraphics g, int mx, int my, float fade) {
        int w = 170, h = 20, x = width / 2 - w / 2, y = height - 34;
        embraceRect = null;
        if (selected == null) {
            border(g, x, y, w, h, alpha(EDGE, 0.6f * fade));
            centered(g, font, Component.translatable("gui.abyssorigins.ritual.pick_short"), width / 2, y + 6, alpha(TEXT_FAINT, fade));
            return;
        }
        int c = selected.getThemeColor();
        boolean hover = inside(mx, my, x, y, w, h);
        glow(g, width / 2f, y + h / 2f, w * 0.45f, c, (hover ? 0.5f : 0.25f + 0.15f * pulse(1.6f)) * fade);
        g.fill(x, y, x + w, y + h, alpha(withAlpha(c, hover ? 90 : 45), fade));
        border(g, x, y, w, h, alpha(hover ? lighten(c, 0.5f) : c, fade));
        centered(g, font, Component.translatable("gui.abyssorigins.ritual.embrace", selected.getDisplayName()), width / 2, y + 6,
            alpha(hover ? 0xFFFFFFFF : lighten(c, 0.4f), fade));
        embraceRect = new int[]{ x, y, w, h };
    }

    // ---- Preview aura (client-local) --------------------------------------------------------------

    private void select(Origin o) {
        if (o == selected) return;
        selected = o;
        if (minecraft != null)
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.9f));
        setPreview(o.getThemeColor() & 0xFFFFFF);
    }

    private void setPreview(int rgb) {
        var player = minecraft != null ? minecraft.player : null;
        if (player == null) return;
        // The pale awakening aura gives way to the chosen colour — on this screen only, until confirmed
        VfxManager.spawn(Vfx.of(VfxType.AURA).follow(player).color(OriginRitual.NEUTRAL).extra(1).flags(Vfx.STOP).build());
        if (previewColor >= 0)
            VfxManager.spawn(Vfx.of(VfxType.AURA).follow(player).color(previewColor).extra(1).flags(Vfx.STOP).build());
        previewColor = rgb;
        if (rgb >= 0)
            VfxManager.spawn(Vfx.of(VfxType.AURA).follow(player).scale(1.3f).colors(rgb, 0xFFFFFF).duration(20 * 120).build());
    }

    // ---- Input -------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (embraceRect != null && inside(mx, my, embraceRect[0], embraceRect[1], embraceRect[2], embraceRect[3])) { embrace(); return true; }
        if (my >= TOP && my <= bottom())
            for (int i = 0; i < origins.size(); i++)
                if (inside(mx, my, listX(), cardY(i), listW(), CARD_H)) { select(origins.get(i)); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Mth.clamp(scroll - sy * 20, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!origins.isEmpty() && (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP)) {
            int i = selected == null ? -1 : origins.indexOf(selected);
            i = key == GLFW.GLFW_KEY_DOWN ? Math.min(origins.size() - 1, i + 1) : Math.max(0, i - 1);
            select(origins.get(i));
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && selected != null) { embrace(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void embrace() {
        if (selected == null || resolved) return;
        resolved = true;
        PacketDistributor.sendToServer(new ChooseOriginPacket(selected.getId().toString()));
        onClose();
    }

    /** The server already ended the ritual (timed out): close without answering it. */
    public void dismiss() {
        resolved = true;
        onClose();
    }

    /** Any way out other than Embrace lets the power fade (the server returns the orb). */
    @Override
    public void removed() {
        if (minecraft != null && hudWasHidden != null) minecraft.options.hideGui = hudWasHidden;
        setPreview(-1);
        if (!resolved) {
            resolved = true;
            PacketDistributor.sendToServer(new ChooseOriginPacket(""));
            ClientRitual.handle(RitualPacket.Stage.CANCELLED, List.of());
        }
        super.removed();
    }
}
