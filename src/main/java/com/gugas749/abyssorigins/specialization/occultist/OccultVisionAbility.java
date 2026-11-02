package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Occult Vision (toggle).
 *
 * BUG FIX: the old version applied the vanilla Glowing effect to every player on the
 * server. Glowing is a shared entity flag, so *everyone* saw everyone outlined, and the
 * revealed players could see that they were glowing.
 *
 * Now the server only records that the vision is active (AbilityStates → synced to the
 * caster alone). The outlines are drawn purely client-side by RevealClient + the
 * Minecraft/Entity mixins, so only the Occultist sees them, coloured by origin.
 */
public class OccultVisionAbility extends SpecializationAbility implements Sustained {

    public static final String ID = "occult_vision";

    public OccultVisionAbility() {
        super(ID,
            "abyssorigins.ability.occult_vision.name",
            "abyssorigins.ability.occult_vision.desc",
            30);
        icon("◉"); color(0xFFB35CFF);
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = Skills.has(player, "occ_ascended_occultist") ? AbilityStates.INDEFINITE
                     : Skills.has(player, "occ_veil_sight") ? 900 : 600;

        if (Skills.has(player, "occ_phantom_step"))
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));

        var level = player.serverLevel();
        Vfx.of(VfxType.SIGIL).at(player.position()).scale(3f).extra(5).colors(Palette.OCCULT, Palette.OCCULT_HI).duration(44).send(level);
        Vfx.burst(level, player.getEyePosition().add(player.getLookAngle().scale(1.4)), 0.6f, Palette.OCCULT, Palette.OCCULT_HI);
        Fx.sound(level, player.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 0.7f);
        Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);

        player.displayClientMessage(Component.translatable("abyssorigins.message.occult_vision_activated"), true);
        AbilityStates.start(player, ID, duration, this);
        return true;
    }

    @Override
    public void tickActive(ServerPlayer player, int remaining) {
        // Hex Mark: hostile creatures within your sight are cursed every 5 seconds
        if (Skills.has(player, "occ_hex_mark") && player.tickCount % 100 == 0) {
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(16), e -> e instanceof Enemy && e.isAlive()))
                e.addEffect(new MobEffectInstance(ModEffects.HEXED, 100, 0), player);
        }
    }

    @Override
    public void onEnd(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("abyssorigins.message.occult_vision_ended"), true);
        Fx.sound(player.level(), player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.5f, 0.5f);
    }
}
