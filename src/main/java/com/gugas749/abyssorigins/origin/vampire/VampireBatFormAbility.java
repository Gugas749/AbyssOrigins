package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
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

/**
 * Bat Form: the vampire stays in their own body but unfurls great bat wings (VFX) and
 * takes flight. Press again to fold the wings early.
 * Bat Transformation doubles the duration; Nosferatu makes it free.
 *
 * (Earlier versions turned the player invisible behind a decoy bat — that "model swap"
 * was removed in favour of the wings.)
 */
public class VampireBatFormAbility extends OriginAbility implements Sustained {
    private static final int RESOURCE_COST = 25, DURATION = 300, COOLDOWN_SECS = 60;

    public VampireBatFormAbility() {
        super("vampire_bat_form",
            "abyssorigins.ability.vampire_bat_form.name",
            "abyssorigins.ability.vampire_bat_form.desc",
            COOLDOWN_SECS, RESOURCE_COST);
        icon("❧"); color(0xFF7A1A3A);
    }

    @Override
    public int resourceCost(ServerPlayer player) {
        return Skills.has(player, "vamp_nosferatu") ? 0 : RESOURCE_COST;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var level = player.serverLevel();
        int duration = Skills.has(player, "vamp_bat_mastery") ? DURATION * 2 : DURATION;

        player.getAbilities().mayfly = true; player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(0.08f); player.onUpdateAbilities();
        player.setDeltaMovement(player.getDeltaMovement().add(0, 0.5, 0)); player.hurtMarked = true;

        Vfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.3f).colors(Palette.BLOOD_DARK, Palette.BLOOD).flags(Vfx.VOID).duration(14).send(level);
        Vfx.of(VfxType.WINGS).follow(player).scale(1.1f).colors(0x3A0010, Palette.BLOOD_HI).flags(Vfx.VOID).duration(duration).send(level);
        Fx.sound(level, player.position(), SoundEvents.BAT_TAKEOFF, 1.5f, 0.8f);
        Fx.sound(level, player.position(), SoundEvents.PHANTOM_FLAP, 1.0f, 0.6f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.bat_form_activated", duration / 20), true);

        AbilityStates.start(player, getId(), duration, this);
        return true;
    }

    @Override
    public void tickActive(ServerPlayer player, int remaining) {
        if (!player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
        }
        if (player.getAbilities().flying && player.tickCount % 12 == 0)
            Fx.sound(player.level(), player.position(), SoundEvents.PHANTOM_FLAP, 0.35f, 1.3f);
        if (remaining == 60) player.displayClientMessage(Component.translatable("abyssorigins.message.bat_form_ending_soon"), true);
    }

    @Override
    public void onEnd(ServerPlayer player) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false; player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(0.05f);
        player.onUpdateAbilities();
        // Short slow-fall so ending mid-air is not a death sentence
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));

        var level = player.serverLevel();
        Vfx.stop(level, player, VfxType.WINGS);
        Vfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.0f).colors(Palette.BLOOD_DARK, Palette.BLOOD).flags(Vfx.VOID).duration(12).send(level);
        Fx.sound(level, player.position(), SoundEvents.BAT_DEATH, 0.8f, 1.0f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.bat_form_ended"), true);
    }
}
