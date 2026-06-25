package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class HumanRegenPassive extends OriginPassive {

    private int timer = 0;

    public HumanRegenPassive() {
        super("human_regen", "Well-Fed Recovery",
            "Slowly regenerate health while hunger is above 7 shanks.");
    }

    @Override
    public void onTick(Player player) {
        if (++timer < 100) return;
        timer = 0;
        if (player.getFoodData().getFoodLevel() >= 14) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
        }
    }

    @Override
    public void onRemove(Player player) { player.removeEffect(MobEffects.REGENERATION); timer = 0; }
}
