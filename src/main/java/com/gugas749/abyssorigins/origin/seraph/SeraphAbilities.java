package com.gugas749.abyssorigins.origin.seraph;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import net.minecraft.core.Holder;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Seraph abilities: Ascendant Wings (R) and Divine Light (V). Both spend Radiance. */
public final class SeraphAbilities {

    private SeraphAbilities() {}

    // ---- Ascendant Wings ----------------------------------------------------

    public static class AscendantWings extends OriginAbility implements Sustained {
        public AscendantWings() {
            super("seraph_wings",
                "abyssorigins.ability.seraph_wings.name",
                "abyssorigins.ability.seraph_wings.desc",
                25, 25);
            icon("✧"); color(0xFFFFF1B8);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            boolean span = Skills.has(player, "seraph_wingspan");
            int duration = span ? 240 : 160;

            player.setDeltaMovement(player.getDeltaMovement().x, span ? 1.5 : 1.2, player.getDeltaMovement().z);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration + 40, 0, false, false));

            if (Skills.has(player, "seraph_ascension")) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }

            var level = player.serverLevel();
            Vfx.shockwave(level, player.position(), 2.6f, Palette.RADIANT, Palette.RADIANT_HI, 14);
            Vfx.of(VfxType.WINGS).follow(player).scale(1.15f).colors(0xFFE9A8, Palette.RADIANT_HI).duration(duration).send(level);
            Fx.sound(level, player.position(), SoundEvents.PHANTOM_FLAP, 1.0f, 1.4f);
            Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
            AbilityStates.start(player, getId(), duration, this);
            return true;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            player.fallDistance = 0;

            // Zephyr: sprint mid-air to glide forward
            if (Skills.has(player, "seraph_zephyr") && player.isSprinting() && !player.onGround()) {
                Vec3 look = player.getLookAngle();
                Vec3 v = player.getDeltaMovement().add(look.x * 0.08, 0, look.z * 0.08);
                double h = Math.sqrt(v.x * v.x + v.z * v.z);
                if (h > 1.2) v = new Vec3(v.x / h * 1.2, v.y, v.z / h * 1.2);
                player.setDeltaMovement(v);
                player.hurtMarked = true;
            }
        }

        @Override
        public void onEnd(ServerPlayer player) {
            if (Skills.has(player, "seraph_ascension") && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            Vfx.stop(player.serverLevel(), player, VfxType.WINGS);
            Vfx.sparks(player.serverLevel(), Fx.chest(player), 1.2f, Palette.RADIANT, Palette.RADIANT_HI);
        }
    }

    // ---- Divine Light -------------------------------------------------------

    public static class DivineLight extends OriginAbility {
        private static final double RADIUS = 8;

        public DivineLight() {
            super("seraph_divine_light",
                "abyssorigins.ability.seraph_divine_light.name",
                "abyssorigins.ability.seraph_divine_light.desc",
                30, 40);
            icon("☀"); color(0xFFF6D77A);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            var level = player.serverLevel();
            Vec3 c = player.position();
            boolean fallen = Skills.has(player, "seraph_fallen_grace");
            boolean mending = Skills.has(player, "seraph_mending_light");
            float heal = (6f + (mending ? 4f : 0f)) * (fallen ? 0.5f : 1f);

            for (LivingEntity ally : Targeting.alliesAround(player, c, RADIUS)) {
                ally.heal(heal);
                ally.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, 120, 0), player);
                if (mending) {
                    List<Holder<MobEffect>> bad = ally.getActiveEffects().stream().map(MobEffectInstance::getEffect)
                        .filter(e -> e.value().getCategory() == MobEffectCategory.HARMFUL).toList();
                    bad.forEach(ally::removeEffect);
                }
                Vfx.helix(level, ally, 2.0f, Palette.RADIANT, Palette.RADIANT_HI, 22);
            }

            boolean judgement = Skills.has(player, "seraph_judgement");
            for (LivingEntity enemy : Targeting.enemiesAround(player, c, RADIUS)) {
                if (Targeting.isUndead(enemy)) {
                    enemy.hurt(Targeting.spell(player), 8f);
                    enemy.igniteForSeconds(5);
                } else if (judgement) {
                    enemy.hurt(Targeting.spell(player), 4f);
                }
                if (judgement) enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
                Vfx.burst(level, Fx.chest(enemy), 0.7f, Palette.RADIANT, Palette.RADIANT_HI);
            }

            Vfx.of(VfxType.PILLAR).at(c).scale(10).extra(1.3f).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(34).send(level);
            Vfx.of(VfxType.SIGIL).at(c).scale((float) RADIUS * 0.5f).extra(8).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(40).send(level);
            Vfx.shockwave(level, c, (float) RADIUS, Palette.RADIANT, Palette.RADIANT_HI, 24);
            Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.5f);
            Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
            return true;
        }
    }
}
