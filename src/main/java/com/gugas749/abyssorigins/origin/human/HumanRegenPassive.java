package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.power.Skills;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Well-Fed Recovery: Regeneration I pulses while hunger is high (lower threshold with Endurance). */
public class HumanRegenPassive extends OriginPassive {
    public HumanRegenPassive() {
        super("human_regen",
            "abyssorigins.passive.human_regen.name",
            "abyssorigins.passive.human_regen.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide() || player.tickCount % 100 != 0) return;

        int threshold = Skills.has(player, "human_endurance") ? 10 : 14;
        if (player.getFoodData().getFoodLevel() >= threshold)
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false, true));
    }
}
