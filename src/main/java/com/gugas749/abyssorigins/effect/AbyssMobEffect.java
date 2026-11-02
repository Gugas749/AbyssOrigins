package com.gugas749.abyssorigins.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Generic mod effect. Most effects are passive markers whose behaviour is applied
 * by CombatEvents (Hexed, Sanctified, Hunter's Mark) or by attribute modifiers
 * (Sundered, Stunned). Bleeding and Stunned also need per-tick logic.
 */
public class AbyssMobEffect extends MobEffect {

    public enum Kind { MARKER, BLEEDING, STUNNED }

    private final Kind kind;

    public AbyssMobEffect(MobEffectCategory category, int color, Kind kind) {
        super(category, color);
        this.kind = kind;
    }

    public AbyssMobEffect(MobEffectCategory category, int color, Kind kind, ParticleOptions particle) {
        super(category, color, particle);
        this.kind = kind;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return switch (kind) {
            // Bleeding I: 1 dmg / 30 ticks, II: / 20, III: / 15 ...
            case BLEEDING -> duration % Math.max(10, 30 - amplifier * 10) == 0;
            case STUNNED  -> true;
            case MARKER   -> false;
        };
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        switch (kind) {
            case BLEEDING -> {
                // Bypasses armour like poison, but unlike poison it can kill.
                entity.hurt(entity.damageSources().magic(), 1.0f);
            }
            case STUNNED -> {
                if (entity instanceof Mob mob) {
                    mob.getNavigation().stop();
                    if (mob.getTarget() != null) mob.setTarget(null);
                }
            }
            case MARKER -> {}
        }
        return true;
    }
}
