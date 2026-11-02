package com.gugas749.abyssorigins.client.gui.skilltree;

import com.gugas749.abyssorigins.api.*;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.gui.AbilitiesScreen;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abyssorigins.network.packet.UnlockSkillPacket;
import com.gugas749.abyssorigins.network.packet.UnlockSpecSkillPacket;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.gugas749.abyssorigins.client.gui.AbyssTheme.*;

/**
 * The Abyss skill tree.
 *
 * Origin and class trees in tabs (plus the Abilities tab); diamond nodes whose glow reflects their state;
 * connections that pulse once a path is walked; an inspector panel with the node's
 * real stat bonuses, requirements and exclusions. Click to inspect, click again (or
 * press the inspector button) to unlock. Unlocks are predicted client-side and
 * reconciled by the server's next sync.
 */
public class SkillTreeScreen extends Screen {

    private static final int   HEADER_H = 42, FOOTER_H = 16;
    private static final float SPACING_X = 62, SPACING_Y = 58, NODE_R = 13;
    private static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI" };

    private enum State { UNLOCKED, AVAILABLE, NEED_POINTS, LOCKED, EXCLUDED }

    private record TreeView(SkillTree tree, boolean isClass, int accent, String sigil, Component owner) {}

    private final Screen parent;
    private final List<TreeView> views = new ArrayList<>();
    private int tab = 0;

    private float zoom = 1f, zoomTarget = 1f, panX, panY, anchorX, anchorY;
    private boolean dragging;
    private double dragDistance;
    private SkillNode hovered, selected;
    private long openedAt, tabSwitchedAt;
    private int panelW;

    /** nodeId → time of client-side predicted unlock (reconciled against server sync). */
    private final Map<String, Long> predicted = new HashMap<>();
    private final Map<String, Long> unlockFx  = new HashMap<>();

    // Click targets computed during render
    private int[] unlockButton = null;
    private final List<int[]> tabRects = new ArrayList<>();
    private int[] abilitiesTab = null;
    private int[] closeRect = new int[4];
    private boolean startOnClass;

    public SkillTreeScreen(Screen parent, boolean classTab) {
        super(Component.translatable("gui.abyssorigins.skill_tree"));
        this.parent = parent;
        this.startOnClass = classTab;
    }
    public SkillTreeScreen(Screen parent) { this(parent, false); }
    public SkillTreeScreen() { this(null); }

    // ---- Setup -----------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        if (minecraft == null || minecraft.player == null) { onClose(); return; }

        views.clear();
        Origin origin = ClientOriginData.getOrigin();
        if (origin == null) origin = AbyssOriginsAPI.getPlayerOrigin(minecraft.player);
        if (origin != null) {
            final Origin o = origin;
            SkillTreeRegistry.getTree(o.getId()).ifPresent(t ->
                views.add(new TreeView(t, false, o.getThemeColor(), o.getSigil(), o.getDisplayName())));
        }
        Specialization spec = ClientSpecializationData.getSpec();
        if (spec != null && spec.getSkillTree() != null)
            views.add(new TreeView(spec.getSkillTree(), true, spec.getThemeColor(), spec.getSigil(), spec.getDisplayName()));

        if (views.isEmpty()) {   // e.g. the Eminence without a class: its powers are its "tree"
            minecraft.setScreen(new AbilitiesScreen(parent, SlotGroup.ORIGIN));
            return;
        }
        if (startOnClass) {
            for (int i = 0; i < views.size(); i++) if (views.get(i).isClass()) tab = i;
            startOnClass = false;
        }
        tab = Math.min(tab, views.size() - 1);
        panelW = width < 500 ? 136 : 172;
        openedAt = tabSwitchedAt = System.currentTimeMillis();
        recenter();
    }

    private TreeView view() { return views.get(tab); }

    private int canvasRight()  { return width - panelW; }
    private boolean inCanvas(double mx, double my) { return mx < canvasRight() && my > HEADER_H && my < height - FOOTER_H; }

    private void recenter() {
        SkillTree tree = view().tree();
        int minC = tree.getMinCol(), maxC = tree.getMaxCol(), maxR = tree.getMaxRow();
        float availW = canvasRight() - 70, availH = height - HEADER_H - FOOTER_H - 50;
        float needW = Math.max(1, maxC - minC) * SPACING_X, needH = Math.max(1, maxR) * SPACING_Y;
        zoom = zoomTarget = Mth.clamp(Math.min(availW / needW, availH / needH), 0.55f, 1.35f);
        panX = (canvasRight() + 24) / 2f - ((minC + maxC) / 2f) * SPACING_X * zoom;
        panY = (HEADER_H + height - FOOTER_H) / 2f - (maxR / 2f) * SPACING_Y * zoom;
    }

    // ---- Player state (with prediction) ------------------------------------------

    private Set<String> unlocked() {
        Set<String> base = new HashSet<>(view().isClass() ? ClientSpecializationData.getUnlockedSkills() : ClientOriginData.getUnlockedSkills());
        long now = System.currentTimeMillis();
        predicted.entrySet().removeIf(e -> base.contains(e.getKey()) || now - e.getValue() > 2500);
        base.addAll(predicted.keySet());
        return base;
    }

    private int level() {
        return view().isClass() ? Math.max(1, ClientSpecializationData.getSpecLevel()) : ClientOriginData.getLevel();
    }

    private int points(Set<String> unlocked) {
        int pts = view().isClass() ? ClientSpecializationData.getSkillPoints() : ClientOriginData.getSkillPoints();
        Set<String> confirmed = view().isClass() ? ClientSpecializationData.getUnlockedSkills() : ClientOriginData.getUnlockedSkills();
        for (String id : predicted.keySet()) {
            SkillNode n = view().tree().getSkill(id);
            if (n != null && !confirmed.contains(id)) pts -= n.getPointCost();
        }
        return Math.max(0, pts);
    }

    private State state(SkillNode n, Set<String> unlocked, Set<String> excluded, int level, int points) {
        if (unlocked.contains(n.getId())) return State.UNLOCKED;
        if (excluded.contains(n.getId())) return State.EXCLUDED;
        if (!n.prerequisitesMet(unlocked)) return State.LOCKED;
        if (level < n.getTier().getLevelRequired() || points < n.getPointCost()) return State.NEED_POINTS;
        return State.AVAILABLE;
    }

    private float nodeX(SkillNode n) { return panX + n.getGridX() * SPACING_X * zoom; }
    private float nodeY(SkillNode n) { return panY + n.getGridY() * SPACING_Y * zoom; }
    private float radius(SkillNode n) { return NODE_R * zoom * (n.isCapstone() ? 1.3f : 1f); }

    // ---- Render ------------------------------------------------------------------

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (views.isEmpty()) return;
        long now = System.currentTimeMillis();
        float fade = easeOut((now - openedAt) / 380f);
        TreeView v = view();

        // Smooth, mouse-anchored zoom
        if (Math.abs(zoomTarget - zoom) > 0.0005f) {
            float nz = Mth.lerp(0.25f, zoom, zoomTarget);
            panX = anchorX - (anchorX - panX) * (nz / zoom);
            panY = anchorY - (anchorY - panY) * (nz / zoom);
            zoom = nz;
        }

        backdrop(g, width, height, v.accent(), fade);
        softEllipse(g, canvasRight() / 2f, (HEADER_H + height) / 2f, canvasRight() * 0.42f, height * 0.36f, v.accent(), 0.8f * fade);

        Set<String> unlocked = unlocked();
        Set<String> excluded = v.tree().excludedBy(unlocked);
        excluded.removeAll(unlocked);
        int level = level(), points = points(unlocked);

        g.enableScissor(0, HEADER_H, canvasRight(), height - FOOTER_H);
        drawTierGuides(g, fade);
        drawConnections(g, unlocked, excluded, level, points, fade);
        hovered = null;
        float intro = (now - tabSwitchedAt);
        for (SkillNode n : v.tree().getNodes())
            drawNode(g, n, state(n, unlocked, excluded, level, points), mx, my, easeOut((intro - n.getGridY() * 70f) / 320f) * fade);
        drawUnlockFx(g, now);
        if (hovered != null) drawNodeLabel(g, hovered);
        g.disableScissor();

        drawHeader(g, mx, my, level, points, fade);
        drawInspector(g, mx, my, unlocked, excluded, level, points, fade);
        centered(g, font, Component.translatable("gui.abyssorigins.skill_tree.hint"), canvasRight() / 2, height - 12, alpha(TEXT_FAINT, fade));

        super.render(g, mx, my, pt);
    }

    private void drawTierGuides(GuiGraphics g, float fade) {
        SkillTree tree = view().tree();
        Map<Integer, SkillNode.SkillTier> rowTier = new TreeMap<>();
        for (SkillNode n : tree.getNodes()) rowTier.putIfAbsent(n.getGridY(), n.getTier());
        for (Map.Entry<Integer, SkillNode.SkillTier> e : rowTier.entrySet()) {
            int y = (int) (panY + e.getKey() * SPACING_Y * zoom);
            if (y < HEADER_H || y > height - FOOTER_H) continue;
            gradientH(g, 34, y, canvasRight() - 12, y + 1, alpha(EDGE, 0f), alpha(EDGE, 0.55f * fade));
            SkillNode.SkillTier t = e.getValue();
            g.drawString(font, ROMAN[t.getTier() - 1], 8, y - 8, alpha(t.getTier() >= SkillNode.SkillTier.TIER_5.getTier() ? GOLD : TEXT_FAINT, fade), false);
            g.drawString(font, "Lv" + t.getLevelRequired(), 8, y + 2, alpha(TEXT_FAINT, 0.7f * fade), false);
        }
    }

    private void drawConnections(GuiGraphics g, Set<String> unlocked, Set<String> excluded, int level, int points, float fade) {
        SkillTree tree = view().tree();
        float t = time();
        for (SkillTree.SkillConnection c : tree.getConnections()) {
            SkillNode a = tree.getSkill(c.getFromSkillId()), b = tree.getSkill(c.getToSkillId());
            if (a == null || b == null) continue;
            float ax = nodeX(a), ay = nodeY(a), bx = nodeX(b), by = nodeY(b);
            float dx = bx - ax, dy = by - ay, len = Mth.sqrt(dx * dx + dy * dy);
            if (len < 1) continue;
            float ux = dx / len, uy = dy / len;
            float ra = radius(a) * 0.8f, rb = radius(b) * 0.8f;
            float x1 = ax + ux * ra, y1 = ay + uy * ra, x2 = bx - ux * rb, y2 = by - uy * rb;

            boolean fromOn = unlocked.contains(a.getId());
            State sb = state(b, unlocked, excluded, level, points);
            int color; float thick = Mth.clamp(1.8f * zoom, 1.2f, 3f);
            if (fromOn && sb == State.UNLOCKED)                         color = view().accent();
            else if (fromOn && (sb == State.AVAILABLE || sb == State.NEED_POINTS)) color = alpha(GOLD, 0.6f);
            else if (sb == State.EXCLUDED)                               color = alpha(BAD, 0.25f);
            else                                                         color = alpha(EDGE, 0.9f);
            color = alpha(color, fade);

            if (c.isOptional()) dashedLine(g, x1, y1, x2, y2, thick, 4 * zoom, color);
            else                line(g, x1, y1, x2, y2, thick, color);

            if (fromOn && sb == State.UNLOCKED) {
                float p = (t * 0.55f + (a.getId().hashCode() & 0xFF) / 255f) % 1f;
                diamond(g, Mth.lerp(p, x1, x2), Mth.lerp(p, y1, y2), 2.2f * zoom, alpha(lighten(view().accent(), 0.5f), fade));
            }
        }
    }

    private void drawNode(GuiGraphics g, SkillNode n, State s, int mx, int my, float appear) {
        if (appear <= 0.01f) return;
        float cx = nodeX(n), cy = nodeY(n);
        float r = radius(n) * (0.6f + 0.4f * appear);
        if (cx + r < 0 || cx - r > canvasRight() || cy + r < HEADER_H || cy - r > height) return;

        boolean hover = inCanvas(mx, my) && Math.abs(mx - cx) + Math.abs(my - cy) <= r;
        if (hover) { hovered = n; r *= 1.1f; }

        int accent = view().accent(), branch = n.getBranchColor();
        int ring, fill, icon;
        switch (s) {
            case UNLOCKED -> {
                glow(g, cx, cy, r * 2.0f, accent, (0.45f + 0.3f * pulse(2.6f)) * appear);
                ring = lighten(accent, 0.3f); fill = darken(accent, 0.42f); icon = TEXT;
            }
            case AVAILABLE -> {
                glow(g, cx, cy, r * 1.9f, GOLD, (0.25f + 0.35f * pulse(1.4f)) * appear);
                ring = GOLD; fill = 0xFF17112B; icon = branch;
            }
            case NEED_POINTS -> { ring = mix(GOLD, EDGE, 0.55f); fill = 0xFF120E22; icon = mix(branch, TEXT_DIM, 0.5f); }
            case EXCLUDED    -> { ring = 0xFF5A1A26; fill = 0xFF190A10; icon = 0xFF5A2A33; }
            default          -> { ring = EDGE; fill = 0xFF0B0915; icon = TEXT_FAINT; }
        }

        if (n.isCapstone())
            squareOutline(g, cx, cy, r + 5 * zoom, time() * 18f, 1f, alpha(n.getTierColor(), 0.55f * appear));
        if (n == selected)
            squareOutline(g, cx, cy, r + 3.5f * zoom, -time() * 40f, 1.2f, alpha(0xFFFFFFFF, 0.8f * appear));

        diamondRing(g, cx, cy, r, Math.max(1.2f, 1.6f * zoom), alpha(ring, appear), alpha(fill, appear));
        diamond(g, cx, cy - r + 3.2f * zoom, 1.6f * zoom, alpha(branch, (s == State.LOCKED ? 0.35f : 0.9f) * appear));

        if (zoom >= 0.5f) {
            float scale = Mth.clamp(zoom, 0.6f, 1.6f) * (n.isCapstone() ? 1.15f : 1f);
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, n.getIcon(), -font.width(n.getIcon()) / 2, -4, alpha(icon, appear), s == State.UNLOCKED);
            g.pose().popPose();
        }
        if (s == State.EXCLUDED) {
            line(g, cx - r * 0.45f, cy - r * 0.45f, cx + r * 0.45f, cy + r * 0.45f, 1.2f, alpha(BAD, 0.6f * appear));
            line(g, cx - r * 0.45f, cy + r * 0.45f, cx + r * 0.45f, cy - r * 0.45f, 1.2f, alpha(BAD, 0.6f * appear));
        }
        if (s != State.UNLOCKED && s != State.EXCLUDED && zoom >= 0.7f) {
            String cost = String.valueOf(n.getPointCost());
            g.drawString(font, cost, (int) cx - font.width(cost) / 2, (int) (cy + r + 2), alpha(s == State.AVAILABLE ? GOLD : TEXT_FAINT, appear), true);
        }
    }

    /** Square outline rotated by {@code deg}; at 45° offsets it reads as a diamond frame. */
    private static void squareOutline(GuiGraphics g, float cx, float cy, float r, float deg, float thick, int color) {
        float[] xs = new float[4], ys = new float[4];
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(deg + i * 90 + 90);
            xs[i] = cx + (float) Math.cos(a) * r;
            ys[i] = cy + (float) Math.sin(a) * r;
        }
        for (int i = 0; i < 4; i++) line(g, xs[i], ys[i], xs[(i + 1) % 4], ys[(i + 1) % 4], thick, color);
    }

    private void drawNodeLabel(GuiGraphics g, SkillNode n) {
        Component name = n.getDisplayName();
        int w = font.width(name) + 8;
        int x = (int) nodeX(n) - w / 2, y = (int) (nodeY(n) - radius(n) - 16);
        g.fill(x, y, x + w, y + 12, 0xE0090612);
        border(g, x, y, w, 12, alpha(view().accent(), 0.7f));
        g.drawString(font, name, x + 4, y + 2, TEXT, false);
    }

    private void drawUnlockFx(GuiGraphics g, long now) {
        Iterator<Map.Entry<String, Long>> it = unlockFx.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            float t = (now - e.getValue()) / 700f;
            SkillNode n = view().tree().getSkill(e.getKey());
            if (t >= 1f || n == null) { it.remove(); continue; }
            float cx = nodeX(n), cy = nodeY(n), r = radius(n);
            float k = easeOut(t);
            squareOutline(g, cx, cy, r * (1 + 2.2f * k), 0, 2f * (1 - t), alpha(lighten(view().accent(), 0.4f), 1 - t));
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + t;
                float d1 = r * (1 + 1.2f * k), d2 = r * (1 + 2.4f * k);
                line(g, cx + (float) Math.cos(a) * d1, cy + (float) Math.sin(a) * d1,
                        cx + (float) Math.cos(a) * d2, cy + (float) Math.sin(a) * d2, 1f, alpha(GOLD, 1 - t));
            }
        }
    }

    // ---- Header ------------------------------------------------------------------

    private void drawHeader(GuiGraphics g, int mx, int my, int level, int points, float fade) {
        TreeView v = view();
        g.fillGradient(0, 0, width, HEADER_H, alpha(0xF0080612, fade), alpha(0xC00A0716, fade));
        gradientH(g, 0, HEADER_H - 1, width / 2, HEADER_H, alpha(v.accent() & 0xFFFFFF, fade), alpha(v.accent(), fade));
        gradientH(g, width / 2, HEADER_H - 1, width, HEADER_H, alpha(v.accent(), fade), alpha(v.accent() & 0xFFFFFF, fade));

        glow(g, 20, 21, 18, v.accent(), 0.5f * fade);
        diamondRing(g, 20, 21, 11, 1.4f, alpha(v.accent(), fade), alpha(0xFF0B0816, fade));
        centered(g, font, v.sigil(), 20, 17, alpha(v.accent(), fade));
        scaledText(g, font, v.tree().getDisplayName(), 38, 7, 1.25f, alpha(lighten(v.accent(), 0.35f), fade), true);

        // Tabs under the title (they name the path, so no subtitle): the trees, then Abilities
        tabRects.clear();
        int tx = 38, ty = 23, h = 14;
        for (int i = 0; i < views.size(); i++) {
            TreeView tv = views.get(i);
            Component label = Component.translatable(tv.isClass() ? "gui.abyssorigins.tab.class" : "gui.abyssorigins.tab.origin");
            int w = font.width(label) + 20;
            boolean active = i == tab, hover = inside(mx, my, tx, ty, w, h);
            g.fill(tx, ty, tx + w, ty + h, alpha(active ? withAlpha(tv.accent(), 60) : hover ? 0x40FFFFFF : 0x30000000, fade));
            if (active) g.fill(tx, ty + h - 1, tx + w, ty + h, alpha(tv.accent(), fade));
            border(g, tx, ty, w, h, alpha(active ? tv.accent() : EDGE, fade));
            diamond(g, tx + 7, ty + 7, 2.5f, alpha(tv.accent(), fade));
            g.drawString(font, label, tx + 13, ty + 3, alpha(active ? TEXT : TEXT_DIM, fade), false);
            tabRects.add(new int[]{ tx, ty, w, h });
            tx += w + 4;
        }
        Component label = Component.translatable("gui.abyssorigins.tab.abilities");
        int w = font.width(label) + 20;
        boolean hover = inside(mx, my, tx, ty, w, h);
        g.fill(tx, ty, tx + w, ty + h, alpha(hover ? 0x40FFFFFF : 0x30000000, fade));
        border(g, tx, ty, w, h, alpha(hover ? lighten(EDGE, 0.4f) : EDGE, fade));
        g.drawString(font, "✦", tx + 4, ty + 3, alpha(MIST, fade), false);
        g.drawString(font, label, tx + 13, ty + 3, alpha(hover ? TEXT : TEXT_DIM, fade), false);
        abilitiesTab = new int[]{ tx, ty, w, h };

        // Level + points
        Component lvl = Component.translatable("gui.abyssorigins.level", level);
        Component pts = Component.translatable("gui.abyssorigins.points", points);
        int right = width - 26;
        g.drawString(font, lvl, right - font.width(lvl), 9, alpha(GOLD, fade), true);
        int pc = points > 0 ? mix(GOLD, 0xFFFFF4C8, pulse(1.4f)) : TEXT_DIM;
        g.drawString(font, pts, right - font.width(pts), 23, alpha(pc, fade), true);

        closeRect = new int[]{ width - 18, 6, 12, 12 };
        boolean ch = inside(mx, my, closeRect[0], closeRect[1], 12, 12);
        g.drawString(font, "✕", width - 15, 8, alpha(ch ? TEXT : TEXT_FAINT, fade), false);
    }

    // ---- Inspector ---------------------------------------------------------------

    private void drawInspector(GuiGraphics g, int mx, int my, Set<String> unlocked, Set<String> excluded,
                               int level, int points, float fade) {
        int x = width - panelW + 4, y = HEADER_H + 6, w = panelW - 10, h = height - HEADER_H - FOOTER_H - 10;
        panel(g, x, y, w, h, view().accent(), fade);
        unlockButton = null;
        SkillNode n = selected != null ? selected : hovered;
        int ix = x + 8, iw = w - 16, iy = y + 8;

        if (n == null) { drawOverview(g, ix, iy, iw, unlocked, points, fade); return; }

        State s = state(n, unlocked, excluded, level, points);
        int accent = view().accent();

        // Title block
        diamondRing(g, ix + 10, iy + 10, 10, 1.3f, alpha(s == State.UNLOCKED ? accent : s == State.AVAILABLE ? GOLD : EDGE, fade), alpha(0xFF0B0816, fade));
        centered(g, font, n.getIcon(), ix + 10, iy + 6, alpha(n.getBranchColor(), fade));
        int ty = wrapped(g, font, n.getDisplayName().copy().withStyle(ChatFormatting.BOLD), ix + 24, iy + 2, iw - 24, alpha(TEXT, fade), 2);
        iy = Math.max(iy + 24, ty + 2);

        Component tier = Component.literal(ROMAN[n.getTier().getTier() - 1]).withColor(n.getTierColor() & 0xFFFFFF)
            .append(Component.literal("  ·  ").withColor(TEXT_FAINT & 0xFFFFFF))
            .append(Component.translatable("abyssorigins.branch." + n.getBranch().name().toLowerCase(Locale.ROOT)).withColor(n.getBranchColor() & 0xFFFFFF));
        g.drawString(font, tier, ix, iy, alpha(0xFFFFFFFF, fade), false);
        iy += 12;
        gradientH(g, ix, iy, ix + iw, iy + 1, alpha(accent, 0.6f * fade), alpha(accent & 0xFFFFFF, fade));
        iy += 6;

        iy = wrapped(g, font, n.getDescription(), ix, iy, iw, alpha(TEXT_DIM, fade), 9) + 4;

        for (StatBonus b : n.getStats()) {
            g.drawString(font, statLine(b), ix, iy, alpha(b.amount() >= 0 ? GOOD : BAD, fade), false);
            iy += 10;
        }
        if (!n.getStats().isEmpty()) iy += 4;

        Ability taught = n.grantsAbility() ? AbilityRegistry.ability(n.getGrantedAbility()) : null;
        if (taught != null) {
            Component line = Component.literal(taught.getIcon() + " ").withColor(taught.getColor() & 0xFFFFFF)
                .append(Component.translatable("gui.abyssorigins.grants_power", taught.getDisplayName()).withColor(GOLD & 0xFFFFFF));
            iy = wrapped(g, font, line, ix, iy, iw, alpha(0xFFFFFFFF, fade), 2) + 4;
        }

        // Requirements
        iy += header(g, font, ix, iy, iw, Component.translatable("gui.abyssorigins.requirements"), accent, fade);
        iy = requirement(g, ix, iy, Component.translatable("gui.abyssorigins.req_level", n.getTier().getLevelRequired()),
            level >= n.getTier().getLevelRequired(), fade);
        for (String req : n.getPrerequisites()) {
            SkillNode r = view().tree().getSkill(req);
            if (r != null) iy = requirement(g, ix, iy, r.getDisplayName(), unlocked.contains(req), fade);
        }
        if (!n.getAnyPrerequisites().isEmpty()) {
            Component any = Component.translatable("gui.abyssorigins.req_any");
            for (int i = 0; i < n.getAnyPrerequisites().size(); i++) {
                SkillNode r = view().tree().getSkill(n.getAnyPrerequisites().get(i));
                if (r == null) continue;
                any = any.copy().append(i == 0 ? " " : " / ").append(r.getDisplayName());
            }
            iy = requirement(g, ix, iy, any, n.getAnyPrerequisites().stream().anyMatch(unlocked::contains), fade);
        }
        if (!n.getExclusions().isEmpty() && s != State.UNLOCKED) {
            iy += 3;
            for (String ex : n.getExclusions()) {
                SkillNode e = view().tree().getSkill(ex);
                if (e == null) continue;
                iy = wrapped(g, font, Component.translatable("gui.abyssorigins.forsakes", e.getDisplayName()), ix, iy, iw, alpha(BAD, fade), 2);
            }
        }

        // Action button pinned to the bottom of the panel
        int by = y + h - 24, bw = iw, bx = ix;
        Component label; int col; boolean enabled = false;
        switch (s) {
            case UNLOCKED    -> { label = Component.translatable("gui.abyssorigins.btn.mastered"); col = accent; }
            case AVAILABLE   -> { label = Component.translatable("gui.abyssorigins.btn.unlock", n.getPointCost()); col = GOLD; enabled = true; }
            case NEED_POINTS -> {
                label = level < n.getTier().getLevelRequired()
                    ? Component.translatable("gui.abyssorigins.btn.need_level", n.getTier().getLevelRequired())
                    : Component.translatable("gui.abyssorigins.btn.need_points", n.getPointCost());
                col = TEXT_FAINT;
            }
            case EXCLUDED    -> { label = Component.translatable("gui.abyssorigins.btn.forsaken"); col = BAD; }
            default          -> { label = Component.translatable("gui.abyssorigins.btn.locked"); col = TEXT_FAINT; }
        }
        boolean bh = enabled && inside(mx, my, bx, by, bw, 18);
        g.fill(bx, by, bx + bw, by + 18, alpha(enabled ? (bh ? withAlpha(GOLD, 70) : withAlpha(GOLD, 28)) : 0x40000000, fade));
        border(g, bx, by, bw, 18, alpha(enabled ? mix(GOLD, 0xFFFFFFFF, bh ? 0.4f : 0.25f * pulse(1.4f)) : col, fade));
        centered(g, font, label, bx + bw / 2, by + 5, alpha(enabled ? (bh ? 0xFFFFFFFF : GOLD) : col, fade));
        if (enabled) unlockButton = new int[]{ bx, by, bw, 18 };
    }

    private int requirement(GuiGraphics g, int x, int y, Component text, boolean met, float fade) {
        g.drawString(font, met ? "✓" : "✗", x, y, alpha(met ? GOOD : BAD, fade), false);
        return wrapped(g, font, text, x + 10, y, panelW - 34, alpha(met ? TEXT_DIM : TEXT, fade), 2);
    }

    private void drawOverview(GuiGraphics g, int x, int y, int w, Set<String> unlocked, int points, float fade) {
        SkillTree tree = view().tree();
        y += header(g, font, x, y, w, Component.translatable("gui.abyssorigins.overview"), view().accent(), fade) + 2;
        long mine = tree.getNodes().stream().filter(n -> unlocked.contains(n.getId())).count();
        g.drawString(font, Component.translatable("gui.abyssorigins.mastery", mine, tree.getNodes().size()), x, y, alpha(TEXT, fade), false);
        y += 11;
        bar(g, x, y, w, 3, mine / (float) Math.max(1, tree.getNodes().size()), view().accent(), true);
        y += 10;

        y += header(g, font, x, y, w, Component.translatable("gui.abyssorigins.legend"), view().accent(), fade) + 2;
        y = legend(g, x, y, view().accent(), "unlocked", fade);
        y = legend(g, x, y, GOLD, "available", fade);
        y = legend(g, x, y, mix(GOLD, EDGE, 0.55f), "need", fade);
        y = legend(g, x, y, EDGE, "locked", fade);
        y = legend(g, x, y, 0xFF5A1A26, "forsaken", fade);
        y += 4;

        y += header(g, font, x, y, w, Component.translatable("gui.abyssorigins.schools"), view().accent(), fade) + 2;
        Set<SkillNode.SkillBranch> seen = new LinkedHashSet<>();
        tree.getNodes().forEach(n -> seen.add(n.getBranch()));
        for (SkillNode.SkillBranch b : seen) {
            if (y > height - FOOTER_H - 46) break;
            int color = SkillNode.colorOf(b);
            diamond(g, x + 3, y + 4, 2.5f, alpha(color, fade));
            g.drawString(font, Component.translatable("abyssorigins.branch." + b.name().toLowerCase(Locale.ROOT)), x + 10, y, alpha(TEXT_DIM, fade), false);
            y += 10;
        }
        if (points > 0)
            wrapped(g, font, Component.translatable("gui.abyssorigins.points_hint", points), x, height - FOOTER_H - 34, w, alpha(GOLD, fade), 2);
    }

    private int legend(GuiGraphics g, int x, int y, int color, String key, float fade) {
        diamondRing(g, x + 4, y + 4, 4, 1f, alpha(color, fade), alpha(0xFF0B0915, fade));
        g.drawString(font, Component.translatable("gui.abyssorigins.state." + key), x + 12, y, alpha(TEXT_DIM, fade), false);
        return y + 11;
    }

    private static Component statLine(StatBonus b) {
        double v = b.amount();
        boolean pct = b.operation() != AttributeModifier.Operation.ADD_VALUE;
        String num = pct ? String.format(Locale.ROOT, "%+.0f%%", v * 100)
                   : v == Math.rint(v) ? String.format(Locale.ROOT, "%+.0f", v) : String.format(Locale.ROOT, "%+.1f", v);
        return Component.literal(num + " ").append(Component.translatable(b.attribute().value().getDescriptionId()));
    }

    // ---- Input -------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || views.isEmpty()) return super.mouseClicked(mx, my, button);

        if (inside(mx, my, closeRect[0], closeRect[1], closeRect[2], closeRect[3])) { onClose(); return true; }
        for (int i = 0; i < tabRects.size(); i++) {
            int[] r = tabRects.get(i);
            if (inside(mx, my, r[0], r[1], r[2], r[3])) { switchTab(i); return true; }
        }
        if (abilitiesTab != null && inside(mx, my, abilitiesTab[0], abilitiesTab[1], abilitiesTab[2], abilitiesTab[3])) {
            uiSound(1.1f);
            if (minecraft != null) minecraft.setScreen(new AbilitiesScreen(parent, view().isClass() ? SlotGroup.CLASS : SlotGroup.ORIGIN));
            return true;
        }
        if (unlockButton != null && selected != null && inside(mx, my, unlockButton[0], unlockButton[1], unlockButton[2], unlockButton[3])) {
            unlock(selected);
            return true;
        }
        if (inCanvas(mx, my)) {
            if (hovered != null) {
                Set<String> un = unlocked();
                Set<String> ex = view().tree().excludedBy(un);
                ex.removeAll(un);
                if (selected == hovered && state(hovered, un, ex, level(), points(un)) == State.AVAILABLE) unlock(hovered);
                else { selected = hovered; uiSound(1.3f); }
                return true;
            }
            dragging = true;
            dragDistance = 0;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            panX += (float) dx;
            panY += (float) dy;
            dragDistance += Math.abs(dx) + Math.abs(dy);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            if (dragDistance < 3) selected = null;   // a plain click on empty space clears the selection
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (!inCanvas(mx, my) || sy == 0) return false;
        zoomTarget = Mth.clamp(zoomTarget * (sy > 0 ? 1.15f : 1 / 1.15f), 0.45f, 2.2f);
        anchorX = (float) mx;
        anchorY = (float) my;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_R) { recenter(); return true; }
        if (key == GLFW.GLFW_KEY_TAB && views.size() > 1) { switchTab((tab + 1) % views.size()); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void switchTab(int i) {
        if (i == tab) return;
        tab = i;
        selected = hovered = null;
        tabSwitchedAt = System.currentTimeMillis();
        recenter();
        uiSound(0.9f);
    }

    private void unlock(SkillNode node) {
        if (view().isClass()) PacketDistributor.sendToServer(new UnlockSpecSkillPacket(node.getId()));
        else                  PacketDistributor.sendToServer(new UnlockSkillPacket(node.getId()));
        long now = System.currentTimeMillis();
        predicted.put(node.getId(), now);
        unlockFx.put(node.getId(), now);
        if (minecraft != null)
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, node.isCapstone() ? 0.6f : 1.1f, 1.0f));
    }

    private void uiSound(float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
