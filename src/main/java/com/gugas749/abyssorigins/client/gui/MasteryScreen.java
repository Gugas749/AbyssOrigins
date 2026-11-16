package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.api.Ascension;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.GearClient;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.item.gear.OriginWeaponItem;
import com.gugas749.abyssorigins.item.gear.RelicMastery;
import com.gugas749.abyssorigins.item.gear.WeaponClass;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * The Mastery tab of the K screen: every relic weapon the player carries (main hand first), and for the
 * selected one its mastery, XP, the level cap (mastery past 20 needs the Noble ascension), its stats now and
 * at the next level, and the milestones that unlock its passive, its power and shorter cooldowns.
 */
public class MasteryScreen extends Screen {

    private static final int HEADER_H = 40, ROW_H = 24;

    private final Screen parent;
    private final long opened = Util.getMillis();
    private final List<ItemStack> relics = new ArrayList<>();
    private int selected = 0;
    private int[] treeRect, abilitiesRect, closeRect = new int[4];
    private double scroll;
    private int detailsH, detailsViewH;

    public MasteryScreen(Screen parent) {
        super(Component.translatable("gui.abyssorigins.relic_mastery"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) { onClose(); return; }
        collect();
    }

    /** Relics in the main hand, offhand and inventory; the held one first. */
    private void collect() {
        relics.clear();
        Inventory inv = minecraft.player.getInventory();
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.getItem() instanceof OriginWeaponItem) relics.add(held);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof OriginWeaponItem && s != held) relics.add(s);
        }
        selected = Math.min(selected, Math.max(0, relics.size() - 1));
    }

    @Override public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        float fade = easeOut((Util.getMillis() - opened) / 350f);
        ItemStack sel = relics.isEmpty() ? ItemStack.EMPTY : relics.get(selected);
        int accent = sel.getItem() instanceof OriginWeaponItem w ? 0xFF000000 | Gear.originColor(w.originPath()) : MIST;

        backdrop(g, width, height, accent, fade);
        drawHeader(g, mx, my, fade, accent);

        int top = HEADER_H + 4, bottom = height - 8;
        int listW = Math.min(150, width / 3);
        int lx = 8, dx = lx + listW + 6, dw = width - dx - 8;
        panel(g, lx, top, listW, bottom - top, accent, fade);
        panel(g, dx, top, dw, bottom - top, accent, fade);

        if (relics.isEmpty()) {
            centered(g, font, Component.translatable("gui.abyssorigins.mastery.none"), lx + listW / 2, top + 14, alpha(TEXT_FAINT, fade));
            wrapped(g, font, Component.translatable("gui.abyssorigins.mastery.none_hint"), dx + 10, top + 12, dw - 20, alpha(TEXT_DIM, fade), 6);
        } else {
            drawList(g, lx, top, listW, bottom - top, mx, my, fade);
            detailsViewH = bottom - top - 12;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, detailsH - detailsViewH)));
            g.enableScissor(dx + 1, top + 1, dx + dw - 1, bottom - 1);
            int end = drawDetails(g, sel, dx + 8, top + 8 - (int) scroll, dw - 16, fade, accent);
            detailsH = end - (top + 8 - (int) scroll);
            g.disableScissor();
            if (detailsH > detailsViewH) {   // scrollbar
                int track = bottom - top - 8, thumb = Math.max(14, track * detailsViewH / detailsH);
                int ty = top + 4 + (int) ((track - thumb) * (scroll / Math.max(1, detailsH - detailsViewH)));
                g.fill(dx + dw - 5, top + 4, dx + dw - 3, bottom - 4, 0x40FFFFFF);
                g.fill(dx + dw - 5, ty, dx + dw - 3, ty + thumb, alpha(accent, 0.8f * fade));
            }
        }
        super.render(g, mx, my, pt);
    }

    // ---- Header ---------------------------------------------------------------------------------

    private void drawHeader(GuiGraphics g, int mx, int my, float fade, int accent) {
        scaledText(g, font, Component.translatable("gui.abyssorigins.relic_mastery"), 12, 8, 1.35f, alpha(lighten(accent, 0.3f), fade), true);

        int tabsW = font.width(Component.translatable("gui.abyssorigins.tab.abilities")) + font.width(Component.translatable("gui.abyssorigins.tab.mastery")) + 56
            + (hasTree() ? font.width(Component.translatable("gui.abyssorigins.tab.tree")) + 28 : 0);
        int tx = width - 26 - tabsW;
        String sub = font.plainSubstrByWidth(Component.translatable("gui.abyssorigins.mastery.subtitle").getString(), Math.max(0, tx - 20));
        g.drawString(font, sub, 12, 23, alpha(TEXT_DIM, fade), false);
        treeRect = hasTree() ? tab(g, tx, Component.translatable("gui.abyssorigins.tab.tree"), MIST, false, mx, my, fade) : null;
        if (treeRect != null) tx += treeRect[2] + 6;
        abilitiesRect = tab(g, tx, Component.translatable("gui.abyssorigins.tab.abilities"), MIST, false, mx, my, fade);
        tx += abilitiesRect[2] + 6;
        tab(g, tx, Component.translatable("gui.abyssorigins.tab.mastery"), accent, true, mx, my, fade);

        closeRect = new int[]{ width - 18, 6, 12, 12 };
        boolean ch = inside(mx, my, closeRect[0], closeRect[1], 12, 12);
        g.drawString(font, "✕", width - 15, 8, alpha(ch ? TEXT : TEXT_FAINT, fade), false);
    }

    private int[] tab(GuiGraphics g, int x, Component label, int color, boolean active, int mx, int my, float fade) {
        int w = font.width(label) + 22, h = 16, y = 12;
        boolean hover = !active && inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, alpha(active ? withAlpha(color, 60) : hover ? 0x40FFFFFF : 0x30000000, fade));
        if (active) g.fill(x, y + h - 1, x + w, y + h, alpha(color, fade));
        border(g, x, y, w, h, alpha(active ? color : EDGE, fade));
        diamond(g, x + 7, y + 8, 2.5f, alpha(color, fade));
        g.drawString(font, label, x + 13, y + 4, alpha(active ? TEXT : hover ? TEXT : TEXT_DIM, fade), false);
        return new int[]{ x, y, w, h };
    }

    // ---- List -------------------------------------------------------------------------------------

    private void drawList(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float fade) {
        int iy = y + 5;
        for (int i = 0; i < relics.size() && iy + ROW_H <= y + h - 4; i++) {
            ItemStack s = relics.get(i);
            RelicMastery m = RelicMastery.of(s);
            int color = s.getItem() instanceof OriginWeaponItem rw ? 0xFF000000 | Gear.originColor(rw.originPath()) : MIST;
            boolean sel = i == selected, hover = inside(mx, my, x + 4, iy, w - 8, ROW_H - 2);
            g.fill(x + 4, iy, x + w - 4, iy + ROW_H - 2, alpha(sel ? withAlpha(color, 50) : hover ? 0x30FFFFFF : 0x20000000, fade));
            if (sel) border(g, x + 4, iy, w - 8, ROW_H - 2, alpha(color, fade));
            g.renderItem(s, x + 7, iy + 3);
            int tx = x + 27, tw = w - 35;
            String name = font.plainSubstrByWidth(s.getHoverName().getString(), tw);
            g.drawString(font, name, tx, iy + 3, alpha(sel ? TEXT : TEXT_DIM, fade), false);
            bar(g, tx, iy + 14, tw - 22, 3, m.level() / (float) RelicMastery.MAX_LEVEL, color, sel);
            String lv = String.valueOf(m.level());
            g.drawString(font, lv, x + w - 8 - font.width(lv), iy + 11, alpha(m.maxed() ? GOLD : TEXT_FAINT, fade), false);
            iy += ROW_H;
        }
    }

    // ---- Details ----------------------------------------------------------------------------------

    /** @return the y below the last line drawn. */
    private int drawDetails(GuiGraphics g, ItemStack s, int x, int y, int w, float fade, int accent) {
        if (!(s.getItem() instanceof OriginWeaponItem relic)) return y;
        RelicMastery m = RelicMastery.of(s);
        int cap = RelicMastery.capFor(GearClient.localOriginLevel());
        String k = "abyssorigins.weapon." + relic.power().id();
        int iy = y;

        // Title: icon, name, origin and type
        g.pose().pushPose();
        g.pose().translate(x, iy, 0);
        g.pose().scale(1.5f, 1.5f, 1);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
        g.drawString(font, s.getHoverName(), x + 30, iy + 2, alpha(lighten(accent, 0.3f), fade), true);
        Component sub = Gear.originName(relic.originPath()).copy().append("  ·  ")
            .append(Component.translatable("abyssorigins.weapon_class." + relic.type().name().toLowerCase(Locale.ROOT)));
        g.drawString(font, sub, x + 30, iy + 13, alpha(TEXT_FAINT, fade), false);
        iy += 28;

        // Mastery level and XP
        Component lvl = m.maxed() ? Component.translatable("abyssorigins.mastery.mastered_tag")
            : Component.translatable("abyssorigins.mastery.level", m.level(), RelicMastery.MAX_LEVEL);
        g.drawString(font, lvl, x, iy, alpha(m.maxed() ? GOLD : TEXT, fade), true);
        if (!m.maxed()) {
            String xp = m.xp() + " / " + RelicMastery.xpForLevel(m.level()) + " XP";
            g.drawString(font, xp, x + w - font.width(xp), iy, alpha(TEXT_DIM, fade), false);
        }
        iy += 11;
        bar(g, x, iy, w, 4, m.maxed() ? 1f : m.progress(), accent, true);
        iy += 8;
        if (!m.maxed() && m.level() >= cap) {
            iy = wrapped(g, font, Component.translatable("abyssorigins.mastery.capped_short", RelicMastery.UNASCENDED_CAP, Ascension.NOBLE_LEVEL),
                x, iy, w, alpha(BAD, fade), 2) + 2;
        } else if (cap < RelicMastery.MAX_LEVEL) {
            iy = wrapped(g, font, Component.translatable("gui.abyssorigins.mastery.cap_note", RelicMastery.UNASCENDED_CAP, Ascension.NOBLE_LEVEL),
                x, iy, w, alpha(TEXT_FAINT, fade), 2) + 2;
        }
        iy += 3;

        // Stats now and at the next level
        iy += header(g, font, x, iy, w, Component.translatable("gui.abyssorigins.mastery.stats"), accent, fade);
        WeaponClass t = relic.type();
        int next = Math.min(RelicMastery.MAX_LEVEL, m.level() + 1);
        iy = stat(g, x, iy, w, "gui.abyssorigins.mastery.damage", t.damage(m.level()), t.damage(next), m.maxed(), fade);
        iy = stat(g, x, iy, w, "gui.abyssorigins.mastery.speed", t.speed(m.level()), t.speed(next), m.maxed(), fade);
        iy = stat(g, x, iy, w, "gui.abyssorigins.mastery.reach", t.reach(m.level()), t.reach(next), m.maxed(), fade);
        iy += 4;

        // Milestones
        iy += header(g, font, x, iy, w, Component.translatable("gui.abyssorigins.mastery.milestones"), accent, fade);
        iy = milestone(g, x, iy, w, m, RelicMastery.PASSIVE_LEVEL,
            Component.translatable("abyssorigins.tooltip.passive", Component.translatable(k + ".passive")),
            Component.translatable(k + ".passive.desc"), fade);
        iy = milestone(g, x, iy, w, m, RelicMastery.POWER_LEVEL,
            Component.translatable("gui.abyssorigins.mastery.power", Component.translatable(k + ".power")),
            Component.translatable(k + ".power.desc"), fade);
        iy = milestone(g, x, iy, w, m, RelicMastery.HONED_LEVEL,
            Component.translatable("gui.abyssorigins.mastery.honed"), null, fade);
        iy = milestone(g, x, iy, w, m, RelicMastery.MAX_LEVEL,
            Component.translatable("gui.abyssorigins.mastery.mastered", Ascension.NOBLE_LEVEL), null, fade);
        iy += 4;
        return wrapped(g, font, Component.translatable("gui.abyssorigins.mastery.sources", RelicMastery.XP_HIT, RelicMastery.XP_KILL,
            RelicMastery.XP_KILL_BOSS, RelicMastery.XP_POWER), x, iy, w, alpha(TEXT_FAINT, fade), 3) + 4;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, scroll - sy * 14);
        return true;
    }

    private int stat(GuiGraphics g, int x, int y, int w, String key, double now, double next, boolean maxed, float fade) {
        g.drawString(font, Component.translatable(key), x + 4, y, alpha(TEXT_DIM, fade), false);
        String v = fmt(now);
        if (!maxed && next != now) v += "  ->  " + fmt(next);
        g.drawString(font, v, x + w - font.width(v), y, alpha(TEXT, fade), false);
        return y + 10;
    }

    private int milestone(GuiGraphics g, int x, int y, int w, RelicMastery m, int level, Component title, Component desc, float fade) {
        boolean done = m.level() >= level;
        diamond(g, x + 3, y + 4, 2.5f, alpha(done ? GOLD : EDGE, fade));
        String lv = String.valueOf(level);
        g.drawString(font, lv, x + 9, y, alpha(done ? GOLD : TEXT_FAINT, fade), false);
        int tx = x + 24;
        y = wrapped(g, font, title, tx, y, w - 24, alpha(done ? TEXT : TEXT_DIM, fade), 2);
        if (desc != null) y = wrapped(g, font, desc, tx, y, w - 24, alpha(done ? TEXT_DIM : TEXT_FAINT, fade), 3);
        return y + 2;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0$", "");
    }

    // ---- Input ------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (inside(mx, my, closeRect[0], closeRect[1], closeRect[2], closeRect[3])) { onClose(); return true; }
        if (treeRect != null && inside(mx, my, treeRect[0], treeRect[1], treeRect[2], treeRect[3])) {
            click(1.1f);
            minecraft.setScreen(new SkillTreeScreen(parent));
            return true;
        }
        if (abilitiesRect != null && inside(mx, my, abilitiesRect[0], abilitiesRect[1], abilitiesRect[2], abilitiesRect[3])) {
            click(1.1f);
            minecraft.setScreen(new AbilitiesScreen(parent, SlotGroup.ORIGIN));
            return true;
        }
        int listW = Math.min(150, width / 3), top = HEADER_H + 4, iy = top + 5;
        for (int i = 0; i < relics.size(); i++, iy += ROW_H) {
            if (inside(mx, my, 12, iy, listW - 8, ROW_H - 2)) { selected = i; scroll = 0; click(1.3f); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean hasTree() {
        Origin o = ClientOriginData.getOrigin();
        Specialization s = ClientSpecializationData.getSpec();
        return (o != null && SkillTreeRegistry.hasTree(o.getId())) || (s != null && s.getSkillTree() != null);
    }

    private void click(float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
