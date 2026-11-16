package com.gugas749.abyssorigins.origin.voidborn;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.*;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
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

/** Voidborn abilities: Void Step (R), Rift Collapse (V) and Echo Strikes (taught by the tree). All spend Void Essence. */
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

            AbyssVfx.of(VfxType.RIFT).at(from.add(0, 1, 0)).dir(look).scale(2.4f).colors(Palette.VOID, Palette.VOID_HI).duration(16).send(level);
            AbyssVfx.of(VfxType.RIFT).at(to.add(0, 1, 0)).dir(look).scale(2.4f).colors(Palette.VOID, Palette.VOID_HI).duration(16).send(level);
            AbyssVfx.of(VfxType.BEAM).at(from.add(0, 1, 0)).to(to.add(0, 1, 0)).extra(0.18f).colors(Palette.VOID, Palette.VOID_HI).flags(AbyssVfx.VOID).duration(10).send(level);
            Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.3f);
            Fx.sound(level, to, SoundEvents.SHULKER_TELEPORT, 0.6f, 0.7f);

            if (Skills.has(player, "void_unseen_step")) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
                PowerState.flag(player, "void_empowered", 60);
            }

            // Echo: the departure point detonates a second later
            if (Skills.has(player, "void_echo")) {
                AbyssVfx.of(VfxType.SIGIL).at(from).scale(1.6f).extra(5).colors(Palette.VOID, Palette.VOID_HI).duration(24).send(level);
                Delayed.schedule(20, () -> {
                    if (!player.isAlive() || player.level() != level) return;
                    for (LivingEntity e : Targeting.enemiesAround(player, from, 3))
                        e.hurt(Targeting.spell(player), 5f);
                    AbyssVfx.burst(level, from.add(0, 1, 0), 1.4f, Palette.VOID, Palette.VOID_HI);
                    AbyssVfx.shockwave(level, from, 3, Palette.VOID, Palette.VOID_HI, 14);
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
            AbyssVfx.of(VfxType.VORTEX).at(center).scale((float) radius(player)).colors(Palette.VOID, Palette.VOID_DARK).duration(duration).send(player.serverLevel());
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
            AbyssVfx.burst(level, c, 2.4f, Palette.VOID, Palette.VOID_HI);
            AbyssVfx.of(VfxType.BURST).at(c).scale(1.6f).colors(Palette.VOID_DARK, Palette.VOID).flags(AbyssVfx.VOID).duration(14).send(level);
            AbyssVfx.shockwave(level, c.subtract(0, 1.2, 0), (float) radius(player), Palette.VOID, Palette.VOID_HI, 18);
            Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.4f);
        }
    }

    // ---- Echo Strikes (skill node "void_echo_strikes") ---------------------------------

    /**
     * For 12 seconds every melee hit is repeated a moment later
     * for 75% of the damage it dealt. Echoes never echo. Press again to end it early.
     */
    public static class EchoStrikes extends OriginAbility implements Sustained {
        public static final String ID = "void_echo_strikes";
        public static final float SHARE = 0.75f;
        private static final int DURATION = 240, DELAY = 6;
        /** Set while an echo lands, so its own damage is not echoed again. */
        private static boolean echoing;

        public EchoStrikes() {
            super(ID, "abyssorigins.ability." + ID + ".name", "abyssorigins.ability." + ID + ".desc", 40, 35);
            icon("❖"); color(0xFFB48CFF);
        }

        public static boolean active(ServerPlayer p) { return AbilityStates.isActive(p, ID); }

        @Override
        public boolean activate(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            AbyssVfx.helix(level, player, 2.2f, Palette.VOID, Palette.VOID_HI, 22);
            AbyssVfx.of(VfxType.RIFT).at(Fx.chest(player)).dir(player.getLookAngle()).scale(2.4f).colors(Palette.VOID, Palette.VOID_HI).duration(14).send(level);
            AbyssVfx.burst(level, Fx.chest(player), 1.0f, Palette.VOID_DARK, Palette.VOID_HI);
            Fx.sound(level, player.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0f, 0.9f);
            Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f);
            AbilityStates.start(player, getId(), DURATION, this);
            return true;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            if (remaining % 20 == 0)
                AbyssVfx.of(VfxType.AURA).follow(player).scale(1.3f).colors(Palette.VOID_DARK, Palette.VOID).flags(AbyssVfx.VOID).duration(24).send(player.serverLevel());
        }

        @Override
        public void onEnd(ServerPlayer player) {
            AbyssVfx.sparks(player.serverLevel(), Fx.chest(player), 0.8f, Palette.VOID, Palette.VOID_HI);
        }

        /** Called from VoidbornHooks.onDamageDealt for each melee blow while the strikes are active. */
        public static void echo(ServerPlayer player, LivingEntity target, float dealt) {
            if (echoing || dealt <= 0) return;
            ServerLevel level = player.serverLevel();
            // Echo VFX at once, damage shortly after
            AbyssVfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(target.position().subtract(player.position()))
                .scale(1.1f).extra(140).colors(Palette.VOID_DARK, Palette.VOID_HI).flags(AbyssVfx.REVERSE).duration(DELAY + 6).send(level);
            Delayed.schedule(DELAY, () -> {
                if (!target.isAlive() || !player.isAlive() || target.level() != player.level()) return;
                echoing = true;
                try {
                    target.invulnerableTime = 0;                  // the echo is its own blow, not swallowed by i-frames
                    target.hurt(Targeting.spell(player), dealt * SHARE);
                } finally {
                    echoing = false;
                }
                AbyssVfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(target.position().subtract(player.position()))
                    .scale(1.3f).extra(150).colors(Palette.VOID, Palette.VOID_HI).flags(AbyssVfx.VERTICAL).duration(10).send(level);
                AbyssVfx.burst(level, Fx.chest(target), 0.7f, Palette.VOID, Palette.VOID_HI);
                Fx.sound(level, target.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9f, 0.7f);
                Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.7f, 0.6f);
            });
        }
    }
}
