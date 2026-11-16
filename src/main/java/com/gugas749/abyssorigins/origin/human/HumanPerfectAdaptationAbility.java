package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Perfect Adaptability (skill node "human_perfect_adaptation"): 
 * Resistance II, Strength I and Speed I for 12 seconds. The evolved form of Adaptability.
 */
public class HumanPerfectAdaptationAbility extends OriginAbility {
    public static final String ID = "human_perfect_adaptation";
    private static final int DURATION = 240;

    public HumanPerfectAdaptationAbility() {
        super(ID, "abyssorigins.ability." + ID + ".name", "abyssorigins.ability." + ID + ".desc", 60, 0);
        icon("❂"); color(0xFFFFD86B);
    }

    @Override
    public boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,      DURATION, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,    DURATION, 0, false, true));

        var level = player.serverLevel();
        var c = player.position();
        AbyssVfx.helix(level, player, 2.4f, Palette.HUMAN, Palette.HUMAN_HI, 30);
        AbyssVfx.of(VfxType.SIGIL).at(c).scale(2.6f).extra(6).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(34).send(level);
        AbyssVfx.of(VfxType.DOME).follow(player).scale(1.6f).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(18).send(level);
        AbyssVfx.shockwave(level, c, 4f, Palette.HUMAN, Palette.HUMAN_HI, 18);
        AbyssVfx.of(VfxType.AURA).follow(player).scale(1.4f).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(DURATION).send(level);
        Fx.sound(level, c, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0f, 0.8f);
        Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.2f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.perfect_adaptation"), true);
        return true;
    }
}
