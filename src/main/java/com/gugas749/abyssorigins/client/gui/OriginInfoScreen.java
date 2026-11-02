package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.client.ClientAbilityData;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.ClientTimers;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import com.gugas749.abyssorigins.client.input.KeyBindings;
import com.gugas749.abyssorigins.event.XpEventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * The Codex — origin & class dashboard.
 * Left: who you are (origin, traits, resource). Middle: what you can do (abilities,
 * passives). Right: your class, its resonance with your origin, and progression.
 */
public class OriginInfoScreen extends Screen {

    private Origin origin;
    private Specialization spec;
    private long openedAt;

    private record Card(Ability ability, SlotGroup group, int slot, int x, int y, int w, int h) {}
    private final List<Card> cards = new ArrayList<>();
    private int[] treeButton = new int[4];
    private int[] abilitiesButton = new int[4];
    private List<FormattedCharSequence> tooltip = null;

    public OriginInfoScreen() {
        super(Component.translatable("gui.abyssorigins.origin_info"));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft == null || minecraft.player == null) { onClose(); return; }
        origin = ClientOriginData.getOrigin();
        if (origin == null) origin = AbyssOriginsAPI.getPlayerOrigin(minecraft.player);
        if (origin == null) { onClose(); return; }
        spec = ClientSpecializationData.getSpec();
        openedAt = System.currentTimeMillis();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (origin == null) return;
        cards.clear();
        tooltip = null;
        long now = System.currentTimeMillis();
        float fade = easeOut((now - openedAt) / 350f);
        int accent = origin.getThemeColor();

        backdrop(g, width, height, accent, fade);

        // Title
        centered(g, font, Component.translatable("gui.abyssorigins.codex").withStyle(ChatFormatting.ITALIC), width / 2, 6, alpha(TEXT_FAINT, fade));
        MutableComponent title = origin.getDisplayName().copy();
        if (spec != null) title.append(Component.literal("  ◆  ").withColor(TEXT_FAINT & 0xFFFFFF)).append(spec.getDisplayName());
        float ts = 1.35f;
        scaledText(g, font, title, width / 2f - font.width(title) * ts / 2f, 17, ts, alpha(lighten(accent, 0.3f), fade), true);

        int cw = Math.min(width - 20, 780), x0 = (width - cw) / 2, top = 36, bottom = height - 16;
        int gap = 8;
        int w1 = (int) (cw * 0.32f), w2 = (int) (cw * 0.37f), w3 = cw - w1 - w2 - gap * 2;
        int h = bottom - top;

        column(g, 0, now, fade, x0, top, w1, h, accent, () -> drawOrigin(g, x0, top, w1, h, fade));
        column(g, 1, now, fade, x0 + w1 + gap, top, w2, h, accent, () -> drawPowers(g, x0 + w1 + gap, top, w2, h, mx, my, fade));
        int c3 = x0 + w1 + w2 + gap * 2;
        column(g, 2, now, fade, c3, top, w3, h, spec != null ? spec.getThemeColor() : accent, () -> drawClass(g, c3, top, w3, h, mx, my, fade));

        centered(g, font, Component.translatable("gui.abyssorigins.codex.hint"), width / 2, height - 11, alpha(TEXT_FAINT, fade));
        super.render(g, mx, my, pt);
        if (tooltip != null) g.renderTooltip(font, tooltip, mx, my);
    }

    /** Staggered fade/slide-in per column, clipped to its panel. */
    private void column(GuiGraphics g, int index, long now, float fade, int x, int y, int w, int h, int accent, Runnable body) {
        float f = easeOut((now - openedAt - index * 90L) / 380f) * fade;
        if (f <= 0.01f) return;
        int dy = (int) ((1 - f) * 12);
        g.pose().pushPose();
        g.pose().translate(0, dy, 0);
        panel(g, x, y, w, h, accent, f);
        g.enableScissor(x + 1, y + 1 + dy, x + w - 1, y + h - 1 + dy);
        body.run();
        g.disableScissor();
        g.pose().popPose();
    }

    // ---- Left: origin --------------------------------------------------------

    private void drawOrigin(GuiGraphics g, int x, int y, int w, int h, float fade) {
        int accent = origin.getThemeColor();
        int cx = x + w / 2, iy = y + 10;

        glow(g, cx, iy + 18, 34, accent, (0.5f + 0.2f * pulse(3f)) * fade);
        diamondRing(g, cx, iy + 18, 17, 1.6f, alpha(accent, fade), alpha(0xFF0B0816, fade));
        scaledText(g, font, Component.literal(origin.getSigil()), cx - font.width(origin.getSigil()) * 0.8f, iy + 12, 1.6f, alpha(accent, fade), true);
        iy += 42;

        float ns = 1.3f;
        scaledText(g, font, origin.getDisplayName(), cx - font.width(origin.getDisplayName()) * ns / 2, iy, ns, alpha(TEXT, fade), true);
        iy += 14;
        int impact = origin.getImpactLevel().ordinal();
        int ic = impact == 0 ? GOOD : impact == 1 ? GOLD : BAD;
        for (int i = 0; i < 3; i++) diamond(g, cx - 10 + i * 10, iy + 3, 3, alpha(i <= impact ? ic : EDGE, fade));
        iy += 12;

        iy = wrapped(g, font, origin.getDescription(), x + 8, iy, w - 16, alpha(TEXT_DIM, fade), 6) + 6;

        if (!origin.getTraits().isEmpty()) {
            iy += header(g, font, x + 8, iy, w - 16, Component.translatable("gui.abyssorigins.traits"), accent, fade);
            for (Origin.Trait t : origin.getTraits()) {
                g.drawString(font, t.positive() ? "+" : "−", x + 9, iy, alpha(t.positive() ? GOOD : BAD, fade), false);
                iy = wrapped(g, font, t.text(), x + 18, iy, w - 26, alpha(t.positive() ? TEXT : mix(TEXT, BAD, 0.35f), fade), 3) + 2;
            }
            iy += 4;
        }

        ResourceType rt = origin.getResourceType();
        if (rt != null && iy < y + h - 30) {
            iy += header(g, font, x + 8, iy, w - 16, rt.getDisplayName(), rt.getBarColor(), fade);
            float v = ClientOriginData.getResourceBar();
            bar(g, x + 9, iy + 2, w - 18, 6, v / 100f, rt.getBarColor(), true);
            String num = Math.round(v) + " / " + rt.getMaxAmount();
            g.drawString(font, num, x + w - 9 - font.width(num), iy + 11, alpha(TEXT_DIM, fade), false);
        }
    }

    // ---- Middle: abilities + passives ------------------------------------------

    private void drawPowers(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float fade) {
        int iy = y + 8;
        int hy = iy;
        iy += header(g, font, x + 8, iy, w - 16, Component.translatable("gui.abyssorigins.abilities"), origin.getThemeColor(), fade) + 2;

        // "Manage" link to the Abilities tab, on the header line
        Component manage = Component.translatable("gui.abyssorigins.manage_abilities", KeyBindings.ABILITIES.getTranslatedKeyMessage().getString());
        int lw = font.width(manage) + 6, lx = x + w - 8 - lw;
        boolean lh = inside(mx, my, lx, hy - 2, lw, 12);
        g.fill(lx, hy - 2, lx + lw, hy + 10, alpha(lh ? withAlpha(origin.getThemeColor(), 70) : 0xE00F0C1D, fade));
        border(g, lx, hy - 2, lw, 12, alpha(lh ? lighten(origin.getThemeColor(), 0.3f) : EDGE, fade));
        g.drawString(font, manage, lx + 3, hy, alpha(lh ? 0xFFFFFFFF : GOLD, fade), false);
        abilitiesButton = new int[]{ lx, hy - 2, lw, 12 };

        // The bound loadouts, in slot order (empty slots are left to the Abilities tab)
        List<Card> bound = new ArrayList<>();
        for (SlotGroup group : SlotGroup.values()) {
            if (group == SlotGroup.CLASS && spec == null) continue;
            for (int i = 0; i < group.size(); i++) {
                Ability a = ClientAbilityData.ability(group, i);
                if (a != null) bound.add(new Card(a, group, i, 0, 0, 0, 0));
            }
        }
        boolean compact = bound.size() > 4;          // a full loadout: name + meta only
        for (Card c : bound)
            iy = abilityCard(g, c.ability(), c.group(), c.slot(), x + 8, iy, w - 16, mx, my, fade, compact,
                ClientAbilityData.cooldown(c.ability().getId()), ClientAbilityData.cooldownMax(c.ability().getId())) + (compact ? 3 : 4);

        iy += 4;
        iy += header(g, font, x + 8, iy, w - 16, Component.translatable("gui.abyssorigins.passives"), origin.getThemeColor(), fade) + 2;
        for (OriginPassive p : origin.getPassives()) {
            if (p.isHidden()) continue;
            iy = passiveRow(g, p.getDisplayName(), p.getDescription(), origin.getThemeColor(), x + 8, iy, w - 16, mx, my, fade);
        }
        if (spec != null)
            for (SpecializationPassive p : spec.getPassives())
                iy = passiveRow(g, p.getDisplayName(), p.getDescription(), spec.getThemeColor(), x + 8, iy, w - 16, mx, my, fade);
    }

    private int abilityCard(GuiGraphics g, Ability a, SlotGroup group, int slot, int x, int y, int w,
                            int mx, int my, float fade, boolean compact, int cd, int maxCd) {
        boolean classAbility = group == SlotGroup.CLASS;
        int h = compact ? 24 : 34;
        boolean hover = inside(mx, my, x, y, w, h);
        boolean active = ClientTimers.isActive(a.getId());
        int color = a.getColor();

        if (active) glow(g, x + 14, y + h / 2f, 24, color, 0.5f * fade);
        g.fillGradient(x, y, x + w, y + h, alpha(hover ? 0xF0201A3A : 0xE0150F28, fade), alpha(0xE00B0816, fade));
        border(g, x, y, w, h, alpha(active ? color : hover ? mix(EDGE, color, 0.6f) : EDGE, fade));

        diamondRing(g, x + 14, y + h / 2f, 10, 1.2f, alpha(cd > 0 ? EDGE : color, fade), alpha(0xFF0B0816, fade));
        centered(g, font, a.getIcon(), x + 14, y + h / 2 - 4, alpha(cd > 0 ? TEXT_FAINT : color, fade));

        g.drawString(font, a.getDisplayName(), x + 30, y + 4, alpha(TEXT, fade), false);
        String key = KeyBindings.label(group, slot, 6);
        int kw = font.width(key) + 6;
        g.fill(x + w - kw - 4, y + 3, x + w - 4, y + 13, alpha(0xC0080611, fade));
        border(g, x + w - kw - 4, y + 3, kw, 10, alpha(EDGE, fade));
        g.drawString(font, key, x + w - kw - 1, y + 4, alpha(GOLD, fade), false);

        List<FormattedCharSequence> desc = font.split(a.getDescription(), w - 36);
        if (!compact && !desc.isEmpty()) g.drawString(font, desc.get(0), x + 30, y + 14, alpha(TEXT_DIM, fade), false);

        MutableComponent meta = Component.empty();
        if (a.getResourceCost() > 0 && origin.getResourceType() != null)
            meta.append(Component.literal(a.getResourceCost() + " ")).append(origin.getResourceType().getDisplayName()).append("  ·  ");
        meta.append(Component.translatable("gui.abyssorigins.cooldown_secs", a.getBaseCooldownTicks() / 20));
        if (classAbility) meta.append(Component.literal("  ·  ")).append(Component.translatable("gui.abyssorigins.class_power"));
        g.drawString(font, meta, x + 30, y + (compact ? 14 : 24), alpha(TEXT_FAINT, fade), false);

        if (cd > 0) {
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, alpha(0xFF09070F, fade));
            g.fill(x + 1, y + h - 2, x + 1 + (int) ((w - 2) * (1f - cd / (float) maxCd)), y + h - 1, alpha(color, fade));
        }
        if (hover) tooltip = font.split(a.getDescription(), 220);
        cards.add(new Card(a, group, slot, x, y, w, h));
        return y + h;
    }

    private int passiveRow(GuiGraphics g, Component name, Component desc, int color, int x, int y, int w,
                           int mx, int my, float fade) {
        boolean hover = inside(mx, my, x, y - 1, w, 11);
        if (hover) g.fill(x, y - 1, x + w, y + 10, alpha(0x30FFFFFF, fade));
        diamond(g, x + 3, y + 4, 2.5f, alpha(color, fade));
        g.drawString(font, name, x + 10, y, alpha(hover ? TEXT : TEXT_DIM, fade), false);
        if (hover) tooltip = font.split(desc, 220);
        return y + 11;
    }

    // ---- Right: class, resonance, progression -----------------------------------

    private void drawClass(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float fade) {
        int iy = y + 8, ix = x + 8, iw = w - 16;

        if (spec != null) {
            int sc = spec.getThemeColor();
            iy += header(g, font, ix, iy, iw, Component.translatable("gui.abyssorigins.class"), sc, fade) + 2;
            diamondRing(g, ix + 9, iy + 9, 9, 1.2f, alpha(sc, fade), alpha(0xFF0B0816, fade));
            centered(g, font, spec.getSigil(), ix + 9, iy + 5, alpha(sc, fade));
            g.drawString(font, spec.getDisplayName(), ix + 22, iy + 1, alpha(lighten(sc, 0.3f), fade), true);
            g.drawString(font, Component.translatable("gui.abyssorigins.level", Math.max(1, ClientSpecializationData.getSpecLevel())),
                ix + 22, iy + 11, alpha(TEXT_DIM, fade), false);
            iy += 22;
            bar(g, ix, iy, iw, 3, ClientSpecializationData.getXpProgress(), sc, true);
            iy += 8;
            iy = wrapped(g, font, spec.getDescription(), ix, iy, iw, alpha(TEXT_DIM, fade), 4) + 6;

            Resonance res = spec.getResonance(origin.getPath()).orElse(null);
            if (res != null) {
                int bh = 14 + Math.min(4, font.split(res.description(), iw - 10).size()) * 10;
                glow(g, ix + iw / 2f, iy + bh / 2f, iw * 0.55f, GOLD, 0.25f * fade);
                g.fill(ix, iy, ix + iw, iy + bh, alpha(0x60140F06, fade));
                border(g, ix, iy, iw, bh, alpha(mix(GOLD, 0xFFFFFFFF, 0.3f * pulse(2f)), fade));
                g.drawString(font, Component.literal("✦ ").append(res.name()), ix + 5, iy + 3, alpha(GOLD, fade), true);
                wrapped(g, font, res.description(), ix + 5, iy + 14, iw - 10, alpha(TEXT, fade), 4);
                iy += bh + 6;
            } else if (!spec.getResonances().isEmpty()) {
                MutableComponent list = Component.translatable("gui.abyssorigins.resonates_with");
                boolean first = true;
                for (Resonance r : spec.getResonances()) {
                    Origin o = AbyssOriginsAPI.getOrigin(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, r.originPath()));
                    list.append(first ? " " : ", ").append(o != null ? o.getDisplayName() : Component.literal(r.originPath()));
                    first = false;
                }
                iy = wrapped(g, font, list, ix, iy, iw, alpha(TEXT_FAINT, fade), 3) + 6;
            }
        } else {
            iy += header(g, font, ix, iy, iw, Component.translatable("gui.abyssorigins.class"), TEXT_FAINT, fade) + 2;
            iy = wrapped(g, font, Component.translatable("gui.abyssorigins.no_class"), ix, iy, iw, alpha(TEXT_FAINT, fade), 4) + 8;
        }

        // Progression
        iy += header(g, font, ix, iy, iw, Component.translatable("gui.abyssorigins.progression"), origin.getThemeColor(), fade) + 2;
        g.drawString(font, Component.translatable("gui.abyssorigins.origin_level", ClientOriginData.getLevel()), ix, iy, alpha(GOLD, fade), true);
        iy += 11;
        bar(g, ix, iy, iw, 4, ClientOriginData.getXpProgress(), origin.getThemeColor(), true);
        iy += 7;
        String xp = ClientOriginData.getXpIntoLevel() + " / " + XpEventHandler.xpForLevel(ClientOriginData.getLevel()) + " XP";
        g.drawString(font, xp, ix, iy, alpha(TEXT_FAINT, fade), false);
        iy += 13;
        int op = ClientOriginData.getSkillPoints(), cp = spec != null ? ClientSpecializationData.getSkillPoints() : 0;
        Component pts = Component.translatable("gui.abyssorigins.points_split", op, cp);
        g.drawString(font, pts, ix, iy, alpha(op + cp > 0 ? mix(GOLD, 0xFFFFF4C8, pulse(1.5f)) : TEXT_DIM, fade), false);

        // Skill tree button
        treeButton = button(g, ix, y + h - 26, iw, mx, my, fade,
            Component.translatable("gui.abyssorigins.open_tree", KeyBindings.SKILL_TREE.getTranslatedKeyMessage().getString()));
    }

    private int[] button(GuiGraphics g, int x, int y, int w, int mx, int my, float fade, Component label) {
        int accent = origin.getThemeColor();
        boolean hover = inside(mx, my, x, y, w, 18);
        g.fill(x, y, x + w, y + 18, alpha(hover ? withAlpha(accent, 70) : 0x40000000, fade));
        border(g, x, y, w, 18, alpha(hover ? lighten(accent, 0.3f) : mix(EDGE, accent, 0.5f), fade));
        centered(g, font, label, x + w / 2, y + 5, alpha(hover ? 0xFFFFFFFF : TEXT, fade));
        return new int[]{ x, y, w, 18 };
    }

    // ---- Input -------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (inside(mx, my, treeButton[0], treeButton[1], treeButton[2], treeButton[3])) { openTree(); return true; }
            if (inside(mx, my, abilitiesButton[0], abilitiesButton[1], abilitiesButton[2], abilitiesButton[3])) { openAbilities(); return true; }
            for (Card c : cards) if (inside(mx, my, c.x, c.y, c.w, c.h)) { activate(c.group, c.slot); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scan, int mods) {
        for (SlotGroup group : SlotGroup.values()) {
            KeyMapping[] keys = KeyBindings.forGroup(group);
            for (int i = 0; i < keys.length; i++)
                if (keys[i].matches(keyCode, scan)) { activate(group, i); return true; }
        }
        if (KeyBindings.SKILL_TREE.matches(keyCode, scan))     { openTree(); return true; }
        if (KeyBindings.ABILITIES.matches(keyCode, scan))      { openAbilities(); return true; }
        if (KeyBindings.ORIGIN_INFO.matches(keyCode, scan))    { onClose(); return true; }
        return super.keyPressed(keyCode, scan, mods);
    }

    private void openTree() {
        if (minecraft == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
        minecraft.setScreen(new SkillTreeScreen(this));
    }

    private void openAbilities() {
        if (minecraft == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
        minecraft.setScreen(new AbilitiesScreen(this, SlotGroup.ORIGIN));
    }

    private void activate(SlotGroup group, int slot) {
        if (group == SlotGroup.CLASS && spec == null) return;
        if (ClientAbilityData.slot(group, slot).isEmpty()) return;
        ClientAbilityData.activate(group, slot);
        onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
