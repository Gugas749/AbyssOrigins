package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.power.Skills;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Survival Instinct: Resistance I at 4 hearts or less (5 with Calloused Hide). */
public class HumanResiliencePassive extends OriginPassive {
    public HumanResiliencePassive() {
        super("human_resilience",
            "abyssorigins.passive.human_resilience.name",
            "abyssorigins.passive.human_resilience.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide() || player.tickCount % 10 != 0) return;

        float threshold = Skills.has(player, "human_tough_skin") ? 10.0f : 8.0f;
        if (player.getHealth() <= threshold)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
    }
}
