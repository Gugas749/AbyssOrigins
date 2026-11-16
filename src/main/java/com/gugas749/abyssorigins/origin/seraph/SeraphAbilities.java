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
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
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

    /**
     * Wings of light and flight (like the Vampire's Bat Form). Wingspan lifts higher and flies longer,
     * Archangel flies faster, Zephyr surges forward while sprinting. Press again to fold the wings early.
     */
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
            int duration = duration(player);

            player.setDeltaMovement(player.getDeltaMovement().x, span ? 0.9 : 0.6, player.getDeltaMovement().z);
            player.hurtMarked = true;

            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlyingSpeed(flightSpeed(player));
            player.onUpdateAbilities();

            var level = player.serverLevel();
            AbyssVfx.shockwave(level, player.position(), 2.6f, Palette.RADIANT, Palette.RADIANT_HI, 14);
            AbyssVfx.of(VfxType.WINGS).follow(player).scale(1.15f).colors(0xFFE9A8, Palette.RADIANT_HI).duration(duration).send(level);
            Fx.sound(level, player.position(), SoundEvents.PHANTOM_FLAP, 1.0f, 1.4f);
            Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
            AbilityStates.start(player, getId(), duration, this);
            return true;
        }

        private static int duration(ServerPlayer player) {
            return Skills.has(player, "seraph_wingspan") ? 400 : 240;
        }

        private static float flightSpeed(ServerPlayer player) {
            return Skills.has(player, "seraph_ascension") ? 0.11f : 0.08f;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            player.fallDistance = 0;
            // The launch tick still counts as "on the ground", and landing ends flight: hold it for the first moments
            if (!player.getAbilities().flying && duration(player) - remaining < 12 && !player.isSpectator()) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            if (!player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            if (player.getAbilities().flying && player.tickCount % 14 == 0)
                Fx.sound(player.level(), player.position(), SoundEvents.PHANTOM_FLAP, 0.3f, 1.6f);
            if (remaining == 60)
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("abyssorigins.message.wings_ending_soon"), true);

            // Zephyr: sprint mid-air to surge forward
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
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
            }
            player.getAbilities().setFlyingSpeed(0.05f);
            player.onUpdateAbilities();
            // A short slow fall so folding the wings mid-air is not lethal
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
            AbyssVfx.stop(player.serverLevel(), player, VfxType.WINGS);
            AbyssVfx.sparks(player.serverLevel(), Fx.chest(player), 1.2f, Palette.RADIANT, Palette.RADIANT_HI);
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
                AbyssVfx.helix(level, ally, 2.0f, Palette.RADIANT, Palette.RADIANT_HI, 22);
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
                AbyssVfx.burst(level, Fx.chest(enemy), 0.7f, Palette.RADIANT, Palette.RADIANT_HI);
            }

            AbyssVfx.of(VfxType.PILLAR).at(c).scale(10).extra(1.3f).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(34).send(level);
            AbyssVfx.of(VfxType.SIGIL).at(c).scale((float) RADIUS * 0.5f).extra(8).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(40).send(level);
            AbyssVfx.shockwave(level, c, (float) RADIUS, Palette.RADIANT, Palette.RADIANT_HI, 24);
            Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.5f);
            Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
            return true;
        }
    }

    // ---- Judgement (skill node "seraph_judgement_blade") ---------------------------------

    /**
     * Judgement: a sigil appears where the seraph looks (up to 32 blocks), and a giant sword of
     * light falls from the sky onto it point-first (JudgementSwordEntity) - 14 damage to enemies within 5
     * blocks of the impact (undead take double and burn), launching them up and Stunning them for 1.5s.
     */
    public static class Judgement extends OriginAbility {
        public static final String ID = "seraph_judgement_blade";
        private static final double RANGE = 32;

        public Judgement() {
            super(ID, "abyssorigins.ability." + ID + ".name", "abyssorigins.ability." + ID + ".desc", 45, 50);
            icon("✝"); color(0xFFFFE9A6);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            var level = player.serverLevel();
            var hit = Targeting.lookBlock(player, RANGE);
            Vec3 target = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? ground(level, hit.getLocation())
                : hit.getLocation();
            if (target == null) return false;
            com.gugas749.abyssorigins.entity.JudgementSwordEntity.strike(player, target);
            AbyssVfx.burst(level, Fx.hand(player), 0.6f, Palette.RADIANT, Palette.RADIANT_HI);
            Fx.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.4f);
            return true;
        }

        /** The ground under a mid-air point (the look ray missed): straight down up to 32 blocks. */
        private static Vec3 ground(net.minecraft.server.level.ServerLevel level, Vec3 at) {
            var down = level.clip(new net.minecraft.world.level.ClipContext(at, at.add(0, -RANGE, 0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
            return down.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : down.getLocation();
        }
    }
}
