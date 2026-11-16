package com.gugas749.abyssorigins.config;

import com.gugas749.abysscore.api.gui.hud.HudAnchor;
import com.gugas749.abysscore.api.gui.hud.HudBounds;
import com.gugas749.abysscore.api.gui.hud.HudBox;
import com.gugas749.abysscore.api.gui.hud.HudPlacement;
import com.gugas749.abysscore.api.gui.hud.HudPositioning;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The HUD moved to AbyssCore's shared HUD API: layouts players saved in client.toml before
 * (anchor + offsets + scale) must still land on the same pixels.
 */
class HudLayoutCompatTest {

    /** The fractions AbyssOriginsConfig.HudAnchor carried before the move. */
    private static final Map<String, float[]> OLD_FRACTIONS = Map.of(
        "TOP_LEFT", new float[]{0, 0}, "TOP_CENTER", new float[]{0.5f, 0}, "TOP_RIGHT", new float[]{1, 0},
        "CENTER_LEFT", new float[]{0, 0.5f}, "CENTER", new float[]{0.5f, 0.5f}, "CENTER_RIGHT", new float[]{1, 0.5f},
        "BOTTOM_LEFT", new float[]{0, 1}, "BOTTOM_CENTER", new float[]{0.5f, 1}, "BOTTOM_RIGHT", new float[]{1, 1});

    @Test
    void storedAnchorsMapBothWays() {
        assertNull(AbyssOriginsConfig.HudAnchor.AUTO.toCore(), "AUTO = the element's own default spot");
        assertEquals(AbyssOriginsConfig.HudAnchor.AUTO, AbyssOriginsConfig.HudAnchor.fromCore(null));
        for (AbyssOriginsConfig.HudAnchor stored : AbyssOriginsConfig.HudAnchor.values()) {
            assertEquals(stored, AbyssOriginsConfig.HudAnchor.fromCore(stored.toCore()));
        }
        for (HudAnchor core : HudAnchor.values()) {
            assertEquals(core, AbyssOriginsConfig.HudAnchor.fromCore(core).toCore());
        }
    }

    @Test
    void storedLayoutsLandWhereTheOldEditorPutThem() {
        int sw = 427, sh = 240;
        HudBounds plate = HudBounds.ofSize(136, 37);
        for (AbyssOriginsConfig.HudAnchor stored : AbyssOriginsConfig.HudAnchor.values()) {
            if (stored == AbyssOriginsConfig.HudAnchor.AUTO) continue;
            float[] f = OLD_FRACTIONS.get(stored.name());
            HudAnchor core = stored.toCore();
            assertEquals(f[0], core.fx, 1e-6, stored.name());
            assertEquals(f[1], core.fy, 1e-6, stored.name());

            for (int[] offset : new int[][]{{0, 0}, {-30, 12}, {7, -5}}) {
                HudBox box = HudPositioning.ALIGNED.place(new HudPlacement(core, offset[0], offset[1], 1.5f, true), plate, 1.5f, sw, sh);
                // The old HudElement.box(): ceil'd size, round(f * (screen - size)) + offset, clamped on screen
                int w = (int) Math.ceil(136 * 1.5f), h = (int) Math.ceil(37 * 1.5f);
                int x = Math.max(0, Math.min(Math.round(f[0] * (sw - w)) + offset[0], sw - w));
                int y = Math.max(0, Math.min(Math.round(f[1] * (sh - h)) + offset[1], sh - h));
                assertEquals(x, box.x(), stored.name());
                assertEquals(y, box.y(), stored.name());
            }
        }
    }

    @Test
    void anElementMovedInTheEditorIsStoredAndReadBackAtTheSameSpot() {
        int sw = 480, sh = 270;
        HudBounds bar = HudBounds.ofSize(116, 13);
        HudPlacement moved = HudPositioning.ALIGNED.moveTo(HudPlacement.auto(), bar, 1f, 300, 220, sw, sh);
        AbyssOriginsConfig.HudAnchor stored = AbyssOriginsConfig.HudAnchor.fromCore(moved.anchor());

        HudPlacement reloaded = new HudPlacement(stored.toCore(), moved.offsetX(), moved.offsetY(), 1f, true);
        HudBox box = HudPositioning.ALIGNED.place(reloaded, bar, 1f, sw, sh);
        assertEquals(300, box.x());
        assertEquals(220, box.y());
    }
}
