package com.gugas749.abyssorigins.origin.human;

import net.minecraft.world.entity.LivingEntity;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Human skill mechanics. Humans have no supernatural powers; their tree focuses on
 * endurance, death prevention and class mastery.
 */
public class HumanHooks implements PowerHooks {

    @Override
    public void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;

        // Last Stand: Resistance II below 30% HP
        if (Skills.has(player, "human_last_stand") && player.getHealth() / player.getMaxHealth() < 0.30f)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false, true));

        // Steady Breath: out of combat (8s without damage), 1 heart every 4s
        if (Skills.has(player, "human_steady_breath") && player.tickCount % 80 == 0
                && !PowerState.flagged(player, "human_in_combat") && player.getHealth() < player.getMaxHealth())
            player.heal(2f);
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;

        // Keen Edge: critical hits +25%
        boolean crit = player.fallDistance > 0 && !player.onGround() && !player.isInWater() && !player.isPassenger();
        if (crit && Skills.has(player, "human_keen_edge")) event.setAmount(event.getAmount() * 1.25f);

        // Warlord: +10% per extra enemy nearby, up to +30%
        if (Skills.has(player, "human_warlord")) {
            int extra = Math.min(3, Targeting.enemiesAround(player, player.position(), 6).size() - 1);
            if (extra > 0) event.setAmount(event.getAmount() * (1f + 0.10f * extra));
        }
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        // Veteran: Strength I for 4s on every kill
        if (Skills.has(player, "human_veteran"))
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0, false, true));
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        PowerState.flag(player, "human_in_combat", 160);                  // read by Steady Breath

        // Unyielding: heavy hits (over 3 hearts) are reduced by 20%
        if (Skills.has(player, "human_unyielding") && event.getAmount() > 6f) event.setAmount(event.getAmount() * 0.8f);

        // Swift: dropping below half health triggers a burst of Speed II (6s internal cooldown)
        if (Skills.has(player, "human_swift")
                && player.getHealth() - event.getAmount() < player.getMaxHealth() / 2
                && PowerState.tryTrigger(player, "human_swift", 120)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, true));
            AbyssVfx.shockwave(player.serverLevel(), player.position(), 1.6f, Palette.HUMAN, Palette.HUMAN_HI, 10);
        }
    }

    @Override
    public boolean onLethal(ServerPlayer player, DamageSource source) {
        // Unbroken: once every 5 minutes, survive a lethal hit
        if (!Skills.has(player, "human_unbroken") || !PowerState.tryTrigger(player, "human_unbroken", 6000)) return false;

        player.setHealth(1f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
        var level = player.serverLevel();
        AbyssVfx.of(VfxType.PILLAR).at(player.position()).scale(7).extra(0.9f).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(34).send(level);
        AbyssVfx.burst(level, Fx.chest(player), 1.4f, Palette.HUMAN, Palette.HUMAN_HI);
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
