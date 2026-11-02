package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Second Wind: heal 30% max HP, purge every harmful effect, Absorption I for 10s. */
public class HumanSecondWindAbility extends OriginAbility {

    public HumanSecondWindAbility() {
        super("human_second_wind",
            "abyssorigins.ability.human_second_wind.name",
            "abyssorigins.ability.human_second_wind.desc",
            90, 0);
        icon("❤"); color(0xFFF2D47E);
    }

    @Override
    public int cooldownTicks(ServerPlayer player) {
        return Skills.has(player, "human_paragon") ? getBaseCooldownTicks() / 2 : getBaseCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        player.heal(player.getMaxHealth() * 0.30f);

        List<Holder<MobEffect>> harmful = player.getActiveEffects().stream()
            .map(MobEffectInstance::getEffect)
            .filter(e -> e.value().getCategory() == MobEffectCategory.HARMFUL)
            .toList();
        harmful.forEach(player::removeEffect);

        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true));
        if (Skills.has(player, "human_iron_will"))
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, false, true));

        var level = player.serverLevel();
        Vfx.helix(level, player, 2.4f, Palette.HUMAN_HI, Palette.HUMAN, 30);
        Vfx.of(VfxType.DOME).follow(player).scale(1.4f).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(22).send(level);
        Vfx.burst(level, Fx.chest(player), 0.8f, Palette.HUMAN, Palette.HUMAN_HI);
        Fx.sound(level, player.position(), SoundEvents.PLAYER_BREATH, 1.0f, 0.8f);
        Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        return true;
    }
}
