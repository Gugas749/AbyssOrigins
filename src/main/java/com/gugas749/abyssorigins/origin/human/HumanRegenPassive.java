package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class HumanRegenPassive extends OriginPassive {
    private int timer = 0;
    public HumanRegenPassive() {
        super("human_regen",
            "abyssorigins.passive.human_regen.name",
            "abyssorigins.passive.human_regen.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide())
            return;

        if (++timer < 100)
            return;

        timer = 0;

        if (player.getFoodData().getFoodLevel() >= 14)
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
    }
    @Override public void onRemove(Player player) { player.removeEffect(MobEffects.REGENERATION); timer = 0; }
}
