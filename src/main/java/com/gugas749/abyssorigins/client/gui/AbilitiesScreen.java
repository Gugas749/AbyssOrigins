package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.client.ClientAbilityData;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import com.gugas749.abyssorigins.client.input.KeyBindings;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * The Abilities tab: every power the player knows, and the slots they are bound to.
 *
 * Two loadouts with the same layout for every Origin and Class — Origin (five slots) and
 * Class (five slots plus three class-specific ones). Pick a slot, then a power; right-click
 * a slot to clear it. Powers are grouped by the origin / class they come from, with a badge
 * for how they were learned (granted, skill, inherited). The server re-validates every bind.
 */
public class AbilitiesScreen extends Screen {

    private static final int SLOT = 30, SLOT_GAP = 8, GROUP_GAP = 18, HEADER_H = 18, ROW_H = 24;

    private record Row(AbilityRegistry.Entry entry, Loadouts.Source source, Component header, int color, String sigil, int y, int h) {
        boolean isHeader() { return entry == null; }
    }

    private final Screen parent;
    private final long opened = Util.getMillis();
    private SlotGroup group;
    private int selected = 0;
    private double scroll = 0;
    private int contentH;
    private final List<Row> rows = new ArrayList<>();
    private final List<int[]> tabRects = new ArrayList<>();
    private int[] treeRect = null, closeRect = new int[4];
    private List<FormattedCharSequence> tooltip;

    public AbilitiesScreen(Screen parent, SlotGroup group) {
        super(Component.translatable("gui.abyssorigins.abilities"));
        this.parent = parent;
        this.group = group;
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null || ClientOriginData.getOrigin() == null) { onClose(); return; }
        if (group == SlotGroup.CLASS && !ClientSpecializationData.hasSpec()) group = SlotGroup.ORIGIN;
        selectFirstEmpty();
    }

    private void selectFirstEmpty() {
        selected = 0;
        for (int i = 0; i < group.general(); i++)
            if (ClientAbilityData.slot(group, i).isEmpty()) { selected = i; return; }
    }

    @Override public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}
    @Override public boolean isPauseScreen() { return false; }

    // ---- Owner of the current loadout -------------------------------------------------------

    private int accent() {
        if (group == SlotGroup.CLASS && ClientSpecializationData.getSpec() != null) return ClientSpecializationData.getSpec().getThemeColor();
        return ClientOriginData.getOrigin() != null ? ClientOriginData.getOrigin().getThemeColor() : MIST;
    }

    private Component ownerName() {
        if (group == SlotGroup.CLASS && ClientSpecializationData.getSpec() != null) return ClientSpecializationData.getSpec().getDisplayName();
        return ClientOriginData.getOrigin() != null ? ClientOriginData.getOrigin().getDisplayName() : Component.empty();
    }

    private String ownerSigil() {
        if (group == SlotGroup.CLASS && ClientSpecializationData.getSpec() != null) return ClientSpecializationData.getSpec().getSigil();
        return ClientOriginData.getOrigin() != null ? ClientOriginData.getOrigin().getSigil() : "◆";
    }

    private boolean hasTree() {
        Origin o = ClientOriginData.getOrigin();
        Specialization s = ClientSpecializationData.getSpec();
        return (o != null && SkillTreeRegistry.hasTree(o.getId())) || (s != null && s.getSkillTree() != null);
    }

    // ---- Layout -----------------------------------------------------------------------------

    private int panelW()   { return Math.min(width - 24, 480); }
    private int panelX()   { return (width - panelW()) / 2; }
    private int panelTop() { return 40; }
    private int slotsY()   { return panelTop() + 30; }

    private int slotX(int i) {
        int total = group.size() * SLOT + (group.size() - 1) * SLOT_GAP + (group.specific() > 0 ? GROUP_GAP - SLOT_GAP : 0);
        int x = width / 2 - total / 2 + i * (SLOT + SLOT_GAP);
        return group.isSpecific(i) ? x + GROUP_GAP - SLOT_GAP : x;
    }

    private int listTop()    { return slotsY() + SLOT + 40; }
    private int listBottom() { return height - 14; }
    private int maxScroll()  { return Math.max(0, contentH - (listBottom() - listTop())); }

    /** Known powers grouped by the origin / class that owns them. */
    private void buildRows() {
        rows.clear();
        Map<ResourceLocation, List<Map.Entry<String, Loadouts.Source>>> byOwner = new LinkedHashMap<>();
        for (Map.Entry<String, Loadouts.Source> e : ClientAbilityData.known(group).entrySet()) {
            AbilityRegistry.Entry entry = AbilityRegistry.get(e.getKey());
            if (entry != null) byOwner.computeIfAbsent(entry.owner(), k -> new ArrayList<>()).add(e);
        }
        int y = 0;
        for (List<Map.Entry<String, Loadouts.Source>> list : byOwner.values()) {
            AbilityRegistry.Entry first = AbilityRegistry.get(list.get(0).getKey());
            rows.add(new Row(null, null, first.ownerName(), first.ownerColor(), first.ownerSigil(), y, HEADER_H));
            y += HEADER_H;
            for (Map.Entry<String, Loadouts.Source> e : list) {
                rows.add(new Row(AbilityRegistry.get(e.getKey()), e.getValue(), null, 0, "", y, ROW_H));
                y += ROW_H;
            }
        }
        contentH = y + 4;
    }

    // ---- Render -------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (ClientOriginData.getOrigin() == null) return;
        tooltip = null;
        buildRows();
        float fade = easeOut((Util.getMillis() - opened) / 350f);
        int accent = accent();
        backdrop(g, width, height, accent, fade);

        drawHeader(g, mx, my, fade, accent);

        int px = panelX(), pw = panelW(), top = panelTop();
        panel(g, px, top, pw, height - top - 8, accent, fade);

        // Resource (origin powers spend it) + hint
        Origin origin = ClientOriginData.getOrigin();
        ResourceType rt = origin.getResourceType();
        if (group == SlotGroup.ORIGIN && rt != null) {
            g.drawString(font, rt.getDisplayName(), px + 8, top + 8, alpha(TEXT_DIM, fade), false);
            bar(g, px + 8, top + 19, 90, 3, ClientOriginData.getResourceBar() / 100f, rt.getBarColor(), true);
        } else {
            g.drawString(font, Component.literal(ownerSigil() + "  ").append(ownerName()), px + 8, top + 10, alpha(lighten(accent, 0.25f), fade), false);
        }
        String hint = Component.translatable("gui.abyssorigins.abilities.hint", group.size()).getString();
        g.drawString(font, hint, px + pw - 8 - font.width(hint), top + 8, alpha(TEXT_FAINT, fade), false);

        for (int i = 0; i < group.size(); i++) drawSlot(g, i, mx, my, fade, accent);
        if (group.specific() > 0) {
            int sx = slotX(group.general()), ex = slotX(group.size() - 1) + SLOT;
            centered(g, font, Component.translatable("gui.abyssorigins.abilities.specific"), (sx + ex) / 2, slotsY() + SLOT + 13, alpha(TEXT_FAINT, fade));
        }
        centered(g, font, Component.translatable("gui.abyssorigins.abilities.instructions"), width / 2, slotsY() + SLOT + 26, alpha(TEXT_FAINT, fade));

        // Known powers
        int lt = listTop(), lb = listBottom();
        scroll = Mth.clamp(scroll, 0, maxScroll());
        g.enableScissor(px + 1, lt, px + pw - 1, lb);
        if (rows.isEmpty())
            centered(g, font, Component.translatable("gui.abyssorigins.abilities.none"), width / 2, lt + 12, alpha(TEXT_FAINT, fade));
        for (Row r : rows) {
            int y = lt + r.y - (int) scroll;
            if (y + r.h < lt || y > lb) continue;
            if (r.isHeader()) drawOwnerHeader(g, r, px + 10, y, pw - 20, fade);
            else drawPower(g, r, px + 10, y, pw - 20, mx, my, lt, lb, fade);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int track = lb - lt, thumb = Math.max(16, track * track / contentH);
            int ty = lt + (int) ((track - thumb) * (scroll / maxScroll()));
            g.fill(px + pw - 5, lt, px + pw - 3, lb, 0x40FFFFFF);
            g.fill(px + pw - 5, ty, px + pw - 3, ty + thumb, alpha(accent, 0.8f));
        }

        super.render(g, mx, my, pt);
        if (tooltip != null) g.renderTooltip(font, tooltip, mx, my);
    }

    private void drawHeader(GuiGraphics g, int mx, int my, float fade, int accent) {
        Component title = Component.translatable("gui.abyssorigins.abilities");
        float ts = 1.35f;
        scaledText(g, font, title, 12, 8, ts, alpha(lighten(accent, 0.3f), fade), true);
        g.drawString(font, Component.translatable("gui.abyssorigins.abilities.of", ownerName()), 12, 22, alpha(TEXT_DIM, fade), false);

        // Loadout tabs + a way back to the tree
        tabRects.clear();
        int tx = width / 2 - 90;
        for (SlotGroup gr : SlotGroup.values()) {
            boolean available = gr == SlotGroup.ORIGIN || ClientSpecializationData.hasSpec();
            Component label = Component.translatable(gr == SlotGroup.ORIGIN ? "gui.abyssorigins.abilities.tab.origin" : "gui.abyssorigins.abilities.tab.class");
            int c = gr == SlotGroup.CLASS && ClientSpecializationData.getSpec() != null ? ClientSpecializationData.getSpec().getThemeColor()
                  : ClientOriginData.getOrigin().getThemeColor();
            int[] r = tab(g, tx, label, c, gr == group, available, mx, my, fade);
            tabRects.add(available ? r : null);
            tx += r[2] + 6;
        }
        treeRect = null;
        if (hasTree())
            treeRect = tab(g, tx + 8, Component.translatable("gui.abyssorigins.tab.tree"), MIST, false, true, mx, my, fade);

        closeRect = new int[]{ width - 18, 6, 12, 12 };
        boolean ch = inside(mx, my, closeRect[0], closeRect[1], 12, 12);
        g.drawString(font, "✕", width - 15, 8, alpha(ch ? TEXT : TEXT_FAINT, fade), false);
    }

    private int[] tab(GuiGraphics g, int x, Component label, int color, boolean active, boolean enabled, int mx, int my, float fade) {
        int w = font.width(label) + 22, h = 16, y = 12;
        boolean hover = enabled && inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, alpha(active ? withAlpha(color, 60) : hover ? 0x40FFFFFF : 0x30000000, fade));
        if (active) g.fill(x, y + h - 1, x + w, y + h, alpha(color, fade));
        border(g, x, y, w, h, alpha(active ? color : EDGE, fade));
        diamond(g, x + 7, y + 8, 2.5f, alpha(enabled ? color : EDGE, fade));
        g.drawString(font, label, x + 13, y + 4, alpha(!enabled ? TEXT_FAINT : active ? TEXT : TEXT_DIM, fade), false);
        return new int[]{ x, y, w, h };
    }

    private void drawSlot(GuiGraphics g, int i, int mx, int my, float fade, int accent) {
        int x = slotX(i), y = slotsY();
        boolean hover = inside(mx, my, x, y, SLOT, SLOT), sel = i == selected, specific = group.isSpecific(i);
        Ability a = ClientAbilityData.ability(group, i);
        int color = a != null ? a.getColor() : 0xFF5E5880;

        if (sel) glow(g, x + SLOT / 2f, y + SLOT / 2f, SLOT * 1.05f, accent, 0.55f + 0.3f * pulse(1.6f));
        g.fillGradient(x, y, x + SLOT, y + SLOT, alpha(specific ? 0xF0281A30 : 0xF0201830, fade), alpha(0xF00A0814, fade));
        border(g, x, y, SLOT, SLOT, alpha(sel ? accent : hover ? lighten(EDGE, 0.4f) : specific ? mix(EDGE, accent, 0.35f) : EDGE, fade));
        g.drawString(font, String.valueOf(i + 1), x + 3, y + 2, alpha(sel ? accent : TEXT_FAINT, fade), false);

        if (a != null) {
            diamond(g, x + SLOT / 2f, y + SLOT / 2f + 1, 9, alpha(darken(color, 0.35f), 0.9f * fade));
            centered(g, font, a.getIcon(), x + SLOT / 2, y + SLOT / 2 - 3, alpha(color, fade));
            int cd = ClientAbilityData.cooldown(a.getId());
            if (cd > 0) {
                int ch = (int) ((SLOT - 2) * Mth.clamp(cd / (float) ClientAbilityData.cooldownMax(a.getId()), 0f, 1f));
                g.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + ch, 0xA0050409);
            }
        } else {
            centered(g, font, specific ? ownerSigil() : "+", x + SLOT / 2, y + SLOT / 2 - 3,
                alpha(TEXT_FAINT, (0.5f + 0.5f * pulse(2f)) * fade));
        }
        centered(g, font, KeyBindings.label(group, i, 4), x + SLOT / 2, y + SLOT + 3, alpha(sel ? accent : TEXT_DIM, fade));

        if (hover) {
            List<FormattedCharSequence> tip = new ArrayList<>();
            if (a != null) {
                tip.add(a.getDisplayName().copy().withStyle(s -> s.withColor(color & 0xFFFFFF)).getVisualOrderText());
                tip.addAll(font.split(a.getDescription(), 220));
                tip.add(Component.translatable("gui.abyssorigins.abilities.unbind").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            } else {
                tip.add(Component.translatable(specific ? "gui.abyssorigins.abilities.empty_specific" : "gui.abyssorigins.abilities.empty",
                    ownerName()).withStyle(ChatFormatting.GRAY).getVisualOrderText());
            }
            tooltip = tip;
        }
    }

    private void drawOwnerHeader(GuiGraphics g, Row r, int x, int y, int w, float fade) {
        g.drawString(font, r.sigil, x, y + 6, alpha(r.color, fade), false);
        g.drawString(font, r.header, x + 12, y + 6, alpha(lighten(r.color, 0.2f), fade), false);
        int lx = x + 18 + font.width(r.header);
        gradientH(g, lx, y + 10, x + w, y + 11, alpha(r.color, 0.5f * fade), alpha(r.color & 0x00FFFFFF, fade));
    }

    private void drawPower(GuiGraphics g, Row r, int x, int y, int w, int mx, int my, int lt, int lb, float fade) {
        Ability a = r.entry.ability();
        boolean hover = my >= lt && my <= lb && inside(mx, my, x, y + 1, w, ROW_H - 2);
        boolean fits = Loadouts.fits(group, selected, a.getId(), ClientSpecializationData.getSpec());
        int bound = ClientAbilityData.data().slotOf(group, a.getId());
        int accent = a.getColor();
        float dim = fits ? 1f : 0.45f;

        g.fillGradient(x, y + 1, x + w, y + ROW_H - 1,
            alpha(hover && fits ? 0xD0261E3A : 0x80141024, fade), alpha(hover && fits ? 0xD0181228 : 0x800C0A18, fade));
        if (bound >= 0) g.fill(x, y + 1, x + 2, y + ROW_H - 1, alpha(accent(), fade));

        diamond(g, x + 13, y + ROW_H / 2f, 8, alpha(darken(accent, 0.35f), fade * dim));
        centered(g, font, a.getIcon(), x + 13, y + ROW_H / 2 - 4, alpha(accent, fade * dim));
        g.drawString(font, a.getDisplayName(), x + 28, y + 4, alpha(hover ? TEXT : lighten(TEXT_DIM, 0.2f), fade * dim), false);

        String meta = (a.getResourceCost() > 0 && group == SlotGroup.ORIGIN ? a.getResourceCost() + " ◆  " : "") + (a.getBaseCooldownTicks() / 20) + "s";
        g.drawString(font, meta, x + 28, y + 14, alpha(TEXT_FAINT, fade * dim), false);

        int right = x + w - 8;
        if (bound >= 0) {
            String tag = "[" + KeyBindings.label(group, bound, 4) + "]";
            g.drawString(font, tag, right - font.width(tag), y + 8, alpha(accent(), fade), false);
            right -= font.width(tag) + 6;
        }
        if (r.source != Loadouts.Source.DEFAULT) {
            Component badge = Component.translatable("gui.abyssorigins.abilities.source." + r.source.name().toLowerCase(Locale.ROOT));
            int bw = font.width(badge) + 6, bx = right - bw;
            int bc = r.source == Loadouts.Source.GRANTED ? GOLD : r.source == Loadouts.Source.SKILL ? GOOD : MIST;
            g.fill(bx, y + 6, bx + bw, y + 17, alpha(withAlpha(bc, 40), fade));
            border(g, bx, y + 6, bw, 11, alpha(bc, 0.7f * fade));
            g.drawString(font, badge, bx + 3, y + 8, alpha(bc, fade), false);
        }

        if (hover) {
            List<FormattedCharSequence> tip = new ArrayList<>();
            tip.add(a.getDisplayName().copy().withStyle(s -> s.withColor(accent & 0xFFFFFF)).getVisualOrderText());
            tip.addAll(font.split(a.getDescription(), 230));
            tip.add((fits
                ? Component.translatable("gui.abyssorigins.abilities.bind_to", selected + 1).withStyle(ChatFormatting.GOLD)
                : Component.translatable("gui.abyssorigins.abilities.not_here", ownerName()).withStyle(ChatFormatting.RED)).getVisualOrderText());
            tooltip = tip;
        }
    }

    // ---- Input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my, closeRect[0], closeRect[1], closeRect[2], closeRect[3])) { onClose(); return true; }
        for (int t = 0; t < tabRects.size(); t++) {
            int[] r = tabRects.get(t);
            if (r != null && inside(mx, my, r[0], r[1], r[2], r[3])) { switchGroup(SlotGroup.values()[t]); return true; }
        }
        if (treeRect != null && inside(mx, my, treeRect[0], treeRect[1], treeRect[2], treeRect[3])) {
            click(1.1f);
            minecraft.setScreen(new SkillTreeScreen(parent, group == SlotGroup.CLASS));
            return true;
        }
        for (int i = 0; i < group.size(); i++) {
            if (!inside(mx, my, slotX(i), slotsY(), SLOT, SLOT)) continue;
            if (button == 1) ClientAbilityData.bind(group, i, "");
            else selected = i;
            click(1.3f);
            return true;
        }
        if (button == 0 && my >= listTop() && my <= listBottom()) {
            buildRows();
            int x = panelX() + 10, w = panelW() - 20;
            for (Row r : rows) {
                if (r.isHeader()) continue;
                int y = listTop() + r.y - (int) scroll;
                if (!inside(mx, my, x, y + 1, w, ROW_H - 2)) continue;
                String id = r.entry.id();
                if (!Loadouts.fits(group, selected, id, ClientSpecializationData.getSpec())) return true;
                ClientAbilityData.bind(group, selected, id);
                if (minecraft != null)
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f));
                selected = (selected + 1) % group.size();                 // flow on to the next slot
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Mth.clamp(scroll - sy * 20, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + group.size()) { selected = key - GLFW.GLFW_KEY_1; return true; }
        if (key == GLFW.GLFW_KEY_TAB) {
            switchGroup(group == SlotGroup.ORIGIN && ClientSpecializationData.hasSpec() ? SlotGroup.CLASS : SlotGroup.ORIGIN);
            return true;
        }
        if (KeyBindings.ABILITIES.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void switchGroup(SlotGroup g) {
        if (g == group) return;
        group = g;
        scroll = 0;
        selectFirstEmpty();
        click(0.9f);
    }

    private void click(float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
