package com.gugas749.abyssorigins.client.gui.admin;

import com.gugas749.abyssorigins.admin.ProfileService.Action;
import com.gugas749.abyssorigins.admin.ProfileSnapshot;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssScrollList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Change origin / class. Hidden origins are only listed when the player already has one —
 * grant them with /abyssorigins set, as before. Changing refunds the old tree (confirmed first).
 */
final class AssignTab implements AbyssTab {

    private static final int GAP = 8, ROW_H = 14, TITLE_H = 14;

    /** id "" = none. */
    private record Choice(String id, Component name, String sigil, int color) {}

    private final ProfileScreen screen;
    private String originPick = null, classPick = null;

    AssignTab(ProfileScreen screen) { this.screen = screen; }

    @Override public Component title() { return Component.translatable("gui.abyssorigins.profile.tab.assign"); }

    @Override
    public void init(TabContext ctx) {
        ProfileSnapshot s = screen.snapshot();
        Font font = ctx.font();
        int cw = (ctx.width - GAP) / 2, top = ctx.y + TITLE_H;
        int listH = ctx.height - TITLE_H - AbyssButton.HEIGHT - 4, by = ctx.y + ctx.height - AbyssButton.HEIGHT;

        List<Choice> origins = new ArrayList<>();
        origins.add(none());
        for (Origin o : AbyssOriginsAPI.getAllOrigins())
            if (!o.isHidden() || o.getId().toString().equals(s.originId()))
                origins.add(new Choice(o.getId().toString(), o.getDisplayName(), o.getSigil(), o.getThemeColor()));
        List<Choice> classes = new ArrayList<>();
        classes.add(none());
        for (Specialization sp : SpecializationRegistry.getAll())
            classes.add(new Choice(sp.getId().toString(), sp.getDisplayName(), sp.getSigil(), sp.getThemeColor()));

        AbyssScrollList<Choice> left = ctx.add(new AbyssScrollList<Choice>(ctx.x, top, cw, listH, ROW_H,
            (g, c, x, y, w, h, hover, sel) -> row(g, font, c, x, y, w, sel, c.id().equals(s.originId()))));
        left.setItems(origins);
        AbyssButton setOrigin = ctx.add(new AbyssButton(ctx.x, by, cw, Component.translatable("gui.abyssorigins.profile.apply_origin"),
            AbyssButton.Style.DANGER, b -> {
                Choice c = left.getSelected();
                if (c != null) screen.confirm(Component.translatable("gui.abyssorigins.profile.apply_origin"),
                    Component.translatable("gui.abyssorigins.profile.change_confirm", s.name(), c.name()),
                    () -> screen.send(Action.SET_ORIGIN, c.id(), 0));
            }));
        select(left, origins, originPick);
        setOrigin.active = left.getSelected() != null && !left.getSelected().id().equals(s.originId());
        left.onSelect(c -> { originPick = c == null ? null : c.id(); setOrigin.active = c != null && !c.id().equals(s.originId()); });

        int rx = ctx.x + cw + GAP;
        AbyssScrollList<Choice> right = ctx.add(new AbyssScrollList<Choice>(rx, top, cw, listH, ROW_H,
            (g, c, x, y, w, h, hover, sel) -> row(g, font, c, x, y, w, sel, c.id().equals(s.classId()))));
        right.setItems(classes);
        AbyssButton setClass = ctx.add(new AbyssButton(rx, by, cw, Component.translatable("gui.abyssorigins.profile.apply_class"),
            AbyssButton.Style.DANGER, b -> {
                Choice c = right.getSelected();
                if (c != null) screen.confirm(Component.translatable("gui.abyssorigins.profile.apply_class"),
                    Component.translatable("gui.abyssorigins.profile.change_confirm", s.name(), c.name()),
                    () -> screen.send(Action.SET_CLASS, c.id(), 0));
            }));
        select(right, classes, classPick);
        setClass.active = right.getSelected() != null && !right.getSelected().id().equals(s.classId());
        right.onSelect(c -> { classPick = c == null ? null : c.id(); setClass.active = c != null && !c.id().equals(s.classId()); });
    }

    @Override
    public void render(GuiGraphics g, TabContext ctx, int mx, int my, float pt) {
        int cw = (ctx.width - GAP) / 2;
        AbyssDraw.sectionTitle(g, ctx.font(), Component.translatable("gui.abyssorigins.profile.origin"), ctx.x, ctx.y);
        AbyssDraw.sectionTitle(g, ctx.font(), Component.translatable("gui.abyssorigins.profile.class"), ctx.x + cw + GAP, ctx.y);
    }

    private static Choice none() {
        return new Choice("", Component.translatable("gui.abyssorigins.profile.none"), "–", 0xFF5E5880);
    }

    private static void select(AbyssScrollList<Choice> list, List<Choice> items, String id) {
        if (id == null) return;
        items.stream().filter(c -> c.id().equals(id)).findFirst().ifPresent(list::setSelected);
    }

    private static void row(GuiGraphics g, Font font, Choice c, int x, int y, int w, boolean sel, boolean current) {
        g.drawString(font, c.sigil(), x + 3, y + 3, 0xFF000000 | (c.color() & 0xFFFFFF), false);
        Component tag = current ? Component.translatable("gui.abyssorigins.profile.current") : Component.empty();
        int tw = font.width(tag);
        g.drawString(font, AbyssDraw.trimmed(font, c.name(), w - 18 - tw - 4), x + 14, y + 3,
            sel ? AbyssTheme.ACCENT : AbyssTheme.TEXT, false);
        if (current) g.drawString(font, tag, x + w - tw - 3, y + 3, AbyssTheme.TEXT_DIM, false);
    }
}
