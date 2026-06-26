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

        // Refresh when below 8 seconds remaining — never lets it expire
        var nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision == null || nightVision.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 500, 1, false, false));
        }

        // Same pattern for speed
        boolean dark = !player.level().isDay()
                || player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
        if (dark) {
            var speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
            if (speed == null || speed.getDuration() < 60) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 300, 0, false, false));
            }
        } else {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
    }
    @Override public void onRemove(Player player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
    }
}
