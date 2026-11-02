package com.gugas749.abyssorigins.origin.voidborn;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.*;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Voidborn abilities: Void Step (R) and Rift Collapse (V). Both spend Void Essence. */
public final class VoidbornAbilities {

    private VoidbornAbilities() {}

    // ---- Void Step ----------------------------------------------------------

    public static class VoidStep extends OriginAbility {
        public VoidStep() {
            super("void_step",
                "abyssorigins.ability.void_step.name",
                "abyssorigins.ability.void_step.desc",
                8, 15);
            icon("✴"); color(0xFFB08CFF);
        }

        @Override
        public int resourceCost(ServerPlayer player) {
            return Skills.has(player, "void_abyss_walker") ? 0 : getResourceCost();
        }

        @Override
        public int cooldownTicks(ServerPlayer player) {
            int cd = Skills.has(player, "void_blink_mastery") ? getBaseCooldownTicks() - 60 : getBaseCooldownTicks();
            return Skills.has(player, "void_abyss_walker") ? cd / 2 : cd;
        }

        @Override
        public boolean activate(ServerPlayer player) {
            double range = Skills.has(player, "void_blink_mastery") ? 24 : 16;
            ServerLevel level = player.serverLevel();
            Vec3 from = player.position();
            Vec3 look = player.getLookAngle();
            Vec3 point = Targeting.lookPoint(player, range).subtract(look.scale(0.6));

            if (!player.randomTeleport(point.x, point.y, point.z, true)) {
                player.displayClientMessage(Component.translatable("abyssorigins.message.void_step_blocked"), true);
                return false;
            }
            player.fallDistance = 0;
            Vec3 to = player.position();

            Vfx.of(VfxType.RIFT).at(from.add(0, 1, 0)).dir(look).scale(2.4f).colors(Palette.VOID, Palette.VOID_HI).duration(16).send(level);
            Vfx.of(VfxType.RIFT).at(to.add(0, 1, 0)).dir(look).scale(2.4f).colors(Palette.VOID, Palette.VOID_HI).duration(16).send(level);
            Vfx.of(VfxType.BEAM).at(from.add(0, 1, 0)).to(to.add(0, 1, 0)).extra(0.18f).colors(Palette.VOID, Palette.VOID_HI).flags(Vfx.VOID).duration(10).send(level);
            Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.3f);
            Fx.sound(level, to, SoundEvents.SHULKER_TELEPORT, 0.6f, 0.7f);

            if (Skills.has(player, "void_unseen_step")) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
                PowerState.flag(player, "void_empowered", 60);
            }

            // Echo: the departure point detonates a second later
            if (Skills.has(player, "void_echo")) {
                Vfx.of(VfxType.SIGIL).at(from).scale(1.6f).extra(5).colors(Palette.VOID, Palette.VOID_HI).duration(24).send(level);
                Delayed.schedule(20, () -> {
                    if (!player.isAlive() || player.level() != level) return;
                    for (LivingEntity e : Targeting.enemiesAround(player, from, 3))
                        e.hurt(Targeting.spell(player), 5f);
                    Vfx.burst(level, from.add(0, 1, 0), 1.4f, Palette.VOID, Palette.VOID_HI);
                    Vfx.shockwave(level, from, 3, Palette.VOID, Palette.VOID_HI, 14);
                    Fx.sound(level, from, SoundEvents.ENDER_EYE_DEATH, 1.0f, 0.6f);
                });
            }
            return true;
        }
    }

    // ---- Rift Collapse ------------------------------------------------------

    public static class RiftCollapse extends OriginAbility implements Sustained {
        private static final Map<UUID, Vec3> RIFTS = new HashMap<>();

        public RiftCollapse() {
            super("void_rift_collapse",
                "abyssorigins.ability.void_rift_collapse.name",
                "abyssorigins.ability.void_rift_collapse.desc",
                30, 45);
            icon("⦿"); color(0xFF6B3FD0);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            Vec3 center = Targeting.lookPoint(player, 20).subtract(player.getLookAngle().scale(1.0)).add(0, 0.5, 0);
            RIFTS.put(player.getUUID(), center);
            int duration = Skills.has(player, "void_heart_of_abyss") ? 120 : 60;
            Vfx.of(VfxType.VORTEX).at(center).scale((float) radius(player)).colors(Palette.VOID, Palette.VOID_DARK).duration(duration).send(player.serverLevel());
            Fx.sound(player.level(), center, SoundEvents.PORTAL_TRIGGER, 0.6f, 1.6f);
            AbilityStates.start(player, getId(), duration, this);
            return true;
        }

        private static double radius(ServerPlayer player) {
            return Skills.has(player, "void_singularity") ? 10 : 7;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            Vec3 c = RIFTS.get(player.getUUID());
            if (c == null) return;
            ServerLevel level = player.serverLevel();
            double r = radius(player);

            for (LivingEntity e : Targeting.enemiesAround(player, c, r)) {
                Vec3 pull = c.subtract(e.position()).normalize().scale(0.12);
                e.push(pull.x, pull.y * 0.5 + 0.01, pull.z);
                e.hurtMarked = true;
            }
            if (player.tickCount % 20 == 0) Fx.sound(level, c, SoundEvents.BEACON_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        public void onEnd(ServerPlayer player) {
            Vec3 c = RIFTS.remove(player.getUUID());
            if (c == null) return;
            ServerLevel level = player.serverLevel();
            float dmg = 8f + (Skills.has(player, "void_singularity") ? 4f : 0f);
            if (Skills.has(player, "void_heart_of_abyss")) dmg *= 2f;

            for (LivingEntity e : Targeting.enemiesAround(player, c, radius(player) * 0.6)) {
                e.hurt(Targeting.spell(player), dmg);
                e.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 100, 1), player);
            }
            Vfx.burst(level, c, 2.4f, Palette.VOID, Palette.VOID_HI);
            Vfx.of(VfxType.BURST).at(c).scale(1.6f).colors(Palette.VOID_DARK, Palette.VOID).flags(Vfx.VOID).duration(14).send(level);
            Vfx.shockwave(level, c.subtract(0, 1.2, 0), (float) radius(player), Palette.VOID, Palette.VOID_HI, 18);
            Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.4f);
        }
    }
}
