package com.gugas749.abyssorigins.origin.demon;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Demon abilities: Hellfire Nova (R) and Infernal Charge (V). Both spend Wrath. */
public final class DemonAbilities {

    private DemonAbilities() {}

    // ---- Hellfire Nova ------------------------------------------------------

    public static class HellfireNova extends OriginAbility {
        public HellfireNova() {
            super("demon_hellfire_nova",
                "abyssorigins.ability.demon_hellfire_nova.name",
                "abyssorigins.ability.demon_hellfire_nova.desc",
                20, 30);
            icon("✹"); color(0xFFFF6A1F);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            erupt(player, 1f);
            return true;
        }

        /** Shared with Ember Heart's free nova. */
        public static void erupt(ServerPlayer player, float power) {
            var level = player.serverLevel();
            double radius = Skills.has(player, "demon_hellfire_mastery") ? 7 : 5;
            Vec3 c = player.position();

            for (LivingEntity e : Targeting.enemiesAround(player, c, radius)) {
                e.hurt(Targeting.spell(player), 6f * power);
                e.igniteForSeconds(5);
                Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize().scale(0.9);
                e.push(push.x, 0.35, push.z);
                e.hurtMarked = true;
                if (Skills.has(player, "demon_hellfire_mastery"))
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), player);
            }

            Vfx.shockwave(level, c, (float) radius, Palette.INFERNO, Palette.INFERNO_HI, 22);
            Vfx.of(VfxType.SIGIL).at(c).scale((float) radius * 0.55f).extra(5).colors(Palette.INFERNO, Palette.INFERNO_HI).duration(26).send(level);
            Vfx.burst(level, c.add(0, 1, 0), 1.3f * power, Palette.INFERNO, Palette.INFERNO_HI);
            for (int i = 0; i < 6; i++) {                       // eruption columns around the rim
                double a = Math.PI * 2 * i / 6 + player.getRandom().nextDouble() * 0.4;
                Vec3 at = c.add(Math.cos(a) * radius * 0.7, 0, Math.sin(a) * radius * 0.7);
                Vfx.of(VfxType.PILLAR).at(at).scale(2.8f).extra(0.35f).colors(Palette.INFERNO, Palette.INFERNO_HI).duration(18).send(level);
            }
            Fx.sound(level, c, SoundEvents.BLAZE_SHOOT, 1.2f, 0.6f);
            Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.4f);
        }
    }

    // ---- Infernal Charge ----------------------------------------------------

    public static class InfernalCharge extends OriginAbility implements Sustained {
        private static final Map<UUID, Set<Integer>> HIT = new HashMap<>();
        private static final Map<UUID, Vec3> DIRECTION = new HashMap<>();

        public InfernalCharge() {
            super("demon_infernal_charge",
                "abyssorigins.ability.demon_infernal_charge.name",
                "abyssorigins.ability.demon_infernal_charge.desc",
                14, 20);
            icon("➹"); color(0xFFE0451A);
        }

        @Override
        public int resourceCost(ServerPlayer player) {
            return Skills.has(player, "demon_cinder_dash") ? 10 : 20;
        }

        @Override
        public boolean activate(ServerPlayer player) {
            Vec3 dir = player.getLookAngle().multiply(1, 0, 1);
            if (dir.lengthSqr() < 1.0E-4) return false;
            DIRECTION.put(player.getUUID(), dir.normalize());
            HIT.put(player.getUUID(), new HashSet<>());
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 0, false, false));
            Fx.sound(player.level(), player.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.7f);
            int ticks = Skills.has(player, "demon_cinder_dash") ? 12 : 8;
            Vfx.of(VfxType.TRAIL).follow(player).scale(1.1f).colors(Palette.INFERNO, Palette.INFERNO_HI).duration(ticks + 10).send(player.serverLevel());
            Vfx.burst(player.serverLevel(), Fx.chest(player), 0.9f, Palette.INFERNO, Palette.INFERNO_HI);
            AbilityStates.start(player, getId(), ticks, this);
            return true;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            Vec3 dir = DIRECTION.get(player.getUUID());
            if (dir == null) return;
            player.setDeltaMovement(dir.x * 1.25, Math.min(player.getDeltaMovement().y, 0.1), dir.z * 1.25);
            player.hurtMarked = true;
            player.fallDistance = 0;

            var level = player.serverLevel();

            Set<Integer> hit = HIT.get(player.getUUID());
            for (LivingEntity e : Targeting.enemiesAround(player, player.position(), 1.8)) {
                if (hit == null || !hit.add(e.getId())) continue;
                e.hurt(Targeting.spell(player), 4f);
                e.igniteForSeconds(3);
                e.push(dir.x * 0.6, 0.4, dir.z * 0.6);
                e.hurtMarked = true;
                Vfx.burst(level, Fx.chest(e), 0.7f, Palette.INFERNO, Palette.INFERNO_HI);
            }
        }

        @Override
        public void onEnd(ServerPlayer player) {
            HIT.remove(player.getUUID());
            DIRECTION.remove(player.getUUID());
            player.setDeltaMovement(player.getDeltaMovement().scale(0.3));
            player.hurtMarked = true;
            Vfx.shockwave(player.serverLevel(), player.position(), 2.2f, Palette.INFERNO, Palette.INFERNO_HI, 12);
        }
    }
}
