package com.gugas749.abyssorigins.client.gui.admin;

import com.gugas749.abyssorigins.admin.ProfileService.Action;
import com.gugas749.abyssorigins.admin.ProfileSnapshot;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssScrollList;
import com.gugas749.abysscore.api.gui.widget.AbyssTextField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Left: every power the player knows (both loadouts), where it's bound and how it was learned.
 * Right: the catalogue to grant from. Hidden abilities (the Eminence's) are not listed in the
 * catalogue — type their exact id in the search to grant one — but once granted they are
 * listed and tracked on the left like any other.
 */
final class AbilitiesTab implements AbyssTab {

    private static final int GAP = 8, ROW_H = 13;

    /** A known power of the profiled player. */
    private record Known(AbilityRegistry.Entry entry, SlotGroup group, Loadouts.Source source, int slot, boolean granted) {}

    private final ProfileScreen screen;
    private String search = "";
    private String selectedKnown = "", selectedCatalog = "";

    AbilitiesTab(ProfileScreen screen) { this.screen = screen; }

    @Override public Component title() { return Component.translatable("gui.abyssorigins.profile.tab.abilities"); }

    @Override
    public void init(TabContext ctx) {
        Font font = ctx.font();
        int cw = (ctx.width - GAP) / 2;
        int listTop = ctx.y + AbyssTextField.HEIGHT + 4;
        int listH = ctx.height - (listTop - ctx.y) - AbyssButton.HEIGHT - 4;

        // Known (left)
        List<Known> known = known();
        AbyssScrollList<Known> left = ctx.add(new AbyssScrollList<Known>(ctx.x, listTop, cw, listH, ROW_H,
            (g, k, x, y, w, h, hover, sel) -> knownRow(g, font, k, x, y, w, sel)));
        left.setItems(known)
            .emptyText(Component.translatable("gui.abyssorigins.profile.no_abilities"));
        known.stream().filter(k -> k.entry().id().equals(selectedKnown)).findFirst().ifPresent(left::setSelected);

        AbyssButton revoke = ctx.add(new AbyssButton(ctx.x, ctx.y + ctx.height - AbyssButton.HEIGHT, cw,
            Component.translatable("gui.abyssorigins.profile.revoke"), AbyssButton.Style.DANGER, b -> {
                Known k = left.getSelected();
                if (k != null && k.granted()) screen.send(Action.REVOKE_ABILITY, k.entry().id(), 0);
            }));
        revoke.active = left.getSelected() != null && left.getSelected().granted();
        left.onSelect(k -> { selectedKnown = k == null ? "" : k.entry().id(); revoke.active = k != null && k.granted(); });

        // Catalogue (right)
        int rx = ctx.x + cw + GAP;
        AbyssTextField field = ctx.add(new AbyssTextField(rx, ctx.y, cw, Component.translatable("gui.abyssorigins.profile.search")));
        field.setValue(search);
        AbyssScrollList<AbilityRegistry.Entry> right = ctx.add(new AbyssScrollList<AbilityRegistry.Entry>(rx, listTop, cw, listH, ROW_H,
            (g, e, x, y, w, h, hover, sel) -> catalogRow(g, font, e, x, y, w, sel)));
        right.setItems(catalog(known)).emptyText(Component.translatable("gui.abyssorigins.profile.no_match"));
        right.getItems().stream().filter(e -> e.id().equals(selectedCatalog)).findFirst().ifPresent(right::setSelected);

        AbyssButton grant = ctx.add(new AbyssButton(rx, ctx.y + ctx.height - AbyssButton.HEIGHT, cw,
            Component.translatable("gui.abyssorigins.profile.grant"), b -> {
                AbilityRegistry.Entry e = right.getSelected();
                if (e != null) screen.send(Action.GRANT_ABILITY, e.id(), 0);
            }));
        grant.active = right.getSelected() != null;
        right.onSelect(e -> { selectedCatalog = e == null ? "" : e.id(); grant.active = e != null; });

        field.setResponder(s -> {
            search = s;
            right.setItems(catalog(known));
            right.setSelected(null);
            selectedCatalog = "";
            grant.active = false;
        });
    }

    @Override
    public void render(GuiGraphics g, TabContext ctx, int mx, int my, float pt) {
        int cw = (ctx.width - GAP) / 2;
        Component label = Component.translatable("gui.abyssorigins.profile.known", known().size());
        g.drawString(ctx.font(), label, ctx.x, ctx.y + 4, AbyssTheme.TITLE, false);
        Component hint = Component.translatable("gui.abyssorigins.profile.slots_hint");
        g.drawString(ctx.font(), AbyssDraw.trimmed(ctx.font(), hint, cw - ctx.font().width(label) - 6),
            ctx.x + ctx.font().width(label) + 6, ctx.y + 4, AbyssTheme.TEXT_OFF, false);
    }

    // ---- Data ------------------------------------------------------------------------------

    private List<Known> known() {
        ProfileSnapshot s = screen.snapshot();
        List<Known> out = new ArrayList<>();
        for (SlotGroup g : SlotGroup.values()) {
            for (Map.Entry<String, Loadouts.Source> e : s.known(g).entrySet()) {
                AbilityRegistry.Entry entry = AbilityRegistry.get(e.getKey());
                if (entry == null) continue;
                out.add(new Known(entry, g, e.getValue(), s.slots(g).indexOf(e.getKey()), s.granted().contains(e.getKey())));
            }
        }
        // Granted but not usable right now (e.g. a class power while classless) — still tracked
        for (String id : s.granted()) {
            AbilityRegistry.Entry entry = AbilityRegistry.get(id);
            if (entry != null && out.stream().noneMatch(k -> k.entry().id().equals(id)))
                out.add(new Known(entry, entry.group(), Loadouts.Source.GRANTED, -1, true));
        }
        return out;
    }

    /** Public abilities matching the search that the player doesn't already have, plus a hidden one on exact id. */
    private List<AbilityRegistry.Entry> catalog(List<Known> known) {
        Set<String> have = new HashSet<>();
        known.forEach(k -> have.add(k.entry().id()));
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<AbilityRegistry.Entry> out = new ArrayList<>();
        for (AbilityRegistry.Entry e : AbilityRegistry.all()) {
            if (have.contains(e.id())) continue;
            if (e.hidden()) { if (e.id().equals(q)) out.add(e); continue; }
            if (q.isEmpty() || e.id().contains(q) || e.ability().getDisplayName().getString().toLowerCase(Locale.ROOT).contains(q))
                out.add(e);
        }
        return out;
    }

    // ---- Rows ------------------------------------------------------------------------------

    private static void knownRow(GuiGraphics g, Font font, Known k, int x, int y, int w, boolean sel) {
        Ability a = k.entry().ability();
        g.drawString(font, a.getIcon(), x + 3, y + 3, a.getColor(), false);
        String tag = k.slot() >= 0 ? (k.group() == SlotGroup.ORIGIN ? "O" : "C") + (k.slot() + 1) : "";
        if (k.source() != Loadouts.Source.DEFAULT) tag += (tag.isEmpty() ? "" : " ") + sourceTag(k.source());
        if (k.entry().hidden()) tag += " ✦";
        int tw = font.width(tag);
        g.drawString(font, AbyssDraw.trimmed(font, a.getDisplayName(), w - 18 - tw - 4), x + 14, y + 3,
            sel ? AbyssTheme.ACCENT : AbyssTheme.TEXT, false);
        g.drawString(font, tag, x + w - tw - 3, y + 3, k.granted() ? 0xFFE8C872 : AbyssTheme.TEXT_DIM, false);
    }

    private static void catalogRow(GuiGraphics g, Font font, AbilityRegistry.Entry e, int x, int y, int w, boolean sel) {
        Ability a = e.ability();
        g.drawString(font, a.getIcon(), x + 3, y + 3, a.getColor(), false);
        String owner = e.ownerSigil();
        int ow = font.width(owner);
        g.drawString(font, AbyssDraw.trimmed(font, a.getDisplayName(), w - 18 - ow - 4), x + 14, y + 3,
            sel ? AbyssTheme.ACCENT : e.hidden() ? 0xFFE8C872 : AbyssTheme.TEXT, false);
        g.drawString(font, owner, x + w - ow - 3, y + 3, 0xFF000000 | (e.ownerColor() & 0xFFFFFF), false);
    }

    private static String sourceTag(Loadouts.Source s) {
        return Component.translatable("gui.abyssorigins.abilities.source." + s.name().toLowerCase(Locale.ROOT)).getString();
    }
}
