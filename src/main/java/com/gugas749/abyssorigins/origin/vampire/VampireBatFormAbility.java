package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class VampireBatFormAbility extends OriginAbility {

    private static final int DURATION_TICKS = 200; // 10 seconds

    public VampireBatFormAbility() {
        super("vampire_bat_form", "Bat Form",
            "Transform briefly to gain flight for 10 seconds. Costs 15 blood.", 60, 15);
    }

    @Override
    public void activate(Player player, Level level) {
        // Grant creative flight for 10 seconds
        // We use a simple approach: enable abilities.mayfly and schedule removal
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        // Schedule removal via a simple tag-based timer tracked in the passive tick
        // We store the expiry tick count on the player's persistent data NBT
        // For now we schedule via a simple delayed task approach
        player.getPersistentData().putInt("abyssorigins_bat_form_ticks", DURATION_TICKS);

        player.displayClientMessage(
            Component.literal("§5🦇 Bat Form activated! " + (DURATION_TICKS / 20) + "s remaining."), true);
    }

    @Override
    public boolean canActivate(Player player) {
        // Can't activate if already in bat form
        return !player.getPersistentData().contains("abyssorigins_bat_form_ticks");
    }
}
