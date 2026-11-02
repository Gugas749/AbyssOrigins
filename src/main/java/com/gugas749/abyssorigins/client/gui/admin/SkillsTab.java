package com.gugas749.abyssorigins.client.gui.admin;

import com.gugas749.abyssorigins.admin.ProfileSnapshot;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssScrollList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The unlocked nodes of both trees (read-only — respec from the Overview tab). */
final class SkillsTab implements AbyssTab {

    private static final int GAP = 8, ROW_H = 12, TITLE_H = 14;

    private record Row(String id, SkillNode node) {}

    private final ProfileScreen screen;

    SkillsTab(ProfileScreen screen) { this.screen = screen; }

    @Override public Component title() { return Component.translatable("gui.abyssorigins.profile.tab.skills"); }

    @Override
    public void init(TabContext ctx) {
        ProfileSnapshot s = screen.snapshot();
        Font font = ctx.font();
        int cw = (ctx.width - GAP) / 2, top = ctx.y + TITLE_H, h = ctx.height - TITLE_H;

        Origin o = s.origin();
        SkillTree originTree = o == null ? null : SkillTreeRegistry.getTree(o.getId()).orElse(null);
        Specialization sp = s.spec();
        SkillTree classTree = sp == null ? null : sp.getSkillTree();

        ctx.add(new AbyssScrollList<Row>(ctx.x, top, cw, h, ROW_H, (g, r, x, y, w, rh, hover, sel) -> row(g, font, r, x, y, w)))
            .setItems(rows(originTree, s.originSkills())).selectable(false)
            .emptyText(Component.translatable("gui.abyssorigins.profile.no_skills"));
        ctx.add(new AbyssScrollList<Row>(ctx.x + cw + GAP, top, cw, h, ROW_H, (g, r, x, y, w, rh, hover, sel) -> row(g, font, r, x, y, w)))
            .setItems(rows(classTree, s.classSkills())).selectable(false)
            .emptyText(Component.translatable("gui.abyssorigins.profile.no_skills"));
    }

    @Override
    public void render(GuiGraphics g, TabContext ctx, int mx, int my, float pt) {
        ProfileSnapshot s = screen.snapshot();
        int cw = (ctx.width - GAP) / 2;
        AbyssDraw.sectionTitle(g, ctx.font(), Component.translatable("gui.abyssorigins.profile.origin_skills", s.originSkills().size()), ctx.x, ctx.y);
        AbyssDraw.sectionTitle(g, ctx.font(), Component.translatable("gui.abyssorigins.profile.class_skills", s.classSkills().size()), ctx.x + cw + GAP, ctx.y);
    }

    private static List<Row> rows(SkillTree tree, List<String> ids) {
        List<Row> out = new ArrayList<>();
        for (String id : ids) out.add(new Row(id, tree == null ? null : tree.getSkill(id)));
        return out;
    }

    private static void row(GuiGraphics g, Font font, Row r, int x, int y, int w) {
        if (r.node() == null) {   // left over from an old tree / another origin
            g.drawString(font, AbyssDraw.trimmed(font, Component.literal(r.id()), w - 6), x + 3, y + 2, AbyssTheme.TEXT_OFF, false);
            return;
        }
        SkillNode n = r.node();
        g.drawString(font, n.getIcon(), x + 3, y + 2, n.getBranchColor(), false);
        String tier = "T" + n.getTier().getTier();
        int tw = font.width(tier);
        g.drawString(font, AbyssDraw.trimmed(font, n.getDisplayName(), w - 18 - tw - 4), x + 14, y + 2, AbyssTheme.TEXT, false);
        g.drawString(font, tier, x + w - tw - 3, y + 2, n.getTierColor(), false);
    }
}
