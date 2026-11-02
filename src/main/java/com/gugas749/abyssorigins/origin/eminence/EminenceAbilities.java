package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.network.packet.QuakePacket;
import com.gugas749.abyssorigins.power.Delayed;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** The Eminence's own powers — the instruments of rule. */
public final class EminenceAbilities {

    private static final String K = "abyssorigins.ability.";

    private EminenceAbilities() {}

    // ---- Presence -------------------------------------------------------------------------

    /**
     * Every other player within 25 blocks must kneel (stunned 20s) or resist through a
     * skill check (three successes; failure costs half their health and 40s on their knees).
     * The ground quakes on use only — at most three seconds.
     */
    public static class Presence extends OriginAbility {
        public static final double RADIUS = 25;
        public static final int QUAKE_TICKS = 60;

        public Presence() {
            super("eminence_presence", K + "eminence_presence.name", K + "eminence_presence.desc", 75, 40);
            icon("♛"); color(0xFF000000 | Eminence.COLOR);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Vec3 c = player.position();
            quake(level, c);

            List<ServerPlayer> subjects = new ArrayList<>();
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(RADIUS))) {
                if (p == player || !p.isAlive() || p.isSpectator() || p.isCreative()) continue;
                if (p.distanceToSqr(c) > RADIUS * RADIUS || Eminence.is(p) || PresenceSessions.involved(p)) continue;
                subjects.add(p);
            }
            subjects.forEach(p -> PresenceSessions.begin(player, p));
            player.displayClientMessage(Component.translatable("abyssorigins.presence.cast", subjects.size())
                .withStyle(ChatFormatting.GOLD), true);
            return true;
        }

        /** Shaking camera for everyone near, cracking rings of ground, a royal toll. All within 3s. */
        public static void quake(ServerLevel level, Vec3 c) {
            PacketDistributor.sendToPlayersNear(level, null, c.x, c.y, c.z, RADIUS * 2,
                new QuakePacket(c.x, c.y, c.z, (float) RADIUS, QUAKE_TICKS, 1f));

            Vfx.of(VfxType.SIGIL).at(c).scale(4.5f).extra(8).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(QUAKE_TICKS).send(level);
            Vfx.of(VfxType.PILLAR).at(c).scale(7f).extra(0.9f).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(26).send(level);
            Vfx.burst(level, c.add(0, 1.2, 0), 1.6f, Eminence.COLOR_HI, 0xFFFFFF);
            Fx.sound(level, c, SoundEvents.BELL_BLOCK, 3f, 0.5f);
            Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.45f);
            Fx.sound(level, c, SoundEvents.RAVAGER_ROAR, 1.2f, 0.5f);

            // Three tremors, 15 ticks apart, each cracking a wider ring of the ground
            for (int wave = 0; wave < 3; wave++) {
                final float r = (float) RADIUS * (0.4f + 0.3f * wave);
                Delayed.schedule(1 + wave * 15, () -> {
                    Vfx.shockwave(level, c, r, Eminence.COLOR, Eminence.COLOR_HI, 22);
                    crackRing(level, c, r * 0.5, 22);
                    crackRing(level, c, r, 46);
                    Fx.sound(level, c, SoundEvents.DEEPSLATE_BREAK, 2.5f, 0.5f);
                    Fx.sound(level, c, SoundEvents.ANCIENT_DEBRIS_BREAK, 2f, 0.4f);
                });
            }
            Delayed.schedule(QUAKE_TICKS - 10, () -> Fx.sound(level, c, SoundEvents.BELL_RESONATE, 1.5f, 0.6f));
        }

        /** Chunks of the actual ground burst up around a ring. */
        private static void crackRing(ServerLevel level, Vec3 c, double radius, int points) {
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2 * i / points + level.random.nextDouble() * 0.2;
                double x = c.x + Math.cos(a) * radius, z = c.z + Math.sin(a) * radius;
                BlockPos ground = BlockPos.containing(x, c.y - 0.2, z);
                for (int dy = 0; dy < 4 && level.getBlockState(ground).isAir(); dy++) ground = ground.below();
                BlockState state = level.getBlockState(ground);
                if (state.isAir()) continue;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, ground.getY() + 1.05, z, 6, 0.3, 0.15, 0.3, 0.25);
                if (i % 3 == 0)
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, ground.getY() + 1.1, z, 1, 0.1, 0.05, 0.1, 0.01);
            }
        }
    }

    // ---- Royal Decree -----------------------------------------------------------------------

    /** Seals the powers of every enemy within 12 blocks; beasts lose their nerve and their quarry. */
    public static class RoyalDecree extends OriginAbility {
        private static final double RADIUS = 12;

        public RoyalDecree() {
            super("eminence_royal_decree", K + "eminence_royal_decree.name", K + "eminence_royal_decree.desc", 45, 30);
            icon("⚜"); color(0xFFE0B04A);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Vec3 c = player.position();
            int sealed = 0;
            for (LivingEntity e : Targeting.enemiesAround(player, c, RADIUS)) {
                if (e instanceof ServerPlayer p) {
                    if (Eminence.is(p)) continue;
                    p.addEffect(new MobEffectInstance(ModEffects.SEALED, 200, 0), player);
                    p.displayClientMessage(Component.translatable("abyssorigins.message.sealed_by", player.getDisplayName())
                        .withStyle(ChatFormatting.GOLD), true);
                } else {
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 60, 0), player);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1), player);
                    if (e instanceof Mob mob) mob.setTarget(null);
                }
                Vfx.of(VfxType.MARK).follow(e).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(40).send(level);
                sealed++;
            }
            Vfx.of(VfxType.DOME).at(c).scale((float) RADIUS).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(20).flags(Vfx.HALF).send(level);
            Vfx.of(VfxType.SIGIL).at(c).scale((float) RADIUS * 0.6f).extra(6).colors(Eminence.COLOR, 0xFFFFFF).duration(30).send(level);
            Fx.sound(level, c, SoundEvents.BELL_BLOCK, 2f, 0.8f);
            Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1f, 0.7f);
            player.displayClientMessage(Component.translatable("abyssorigins.message.decree", sealed).withStyle(ChatFormatting.GOLD), true);
            return true;
        }
    }

    // ---- Usurp --------------------------------------------------------------------------------

    /** Seize a target's blessings and their origin's power for yourself. */
    public static class Usurp extends OriginAbility {
        public Usurp() {
            super("eminence_usurp", K + "eminence_usurp.name", K + "eminence_usurp.desc", 35, 15);
            icon("✠"); color(0xFFC79BFF);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            LivingEntity target = Targeting.lookTarget(player, 16).orElse(null);
            if (target == null) {
                player.displayClientMessage(Component.translatable("abyssorigins.message.no_target"), true);
                return false;
            }
            ServerLevel level = player.serverLevel();
            int taken = 0;
            for (MobEffectInstance fx : new ArrayList<>(target.getActiveEffects())) {
                if (fx.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) continue;
                player.addEffect(new MobEffectInstance(fx.getEffect(), Math.min(fx.getDuration(), 600), fx.getAmplifier()));
                target.removeEffect(fx.getEffect());
                taken++;
            }
            float power = 0f;
            if (target instanceof ServerPlayer victim && !Eminence.is(victim)) {
                Origin o = AbyssOriginsAPI.getPlayerOrigin(victim);
                if (o != null && o.getResourceType() != null) {
                    power = Math.min(30f, Skills.resource(victim));
                    Skills.originData(victim).drainResource(power);
                    Skills.addResource(player, power);
                }
            }
            target.hurt(Targeting.spell(player), 4f);
            player.heal(4f);

            Vec3 from = Fx.chest(target), to = Fx.chest(player);
            Vfx.beam(level, from, to, 0.35f, 0xC79BFF, Eminence.COLOR_HI, 14);
            Vfx.burst(level, from, 0.9f, 0xC79BFF, Eminence.COLOR);
            Vfx.helix(level, player, 2.2f, Eminence.COLOR, 0xC79BFF, 24);
            Fx.sound(level, target.position(), SoundEvents.EVOKER_CAST_SPELL, 1f, 0.7f);
            Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.6f);
            player.displayClientMessage(Component.translatable("abyssorigins.message.usurped", taken, Math.round(power))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return true;
        }
    }
}
