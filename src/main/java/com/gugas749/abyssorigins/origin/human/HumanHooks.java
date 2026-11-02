package com.gugas749.abyssorigins.origin.human;

import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Human skill mechanics. Humans have no supernatural gifts — their tree is about
 * endurance, refusing to die, and mastering whatever class they choose.
 */
public class HumanHooks implements PowerHooks {

    @Override
    public void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;

        // Last Stand: Resistance II below 30% HP
        if (Skills.has(player, "human_last_stand") && player.getHealth() / player.getMaxHealth() < 0.30f)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false, true));
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        // Swift: dropping below half health triggers a burst of Speed II (6s internal cooldown)
        if (Skills.has(player, "human_swift")
                && player.getHealth() - event.getAmount() < player.getMaxHealth() / 2
                && PowerState.tryTrigger(player, "human_swift", 120)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, true));
            Vfx.shockwave(player.serverLevel(), player.position(), 1.6f, Palette.HUMAN, Palette.HUMAN_HI, 10);
        }
    }

    @Override
    public boolean onLethal(ServerPlayer player, DamageSource source) {
        // Unbroken: once every 5 minutes, refuse to die
        if (!Skills.has(player, "human_unbroken") || !PowerState.tryTrigger(player, "human_unbroken", 6000)) return false;

        player.setHealth(1f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
        var level = player.serverLevel();
        Vfx.of(VfxType.PILLAR).at(player.position()).scale(7).extra(0.9f).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(34).send(level);
        Vfx.burst(level, Fx.chest(player), 1.4f, Palette.HUMAN, Palette.HUMAN_HI);
        Fx.sound(level, player.position(), SoundEvents.TOTEM_USE, 0.8f, 1.3f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.human_unbroken"), true);
        return true;
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        if (!(ability instanceof SpecializationAbility)) return 1f;
        float m = 0.90f;                                       // Mortal Ingenuity
        if (Skills.has(player, "human_tactician")) m *= 0.85f;
        return m;
    }
}
