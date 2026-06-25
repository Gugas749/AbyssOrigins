package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.registry.OriginRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * Delegates the per-tick bat form countdown to VampireBatFormAbility.
 * Must hold a reference to the ability instance so it can call tick().
 */
public class VampireBatFormTickPassive extends OriginPassive {

    private final VampireBatFormAbility batForm;

    public VampireBatFormTickPassive(VampireBatFormAbility batForm) {
        super("vampire_bat_form_tick", "Bat Form Timer", "Internal bat form countdown.");
        this.batForm = batForm;
    }

    @Override
    public void onTick(Player player) {
        batForm.tick(player, player.level());
    }

    @Override
    public void onRemove(Player player) {
        // Force-end bat form if origin is stripped mid-flight
        if (player.getPersistentData().contains("abyssorigins_bat_form_ticks")) {
            batForm.tick(player, player.level()); // will trigger endBatForm via remaining <= 0 after clear
            player.getPersistentData().putInt("abyssorigins_bat_form_ticks", 0);
        }
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}
