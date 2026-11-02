package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.ClientAbilityData;
import com.gugas749.abyssorigins.client.ClientTimers;
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
 * Abyss HUD: a compact identity plate (origin sigil, level, XP thread, class line,
 * resource) and an ability bar (origin slots | class slots) with cooldown sweeps, ready
 * flashes, sustained-ability timers and key hints. Empty slots show a faint "+" when the
 * player knows a power that could fill them (bind it in the Abilities tab).
 */
public class OriginHudOverlay implements LayeredDraw.Layer {

    private static final int SLOT = 22, SLOT_GAP = 4, GROUP_GAP = 8;

    /** Per-slot memory for "just became ready" flashes: [cooldown last frame, flash start ms]. */
    private static final long[][] SLOT_MEMORY = new long[SlotGroup.ORIGIN.size() + SlotGroup.CLASS.size()][2];

    /** ability is null for an empty slot. */
    private record Slot(Ability ability, SlotGroup group, int index, int cooldown, int maxCooldown, int accent) {}

    @Override
    public void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Origin origin = ClientOriginData.getOrigin();
        if (origin == null) origin = AbyssOriginsAPI.getPlayerOrigin(mc.player);
        if (origin == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (AbyssOriginsConfig.showOriginHud)         renderPlate(gfx, mc.font, sw, sh, origin);
        if (AbyssOriginsConfig.showAbilityIndicators) renderAbilityBar(gfx, mc.font, sw, sh, origin);
    }

    // ---- Identity plate -------------------------------------------------------

    private static void renderPlate(GuiGraphics g, Font font, int sw, int sh, Origin origin) {
        Specialization spec = ClientSpecializationData.getSpec();
        ResourceType res = origin.getResourceType();
        boolean showRes = res != null && !"blood".equals(res.getId()) && AbyssOriginsConfig.showResourceBar;

        int w = 136;
        int h = 26 + (spec != null ? 11 : 0) + (showRes ? 12 : 0) + (AbyssOriginsConfig.showXpBar ? 0 : -4);
        int[] p = cornerPos(AbyssOriginsConfig.originInfoPosition, sw, sh, w, h, 4, 4);
        int x = p[0], y = p[1];
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
            ly += 11;
        }

        if (showRes) {
            float v = ClientOriginData.getResourceBar();
            bar(g, x + 6, ly + 2, w - 34, 5, v / 100f, res.getBarColor(), true);
            String num = String.valueOf(Math.round(v));
            g.drawString(font, num, x + w - 5 - font.width(num), ly, res.getBarColor(), true);
        }
    }

    // ---- Ability bar ----------------------------------------------------------

    private static void renderAbilityBar(GuiGraphics g, Font font, int sw, int sh, Origin origin) {
        List<Slot> slots = new ArrayList<>();
        addGroup(slots, SlotGroup.ORIGIN, origin.getThemeColor());
        int originCount = slots.size();
        Specialization spec = ClientSpecializationData.getSpec();
        if (spec != null) addGroup(slots, SlotGroup.CLASS, spec.getThemeColor());
        if (slots.isEmpty()) return;

        boolean hints = AbyssOriginsConfig.showKeybindHints;
        int totalW = slots.size() * SLOT + (slots.size() - 1) * SLOT_GAP + (originCount > 0 && originCount < slots.size() ? GROUP_GAP : 0);
        int totalH = SLOT + (hints ? 11 : 0) + 4;
        int[] p = cornerPos(AbyssOriginsConfig.abilityIndicatorsPosition, sw, sh, totalW, totalH, 6, 6);

        float resource = ClientOriginData.getResourceBar();
        int x = p[0];
        for (int i = 0; i < slots.size(); i++) {
            if (i == originCount && originCount > 0) x += GROUP_GAP;
            Slot s = slots.get(i);
            renderSlot(g, font, s, (s.group == SlotGroup.CLASS ? SlotGroup.ORIGIN.size() : 0) + s.index, x, p[1], resource, hints);
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

    // ---- Helpers -------------------------------------------------------------

    /** Top-left corner for a box anchored to a config corner. */
    static int[] cornerPos(AbyssOriginsConfig.HudPosition pos, int sw, int sh, int w, int h, int insetX, int insetY) {
        return switch (pos) {
            case TOP_LEFT     -> new int[]{ insetX,          insetY };
            case TOP_RIGHT    -> new int[]{ sw - w - insetX, insetY };
            case BOTTOM_LEFT  -> new int[]{ insetX,          sh - h - insetY };
            case BOTTOM_RIGHT -> new int[]{ sw - w - insetX, sh - h - insetY };
        };
    }
}
