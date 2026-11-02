package com.gugas749.abyssorigins.client.gui.admin;

import com.gugas749.abyssorigins.admin.ProfileService;
import com.gugas749.abyssorigins.admin.ProfileService.Action;
import com.gugas749.abyssorigins.admin.ProfileSnapshot;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.ResourceType;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.event.XpEventHandler;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Origin and class side by side: identity, level, XP, points — with the edit buttons under each. */
final class OverviewTab implements AbyssTab {

    private static final int GAP = 8, BTN_GAP = 4, TEXT_H = 64;

    private final ProfileScreen screen;

    OverviewTab(ProfileScreen screen) { this.screen = screen; }

    @Override public Component title() { return Component.translatable("gui.abyssorigins.profile.tab.overview"); }

    private int colW(TabContext ctx) { return (ctx.width - GAP) / 2; }

    @Override
    public void init(TabContext ctx) {
        ProfileSnapshot s = screen.snapshot();
        int cw = colW(ctx), bw = (cw - BTN_GAP) / 2, by = ctx.y + TEXT_H;

        // Origin column
        int x = ctx.x;
        boolean hasOrigin = s.origin() != null;
        ctx.add(new AbyssButton(x, by, bw, Component.translatable("gui.abyssorigins.profile.add_xp"), b ->
            screen.askAmount(Component.translatable("gui.abyssorigins.profile.add_origin_xp"), ProfileService.MAX_XP,
                v -> screen.send(Action.ADD_ORIGIN_XP, "", v)))).active = hasOrigin;
        ctx.add(new AbyssButton(x + bw + BTN_GAP, by, bw, Component.translatable("gui.abyssorigins.profile.set_level"), b ->
            screen.askAmount(Component.translatable("gui.abyssorigins.profile.set_origin_level"), XpEventHandler.MAX_LEVEL,
                v -> screen.send(Action.SET_ORIGIN_LEVEL, "", v))));
        ctx.add(new AbyssButton(x, by + 24, bw, Component.translatable("gui.abyssorigins.profile.add_points"), b ->
            screen.askAmount(Component.translatable("gui.abyssorigins.profile.add_origin_points"), ProfileService.MAX_POINTS,
                v -> screen.send(Action.ADD_ORIGIN_POINTS, "", v))));
        ctx.add(new AbyssButton(x + bw + BTN_GAP, by + 24, bw, Component.translatable("gui.abyssorigins.profile.respec"),
            AbyssButton.Style.DANGER, b -> screen.confirm(Component.translatable("gui.abyssorigins.profile.respec_origin"),
                Component.translatable("gui.abyssorigins.profile.respec_confirm", s.name()), () -> screen.send(Action.RESPEC_ORIGIN))))
            .active = hasOrigin && !s.originSkills().isEmpty();
        ctx.add(new AbyssButton(x, by + 48, cw, Component.translatable("gui.abyssorigins.profile.change_origin"), b -> screen.goToTab(3)));

        // Class column
        x = ctx.x + cw + GAP;
        boolean hasClass = s.spec() != null;
        ctx.add(new AbyssButton(x, by, bw, Component.translatable("gui.abyssorigins.profile.add_xp"), b ->
            screen.askAmount(Component.translatable("gui.abyssorigins.profile.add_class_xp"), ProfileService.MAX_XP,
                v -> screen.send(Action.ADD_CLASS_XP, "", v)))).active = hasClass;
        ctx.add(new AbyssButton(x + bw + BTN_GAP, by, bw, Component.translatable("gui.abyssorigins.profile.set_level"), b ->
            screen.askAmount(Component.translatable("gui.abyssorigins.profile.set_class_level"), XpEventHandler.MAX_LEVEL,
                v -> screen.send(Action.SET_CLASS_LEVEL, "", v)))).active = hasClass;
        ctx.add(new AbyssButton(x, by + 24, bw, Component.translatable("gui.abyssorigins.profile.add_points"), b ->
            screen.askAmount(Component.translatable("gui.abyssorigins.profile.add_class_points"), ProfileService.MAX_POINTS,
                v -> screen.send(Action.ADD_CLASS_POINTS, "", v)))).active = hasClass;
        ctx.add(new AbyssButton(x + bw + BTN_GAP, by + 24, bw, Component.translatable("gui.abyssorigins.profile.respec"),
            AbyssButton.Style.DANGER, b -> screen.confirm(Component.translatable("gui.abyssorigins.profile.respec_class"),
                Component.translatable("gui.abyssorigins.profile.respec_confirm", s.name()), () -> screen.send(Action.RESPEC_CLASS))))
            .active = hasClass && !s.classSkills().isEmpty();
        ctx.add(new AbyssButton(x, by + 48, cw, Component.translatable("gui.abyssorigins.profile.change_class"), b -> screen.goToTab(3)));
    }

    @Override
    public void render(GuiGraphics g, TabContext ctx, int mx, int my, float pt) {
        ProfileSnapshot s = screen.snapshot();
        Font font = ctx.font();
        int cw = colW(ctx);

        // Origin
        int x = ctx.x, y = ctx.y;
        Origin o = s.origin();
        AbyssDraw.sectionTitle(g, font, Component.translatable("gui.abyssorigins.profile.origin"), x, y);
        line(g, font, o == null ? Component.translatable("gui.abyssorigins.profile.none") : Component.literal(o.getSigil() + " ").append(o.getDisplayName()),
            x, y + 13, cw, o == null ? AbyssTheme.TEXT_OFF : 0xFF000000 | (o.getThemeColor() & 0xFFFFFF));
        line(g, font, Component.translatable("gui.abyssorigins.profile.level_xp", s.originLevel(), s.originXp()), x, y + 25, cw, AbyssTheme.TEXT);
        line(g, font, Component.translatable("gui.abyssorigins.profile.points_skills", s.originPoints(), s.originSkills().size()), x, y + 36, cw, AbyssTheme.TEXT_DIM);
        ResourceType rt = o != null ? o.getResourceType() : null;
        if (rt != null)
            line(g, font, Component.translatable("gui.abyssorigins.profile.resource", rt.getDisplayName(), Math.round(s.resource()), rt.getMaxAmount()),
                x, y + 47, cw, 0xFF000000 | (rt.getBarColor() & 0xFFFFFF));

        // Class
        x = ctx.x + cw + GAP;
        Specialization sp = s.spec();
        AbyssDraw.sectionTitle(g, font, Component.translatable("gui.abyssorigins.profile.class"), x, y);
        line(g, font, sp == null ? Component.translatable("gui.abyssorigins.profile.none") : Component.literal(sp.getSigil() + " ").append(sp.getDisplayName()),
            x, y + 13, cw, sp == null ? AbyssTheme.TEXT_OFF : 0xFF000000 | (sp.getThemeColor() & 0xFFFFFF));
        if (sp != null) {
            line(g, font, Component.translatable("gui.abyssorigins.profile.level_xp", Math.max(1, s.classLevel()), s.classXp()), x, y + 25, cw, AbyssTheme.TEXT);
            line(g, font, Component.translatable("gui.abyssorigins.profile.points_skills", s.classPoints(), s.classSkills().size()), x, y + 36, cw, AbyssTheme.TEXT_DIM);
        }
        line(g, font, Component.translatable("gui.abyssorigins.profile.granted_count", s.granted().size()), x, y + 47, cw, AbyssTheme.TEXT_DIM);
    }

    private static void line(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        g.drawString(font, AbyssDraw.trimmed(font, text, w), x, y, color, false);
    }
}
