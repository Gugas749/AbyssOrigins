package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class VampireHungerPassive extends OriginPassive {
    private int warnTimer = 0;
    public VampireHungerPassive() {
        super("vampire_hunger",
            "abyssorigins.passive.vampire_hunger.name",
            "abyssorigins.passive.vampire_hunger.desc");
    }
    @Override public void onEquip(Player player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        if (data.getResource() <= 10f) {
            if (++warnTimer >= 100) {
                warnTimer = 0;
                player.displayClientMessage(
                    Component.translatable("abyssorigins.message.blood_critical"), true);
            }
        } else { warnTimer = 0; }
    }
    @Override public void onRemove(Player player) { warnTimer = 0; }
}
