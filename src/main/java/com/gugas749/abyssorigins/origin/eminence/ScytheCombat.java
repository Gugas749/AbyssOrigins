package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Sovereign Scythe's melee chain (stages counted by {@link ScytheCombo}):
 *  1. Reaping Sweep  - a flat swing; everything else in a 150° arc ahead takes 60% of the blow.
 *  2. Sovereign Strike - an overhead chop: +35% damage, the target is slowed and shoved down.
 *  3. Harvest        - a rising backhand reap: 80% of the blow to everything in a 220° arc, pulled
 *                      toward you and given Wither. Then the chain begins again.
 * Every swing - hit or miss - leaves a black-and-gold arc in the air.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class ScytheCombat {

    private static final int DARK = 0x1C1426;
    private static final double REACH = 4.5;
    private static boolean cleaving;

    private ScytheCombat() {}

    private static boolean wielding(ServerPlayer p) {
        return SovereignScythe.isScythe(p.getMainHandItem()) && SovereignScythe.bonded(p, p.getMainHandItem());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !ScytheCombo.swingStarted(p) || !wielding(p)) return;
        int stage = ScytheCombo.advance(p);
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 at = p.position().add(0, p.getBbHeight() * 0.55, 0).add(flat.scale(0.6));
        switch (stage) {
            case ScytheCombo.SWEEP -> {
                AbyssVfx.of(VfxType.SLASH).at(at).dir(flat).scale(3.4f).extra(170).colors(DARK, Eminence.COLOR).duration(10).send(level);
                Fx.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.55f);
            }
            case ScytheCombo.STRIKE -> {
                AbyssVfx.of(VfxType.SLASH).at(at).dir(flat).scale(2.8f).extra(150).colors(DARK, Eminence.COLOR_HI)
                    .flags(AbyssVfx.VERTICAL).duration(10).send(level);
                Fx.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.5f);
            }
            default -> {
                AbyssVfx.of(VfxType.SLASH).at(at.add(0, 0.3, 0)).dir(flat).scale(3.8f).extra(220).colors(DARK, Eminence.COLOR)
                    .flags(AbyssVfx.REVERSE).duration(12).send(level);
                Fx.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.4f);
                Fx.sound(level, p.position(), SoundEvents.WITHER_SHOOT, 0.4f, 0.5f);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (cleaving) return;
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !Targeting.isMelee(event.getSource(), p) || !wielding(p)) return;

        float amount = event.getAmount();
        ServerLevel level = p.serverLevel();
        switch (ScytheCombo.peek(p)) {                         // the swing that is landing right now
            case ScytheCombo.SWEEP -> cleave(p, target, amount * (0.6f + sweeping(p)), 150, false);
            case ScytheCombo.STRIKE -> {
                event.setAmount(amount * 1.35f);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), p);
                target.setDeltaMovement(target.getDeltaMovement().multiply(0.4, 0, 0.4).add(0, -0.3, 0));
                target.hurtMarked = true;
                AbyssVfx.burst(level, Fx.chest(target), 0.8f, DARK, Eminence.COLOR_HI);
                Fx.sound(level, target.position(), SoundEvents.ANVIL_LAND, 0.35f, 0.5f);
            }
            default -> {
                cleave(p, target, amount * (0.8f + sweeping(p)), 220, true);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), p);
            }
        }
    }

    /** Sweeping Edge on the scythe strengthens the chain's cleaves. */
    private static float sweeping(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * 0.5f;
    }

    /** Hit everything else in an arc ahead of {@code p}. */
    private static void cleave(ServerPlayer p, LivingEntity main, float damage, double arcDegrees, boolean pull) {
        Vec3 c = p.position();
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        double cos = Math.cos(Math.toRadians(arcDegrees / 2));
        cleaving = true;
        try {
            for (LivingEntity e : Targeting.preyAround(p, c, REACH)) {
                if (e == main) continue;
                Vec3 to = e.position().subtract(c).multiply(1, 0, 1);
                if (to.lengthSqr() > 1e-4 && to.normalize().dot(look) < cos) continue;
                if (!e.hurt(p.damageSources().playerAttack(p), damage)) continue;
                if (pull) {
                    Vec3 in = c.subtract(e.position()).multiply(1, 0, 1).normalize().scale(0.55);
                    e.push(in.x, 0.1, in.z);
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), p);
                } else {
                    e.knockback(0.4, -to.x, -to.z);
                }
            }
        } finally {
            cleaving = false;
        }
    }
}
