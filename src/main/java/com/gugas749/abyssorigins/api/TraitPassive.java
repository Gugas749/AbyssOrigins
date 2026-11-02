package com.gugas749.abyssorigins.api;

import net.minecraft.world.entity.player.Player;

/**
 * A passive whose mechanics live entirely in the origin's PowerHooks or event
 * handlers. Exists so the trait is listed (with its description) in the UI.
 */
public class TraitPassive extends OriginPassive {

    public TraitPassive(String id, String nameKey, String descKey) {
        super(id, nameKey, descKey);
    }

    /** Uses the standard key layout: abyssorigins.passive.&lt;id&gt;.name / .desc */
    public static TraitPassive of(String id) {
        return new TraitPassive(id, "abyssorigins.passive." + id + ".name", "abyssorigins.passive." + id + ".desc");
    }

    @Override public void onTick(Player player) {}
}
