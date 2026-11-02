package com.gugas749.abyssorigins.specialization.ranger;

import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.AbilityController;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Ranger mechanics — rewards distance, marks and fully drawn shots. */
public class RangerHooks implements PowerHooks {

    private static boolean ownProjectile(DamageSource src, ServerPlayer p) {
        return Targeting.isProjectile(src) && src.getEntity() == p;
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (!ownProjectile(src, player)) return;

        float mult = Skills.has(player, "ranger_eagle_eye") ? 1.25f : 1.15f;          // Steady Aim
        double dist = player.distanceTo(target);
        mult *= 1f + (float) Math.min(0.30, Math.max(0, (dist - 10) * 0.02));          // Hunter's Instinct
        if (Skills.has(player, "ranger_apex_hunter")) mult *= 1.2f;

        if (Skills.has(player, "ranger_heartseeker") && target.hasEffect(ModEffects.HUNTERS_MARK)
                && src.getDirectEntity() instanceof AbstractArrow arrow && arrow.isCritArrow()) {
            mult *= 1.5f;
            Vfx.burst(player.serverLevel(), Fx.chest(target), 0.8f, Palette.HUNT, Palette.HUNT_HI);
        }
        if (Skills.isOrigin(player, "seraph") && Targeting.isUndead(target)) {         // Sunshot
            mult *= 1.2f;
            target.igniteForSeconds(4);
        }
        event.setAmount(event.getAmount() * mult);
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        // Survivalist: disengage leap away from melee attackers (5s)
        if (!Skills.has(player, "ranger_survivalist")) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getSource().getDirectEntity() != attacker) return;
        if (!PowerState.tryTrigger(player, "ranger_disengage", 100)) return;

        Vec3 away = player.position().subtract(attacker.position()).multiply(1, 0, 1).normalize().scale(1.1);
        player.setDeltaMovement(away.x, 0.45, away.z);
        player.hurtMarked = true;
        Vfx.shockwave(player.serverLevel(), player.position(), 1.8f, Palette.HUNT, Palette.HUNT_HI, 10);
        Fx.sound(player.level(), player.position(), SoundEvents.PHANTOM_FLAP, 0.7f, 1.6f);
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (Skills.has(player, "ranger_quarry") && victim.hasEffect(ModEffects.HUNTERS_MARK)) {
            AbilityController.reduceClassCooldown(player, 0, 20 * 60);
            player.heal(4f);
        }
        if (Skills.has(player, "ranger_fleetfoot") && ownProjectile(source, player))
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, true));
    }
}
