package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.ClientAbilityData;
import com.gugas749.abyssorigins.client.ClientTimers;
import com.gugas749.abyssorigins.client.gui.HudLayoutScreen;
import com.gugas749.abyssorigins.client.input.KeyBindings;
import com.gugas749.abyssorigins.config.AbyssOriginsConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * Abyss HUD: a compact identity plate (origin sigil, level, XP thread, class line), the
 * origin's resource bar, and an ability bar (origin slots | class slots) with cooldown
 * sweeps, ready flashes, sustained-ability timers and key hints. Empty slots show a faint
 * "+" when the player knows a power that could fill them (bind it in the Abilities tab).
 *
 * Each of the three is a {@link HudElement} the player can place with {@link HudLayoutScreen}.
 */
public class OriginHudOverlay implements LayeredDraw.Layer {

    private static final int PLATE_WIDTH = 136, SLOT = 22, SLOT_GAP = 4, GROUP_GAP = 8;

    /** Per-slot memory for "just became ready" flashes: [cooldown last frame, flash start ms]. */
    private static final long[][] SLOT_MEMORY = new long[SlotGroup.ORIGIN.size() + SlotGroup.CLASS.size()][2];

    /** ability is null for an empty slot. */
    private record Slot(Ability ability, SlotGroup group, int index, int cooldown, int maxCooldown, int accent) {}

    @Override
    public void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen instanceof HudLayoutScreen) return;   // the editor draws the HUD itself, over its fog

        Origin origin = currentOrigin();
        if (origin == null) return;

        for (HudElement element : HudElement.values()) {
            if (!element.isVisible() || !element.appliesTo(origin)) continue;
            drawElement(gfx, element, origin, element.box(origin, false), ClientOriginData.getResourceBar(), false);
        }
    }

    /** The local player's origin, or null without one. */
    public static Origin currentOrigin() {
        Minecraft mc = Minecraft.getInstance();
        Origin origin = ClientOriginData.getOrigin();
        return origin != null || mc.player == null ? origin : AbyssOriginsAPI.getPlayerOrigin(mc.player);
    }

    /**
     * Draws one element at its box, at its scale.
     * @param resource the resource value the bar should show, 0..100
     * @param preview  true in the layout editor, where an empty ability bar still needs something to grab
     */
    public static void drawElement(GuiGraphics g, HudElement element, Origin origin, HudElement.Box box, float resource, boolean preview) {
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(box.x(), box.y(), 0);
        g.pose().scale(element.layout().scale, element.layout().scale, 1f);
        switch (element) {
            case PLATE -> renderPlate(g, font, origin);
            case RESOURCE -> ResourceBarRenderer.draw(g, font, BarStyle.current(origin), origin.getResourceType(),
                origin.getThemeColor(), resource, AbyssOriginsConfig.showResourceValue);
            case ABILITIES -> renderAbilityBar(g, font, origin, preview);
        }
        g.pose().popPose();
    }

    /** Unscaled size as {width, height}. */
    public static int[] size(HudElement element, Origin origin, boolean preview) {
        Font font = Minecraft.getInstance().font;
        return switch (element) {
            case PLATE -> new int[]{PLATE_WIDTH, plateHeight()};
            case RESOURCE -> ResourceBarRenderer.size(BarStyle.current(origin), font, AbyssOriginsConfig.showResourceValue);
            case ABILITIES -> abilityBarSize(abilitySlots(origin, preview), origin);
        };
    }

    // ---- Identity plate -------------------------------------------------------

    private static int plateHeight() {
        return 26 + (ClientSpecializationData.getSpec() != null ? 11 : 0) + (AbyssOriginsConfig.showXpBar ? 0 : -4);
    }

    private static void renderPlate(GuiGraphics g, Font font, Origin origin) {
        Specialization spec = ClientSpecializationData.getSpec();

        int w = PLATE_WIDTH;
        int h = plateHeight();
        int x = 0, y = 0;
        int accent = origin.getThemeColor();

        panel(g, x, y, w, h, accent, 1f);

        // Sigil
        float cx = x + 13, cy = y + 13;
        glow(g, cx, cy, 16, accent, 0.45f + 0.25f * pulse(3f));
        diamondRing(g, cx, cy, 9, 1.2f, accent, 0xFF0B0816);
        centered(g, font, origin.getSigil(), (int) cx, (int) cy - 4, accent);

        // Name + level
        int tx = x + 27;
        g.drawString(font, origin.getDisplayName(), tx, y + 5, TEXT, true);
        String lvl = "Lv " + ClientOriginData.getLevel();
        g.drawString(font, lvl, x + w - 5 - font.width(lvl), y + 5, GOLD, true);

        // Unspent points beacon
        int points = ClientOriginData.getSkillPoints() + (spec != null ? ClientSpecializationData.getSkillPoints() : 0);
        if (points > 0) {
            float pu = pulse(1.6f);
            diamond(g, x + w - 9, y + 19, 2.5f + pu, alpha(GOLD, 0.6f + 0.4f * pu));
        }

        int ly = y + 16;
        if (AbyssOriginsConfig.showXpBar) {
            bar(g, tx, ly, w - 27 - 14, 2, ClientOriginData.getXpProgress(), lighten(accent, 0.2f), false);
            ly += 6;
        }

        if (spec != null) {
            int sc = spec.getThemeColor();
            diamond(g, tx + 2, ly + 4, 2.5f, sc);
            g.drawString(font, spec.getDisplayName(), tx + 8, ly, mix(sc, TEXT, 0.4f), false);
            String sl = "Lv " + Math.max(1, ClientSpecializationData.getSpecLevel());
            g.drawString(font, sl, x + w - 5 - font.width(sl), ly, TEXT_DIM, false);
        }
    }

    // ---- Ability bar ----------------------------------------------------------

    private static List<Slot> abilitySlots(Origin origin, boolean preview) {
        List<Slot> slots = new ArrayList<>();
        addGroup(slots, SlotGroup.ORIGIN, origin.getThemeColor());
        Specialization spec = ClientSpecializationData.getSpec();
        if (spec != null) addGroup(slots, SlotGroup.CLASS, spec.getThemeColor());
        if (slots.isEmpty() && preview) {   // nothing unlocked yet, but the bar can still be placed
            for (int i = 0; i < SlotGroup.ORIGIN.size(); i++) {
                slots.add(new Slot(null, SlotGroup.ORIGIN, i, 0, 1, origin.getThemeColor()));
            }
        }
        return slots;
    }

    private static int originSlotCount(List<Slot> slots) {
        return (int) slots.stream().filter(s -> s.group == SlotGroup.ORIGIN).count();
    }

    private static int[] abilityBarSize(List<Slot> slots, Origin origin) {
        if (slots.isEmpty()) return new int[]{0, 0};
        int originCount = originSlotCount(slots);
        int w = slots.size() * SLOT + (slots.size() - 1) * SLOT_GAP + (originCount > 0 && originCount < slots.size() ? GROUP_GAP : 0);
        return new int[]{w, SLOT + (AbyssOriginsConfig.showKeybindHints ? 11 : 0) + 4};
    }

    private static void renderAbilityBar(GuiGraphics g, Font font, Origin origin, boolean preview) {
        List<Slot> slots = abilitySlots(origin, preview);
        int originCount = originSlotCount(slots);
        boolean hints = AbyssOriginsConfig.showKeybindHints;

        float resource = ClientOriginData.getResourceBar();
        int x = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (i == originCount && originCount > 0) x += GROUP_GAP;
            Slot s = slots.get(i);
            renderSlot(g, font, s, (s.group == SlotGroup.CLASS ? SlotGroup.ORIGIN.size() : 0) + s.index, x, 0, resource, hints);
            x += SLOT + SLOT_GAP;
        }
    }

    private static void addGroup(List<Slot> out, SlotGroup group, int accent) {
        for (int i = 0; i < ClientAbilityData.visibleSlots(group); i++) {
            Ability a = ClientAbilityData.ability(group, i);
            String id = a != null ? a.getId() : "";
            out.add(new Slot(a, group, i, a != null ? ClientAbilityData.cooldown(id) : 0,
                a != null ? ClientAbilityData.cooldownMax(id) : 1, accent));
        }
    }

    private static void renderSlot(GuiGraphics g, Font font, Slot s, int memIdx, int x, int y, float resource, boolean hints) {
        long now = System.currentTimeMillis();
        long[] mem = SLOT_MEMORY[Math.min(memIdx, SLOT_MEMORY.length - 1)];
        if (mem[0] > 0 && s.cooldown == 0) mem[1] = now;  // just became ready
        mem[0] = s.cooldown;

        if (s.ability == null) { renderEmptySlot(g, font, s, x, y, hints); return; }
        boolean active = ClientTimers.isActive(s.ability.getId());
        boolean onCd = s.cooldown > 0;
        boolean poor = s.group == SlotGroup.ORIGIN && s.ability.getResourceCost() > 0 && resource < s.ability.getResourceCost();
        int accent = s.ability.getColor();

        float cx = x + SLOT / 2f, cy = y + SLOT / 2f;
        if (active) glow(g, cx, cy, SLOT * 0.95f, accent, 0.55f + 0.35f * pulse(1.2f));

        g.fillGradient(x, y, x + SLOT, y + SLOT, 0xE0181330, 0xE00A0814);
        int edge = active ? accent : onCd ? EDGE : mix(EDGE, s.accent, 0.65f);
        border(g, x, y, SLOT, SLOT, edge);

        // Inner diamond plate + glyph
        diamond(g, cx, cy, 8, alpha(onCd ? 0xFF15112A : darken(accent, 0.35f), 0.9f));
        int glyphColor = poor ? BAD : onCd ? TEXT_FAINT : accent;
        centered(g, font, s.ability.getIcon(), (int) cx, (int) cy - 4, glyphColor);

        // Cooldown curtain from the top, receding
        if (onCd && AbyssOriginsConfig.showCooldownOverlays) {
            float pct = Mth.clamp(s.cooldown / (float) s.maxCooldown, 0f, 1f);
            int ch = (int) ((SLOT - 2) * pct);
            g.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + ch, 0xB0050409);
            g.fill(x + 1, y + ch, x + SLOT - 1, y + 1 + ch, alpha(s.accent, 0.8f));
        }
        if (onCd) {
            float secs = s.cooldown / 20f;
            String t = secs >= 10 ? String.valueOf((int) Math.ceil(secs)) : String.format("%.1f", secs);
            centered(g, font, t, (int) cx, (int) cy - 4, TEXT);
        }

        // Ready flash
        float flash = 1f - (now - mem[1]) / 450f;
        if (flash > 0 && flash <= 1f) {
            border(g, x - 1, y - 1, SLOT + 2, SLOT + 2, withAlpha(0xFFFFFF, (int) (220 * flash)));
            g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, withAlpha(accent, (int) (90 * flash)));
        }

        // Sustained timer
        if (active) {
            float f = ClientTimers.fraction(s.ability.getId());
            g.fill(x, y + SLOT + 1, x + SLOT, y + SLOT + 3, 0xC0050409);
            g.fill(x, y + SLOT + 1, x + (int) (SLOT * f), y + SLOT + 3, accent);
        }

        if (hints) {
            String key = keyName(s);
            int kw = font.width(key) + 4;
            int kx = x + (SLOT - kw) / 2, ky = y + SLOT + 4;
            g.fill(kx, ky, kx + kw, ky + 9, 0xC0080611);
            g.drawString(font, key, kx + 2, ky + 1, onCd ? TEXT_FAINT : TEXT_DIM, false);
        }
    }

    /** An empty slot: a faint "+" waiting for a power (bind it in the Abilities tab). */
    private static void renderEmptySlot(GuiGraphics g, Font font, Slot s, int x, int y, boolean hints) {
        g.fillGradient(x, y, x + SLOT, y + SLOT, 0x90181330, 0x900A0814);
        border(g, x, y, SLOT, SLOT, alpha(EDGE, 0.7f));
        centered(g, font, "+", x + SLOT / 2, y + SLOT / 2 - 4, alpha(TEXT_FAINT, 0.5f + 0.3f * pulse(2.4f)));
        if (hints) {
            String key = keyName(s);
            int kw = font.width(key) + 4;
            int kx = x + (SLOT - kw) / 2, ky = y + SLOT + 4;
            g.fill(kx, ky, kx + kw, ky + 9, 0x80080611);
            g.drawString(font, key, kx + 2, ky + 1, TEXT_FAINT, false);
        }
    }

    private static String keyName(Slot s) {
        return KeyBindings.label(s.group, s.index, 3);
    }
}
