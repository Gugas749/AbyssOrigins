package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class VampireNightPassive extends OriginPassive {
    public VampireNightPassive() {
        super("vampire_night",
            "abyssorigins.passive.vampire_night.name",
            "abyssorigins.passive.vampire_night.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        if (!player.hasEffect(MobEffects.NIGHT_VISION))
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
        boolean dark = !player.level().isDay()
            || player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
        if (dark) {
            if (!player.hasEffect(MobEffects.MOVEMENT_SPEED))
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        } else { player.removeEffect(MobEffects.MOVEMENT_SPEED); }
    }
    @Override public void onRemove(Player player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
    }
}
