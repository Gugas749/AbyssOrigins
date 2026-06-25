package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class VampireHungerPassive extends OriginPassive {

    private int warnTimer = 0;

    public VampireHungerPassive() {
        super("vampire_hunger", "Blood Dependency",
            "You do not eat food. Blood is your sustenance.");
    }

    @Override
    public void onEquip(Player player) {
        // Lock hunger bar at full — blood bar replaces it visually via HUD
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;

        // Keep vanilla hunger locked so starvation never fires
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);

        // Warn player when blood is critically low
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        if (data.getResource() <= 10f) {
            if (++warnTimer >= 100) {
                warnTimer = 0;
                player.displayClientMessage(
                    Component.literal("§4⚠ Your blood is critically low! Find sustenance."), true);
            }
        } else {
            warnTimer = 0;
        }
    }

    @Override
    public void onRemove(Player player) {
        warnTimer = 0;
    }
}
