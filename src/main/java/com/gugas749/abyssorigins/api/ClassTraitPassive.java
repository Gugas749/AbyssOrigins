package com.gugas749.abyssorigins.api;

import net.minecraft.world.entity.player.Player;

/** Class equivalent of TraitPassive — listed in the UI, mechanics live in the class's hooks. */
public class ClassTraitPassive extends SpecializationPassive {

    public ClassTraitPassive(String id, String nameKey, String descKey) {
        super(id, nameKey, descKey);
    }

    /** Uses abyssorigins.passive.&lt;id&gt;.name / .desc */
    public static ClassTraitPassive of(String id) {
        return new ClassTraitPassive(id, "abyssorigins.passive." + id + ".name", "abyssorigins.passive." + id + ".desc");
    }

    @Override public void onTick(Player player) {}
}
