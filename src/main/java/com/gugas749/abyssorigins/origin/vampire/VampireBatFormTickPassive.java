package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class VampireBatFormTickPassive extends OriginPassive {

    private static final String KEY = "abyssorigins_bat_form_ticks";

    public VampireBatFormTickPassive() {
        super("vampire_bat_form_tick", "Bat Form Timer", "Internal bat form countdown.");
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        if (!player.getPersistentData().contains(KEY)) return;

        int remaining = player.getPersistentData().getInt(KEY) - 1;

        if (remaining <= 0) {
            player.getPersistentData().remove(KEY);
            // Revoke flight (unless in creative/spectator)
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.displayClientMessage(
                    Component.literal("§5🦇 Bat Form ended."), true);
            }
        } else {
            player.getPersistentData().putInt(KEY, remaining);
        }
    }

    @Override
    public void onRemove(Player player) {
        player.getPersistentData().remove(KEY);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}
