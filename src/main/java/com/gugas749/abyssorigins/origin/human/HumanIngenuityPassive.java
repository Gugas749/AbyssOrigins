package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.world.entity.player.Player;

/**
 * Mortal Ingenuity: +25% class XP and -10% class ability cooldowns.
 * Descriptive only — implemented in XpEventHandler and HumanHooks.
 */
public class HumanIngenuityPassive extends OriginPassive {
    public HumanIngenuityPassive() {
        super("human_ingenuity",
            "abyssorigins.passive.human_ingenuity.name",
            "abyssorigins.passive.human_ingenuity.desc");
    }
    @Override public void onTick(Player player) {}
}
