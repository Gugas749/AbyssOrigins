package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.api.SpecializationPassive;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class OccultistDarkSensePassive extends SpecializationPassive {

    public OccultistDarkSensePassive() {
        super("occultist_dark_sense",
            "abyssorigins.passive.occultist_dark_sense.name",
            "abyssorigins.passive.occultist_dark_sense.desc");
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;

        // Maintain occult vision timer
        OccultVisionAbility.tick(player, player.level());

        // Permanent night vision (dimmer than vampire — no speed bonus)
        var nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision == null || nightVision.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 500, 0, false, false));
        }
    }

    @Override
    public void onRemove(Player player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        // Force-end vision if active
        if (player.getPersistentData().contains("abyssorigins_occult_vision_active")) {
            player.getPersistentData().remove("abyssorigins_occult_vision_active");
            player.removeEffect(MobEffects.DARKNESS);
        }
    }
}
