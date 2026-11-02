package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.OriginPassive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Creature of the Night: permanent night vision, +15% speed in darkness.
 *
 * The speed bonus is a transient attribute modifier rather than a Speed effect —
 * the old version removed *every* Speed effect each daylight tick, including potions.
 */
public class VampireNightPassive extends OriginPassive {
    private static final ResourceLocation NIGHT_SPEED =
        ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "vampire_night_speed");

    public VampireNightPassive() {
        super("vampire_night",
            "abyssorigins.passive.vampire_night.name",
            "abyssorigins.passive.vampire_night.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide()) return;

        // Refresh when below 10 seconds remaining — never lets it flicker
        var nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision == null || nightVision.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 500, 0, false, false));
        }

        if (player.tickCount % 10 != 0) return;
        boolean dark = !player.level().isDay()
                || player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (dark && !speed.hasModifier(NIGHT_SPEED))
            speed.addTransientModifier(new AttributeModifier(NIGHT_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        else if (!dark && speed.hasModifier(NIGHT_SPEED))
            speed.removeModifier(NIGHT_SPEED);
    }
    @Override public void onRemove(Player player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(NIGHT_SPEED);
    }
}
