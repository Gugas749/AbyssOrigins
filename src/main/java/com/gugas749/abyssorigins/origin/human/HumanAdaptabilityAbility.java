package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HumanAdaptabilityAbility extends OriginAbility {
    public HumanAdaptabilityAbility() {
        super("human_adaptability",
            "abyssorigins.ability.human_adaptability.name",
            "abyssorigins.ability.human_adaptability.desc",
            45, 0);
    }
    @Override
    public void activate(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,    160, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true));
        player.displayClientMessage(
            Component.translatable("abyssorigins.message.adaptability_activated"), true);
    }
    @Override public boolean canActivate(Player player) { return true; }
}
