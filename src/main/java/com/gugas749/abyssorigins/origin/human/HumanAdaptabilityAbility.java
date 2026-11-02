package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Adaptability: Haste II + Strength I for 8s. Fighter adds Speed; Rallying Cry shares it with allies. */
public class HumanAdaptabilityAbility extends OriginAbility {
    private static final int DURATION = 160;

    public HumanAdaptabilityAbility() {
        super("human_adaptability",
            "abyssorigins.ability.human_adaptability.name",
            "abyssorigins.ability.human_adaptability.desc",
            45, 0);
        icon("✦"); color(0xFFE8C872);
    }

    @Override
    public boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,    DURATION, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION, 0, false, true));
        if (Skills.has(player, "human_fighter"))
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION, 0, false, true));

        var level = player.serverLevel();
        if (Skills.has(player, "human_rallying_cry")) {
            for (LivingEntity ally : Targeting.alliesAround(player, player.position(), 8)) {
                if (ally == player) continue;
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION, 0, false, true));
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION, 0, false, true));
                Vfx.helix(level, ally, 2.0f, Palette.HUMAN, Palette.HUMAN_HI, 24);
            }
            Vfx.shockwave(level, player.position(), 8, Palette.HUMAN, Palette.HUMAN_HI, 26);
            Fx.sound(level, player.position(), SoundEvents.RAID_HORN, 0.6f, 1.4f);
        }

        Vfx.helix(level, player, 2.2f, Palette.HUMAN, Palette.HUMAN_HI, 26);
        Vfx.shockwave(level, player.position(), 2.6f, Palette.HUMAN, Palette.HUMAN_HI, 16);
        Fx.sound(level, player.position(), SoundEvents.ARMOR_EQUIP_IRON, 1.0f, 0.8f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.adaptability_activated"), true);
        return true;
    }
}
