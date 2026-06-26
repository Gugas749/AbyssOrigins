package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class HumanResiliencePassive extends OriginPassive {
    private int timer = 0;
    public HumanResiliencePassive() {
        super("human_resilience",
            "abyssorigins.passive.human_resilience.name",
            "abyssorigins.passive.human_resilience.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide())
            return;

        if (++timer < 10)
            return;

        timer = 0;

        if (player.getHealth() <= 8.0f)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false));
    }
    @Override public void onRemove(Player player) { player.removeEffect(MobEffects.DAMAGE_RESISTANCE); timer = 0; }
}
