package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abysscore.api.cinematic.AbyssCinematics;
import com.gugas749.abyssorigins.power.Delayed;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abyssorigins.vfx.OriginVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** The Eminence's own powers. */
public final class EminenceAbilities {

    private static final String K = "abyssorigins.ability.";

    private EminenceAbilities() {}

    // ---- Presence -------------------------------------------------------------------------

    /**
     * Every other player within 25 blocks must kneel (stunned 20s) or resist through a
     * skill check (three successes; failure costs half their health and 40s on their knees).
     * The quake effect plays on use only (at most three seconds).
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

        /** Camera shake for nearby players, ground-crack rings and bell sounds. All within 3s. */
        public static void quake(ServerLevel level, Vec3 c) {
            AbyssCinematics.quake(level, c, (float) RADIUS, QUAKE_TICKS, 1f);

            AbyssVfx.of(VfxType.SIGIL).at(c).scale(4.5f).extra(8).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(QUAKE_TICKS).send(level);
            AbyssVfx.of(VfxType.PILLAR).at(c).scale(7f).extra(0.9f).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(26).send(level);
            AbyssVfx.burst(level, c.add(0, 1.2, 0), 1.6f, Eminence.COLOR_HI, 0xFFFFFF);
            Fx.sound(level, c, SoundEvents.BELL_BLOCK, 3f, 0.5f);
            Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.45f);
            Fx.sound(level, c, SoundEvents.RAVAGER_ROAR, 1.2f, 0.5f);

            // Three waves, 15 ticks apart, each with a wider ring of ground particles
            for (int wave = 0; wave < 3; wave++) {
                final float r = (float) RADIUS * (0.4f + 0.3f * wave);
                Delayed.schedule(1 + wave * 15, () -> {
                    AbyssVfx.shockwave(level, c, r, Eminence.COLOR, Eminence.COLOR_HI, 22);
                    crackRing(level, c, r * 0.5, 22);
                    crackRing(level, c, r, 46);
                    Fx.sound(level, c, SoundEvents.DEEPSLATE_BREAK, 2.5f, 0.5f);
                    Fx.sound(level, c, SoundEvents.ANCIENT_DEBRIS_BREAK, 2f, 0.4f);
                });
            }
            Delayed.schedule(QUAKE_TICKS - 10, () -> Fx.sound(level, c, SoundEvents.BELL_RESONATE, 1.5f, 0.6f));
        }

        /** Block particles of the ground along a ring. */
        static void crackRing(ServerLevel level, Vec3 c, double radius, int points) {
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

    /** Seals the powers of every enemy player within 12 blocks; mobs are stunned, weakened and lose their target. */
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
                AbyssVfx.of(VfxType.MARK).follow(e).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(40).send(level);
                sealed++;
            }
            AbyssVfx.of(VfxType.DOME).at(c).scale((float) RADIUS).colors(Eminence.COLOR, Eminence.COLOR_HI).duration(20).flags(AbyssVfx.HALF).send(level);
            AbyssVfx.of(VfxType.SIGIL).at(c).scale((float) RADIUS * 0.6f).extra(6).colors(Eminence.COLOR, 0xFFFFFF).duration(30).send(level);
            Fx.sound(level, c, SoundEvents.BELL_BLOCK, 2f, 0.8f);
            Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1f, 0.7f);
            player.displayClientMessage(Component.translatable("abyssorigins.message.decree", sealed).withStyle(ChatFormatting.GOLD), true);
            return true;
        }
    }

    // ---- Mire Call -----------------------------------------------------------------------------

    /**
     * Summons the Sovereign Scythe out of a mire pool beside you into your hand, as you last left it
     * (enchantments included). Cast again while carrying it to put it away. See SovereignScythe.
     */
    public static class MireCall extends OriginAbility {
        public MireCall() {
            super("eminence_mire_call", K + "eminence_mire_call.name", K + "eminence_mire_call.desc", 6, 10);
            icon("☽"); color(0xFF8C7AA8);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            return SovereignScythe.call(player);
        }
    }

    // ---- I Am Atomic -------------------------------------------------------------------------

    /**
     * Ultimate, based on Shadow's "I Am Atomic" (The Eminence in Shadow). Requires the bonded Sovereign
     * Scythe in the main hand. A bubble marks the blast area ({@link #RADIUS} blocks) and the caster is held
     * in place, raising the scythe while violet energy gathers into an "atom" above them; at {@link #CUT}
     * the scythe cuts the atom in two and at {@link #DETONATE} it explodes: every enemy inside takes
     * {@link #MIN_DAMAGE}-{@link #MAX_DAMAGE} spell damage (more near the centre) and is thrown outward, and
     * the blocks inside are destroyed ({@link AtomicBlast}). Allies are not hit and get Slow Falling. The
     * caster hovers until the crater is done, then floats down with Slow Falling.
     *
     * Every player within the radius gets the cinematic (letterbox bars, vignette, flash, subtitles; see
     * {@link AtomicCinematic}); the 3D effect is {@link OriginVfx#ATOMIC}. Both run on the shared timeline below.
     */
    public static class Atomic extends OriginAbility {
        public static final String ID = "eminence_atomic";
        public static final double RADIUS = 100;
        public static final float MIN_DAMAGE = 20f, MAX_DAMAGE = 60f;
        /** Timeline in ticks from the cast: scythe fully raised, swing starts, cut, explosion, end. */
        public static final int RAISED = 14, SWING = 50, CUT = 56, DETONATE = 62, TOTAL = 170;
        /** Height of the gathered atom above the caster's feet. */
        public static final double ATOM_HEIGHT = 4.6;
        public static final int VIOLET = 0x7B4DFF, VIOLET_HI = 0xE4DAFF;

        public Atomic() {
            super(ID, K + ID + ".name", K + ID + ".desc", 300, 100);
            icon("⚛"); color(0xFF000000 | VIOLET);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            if (!SovereignScythe.isScythe(held) || !SovereignScythe.bonded(player, held)) {
                player.displayClientMessage(Component.translatable("abyssorigins.atomic.need_scythe")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
                return false;
            }
            ServerLevel level = player.serverLevel();
            Vec3 c = player.position();

            AtomicCinematic.play(player, c, RADIUS, RADIUS * 2.5);
            AbyssVfx.of(OriginVfx.ATOMIC).at(c).scale((float) RADIUS).extra((float) ATOM_HEIGHT)
                .colors(VIOLET, VIOLET_HI).duration(TOTAL).send(level, RADIUS * 2.5);

            // Held in place and untouchable while casting and while the crater forms
            AtomicBlast.begin(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DETONATE + 200, 4, false, false, false));

            Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 4f, 0.5f);
            Fx.sound(level, c, SoundEvents.PORTAL_TRIGGER, 2f, 0.5f);

            // Gather: rising resonance; enemies in range are slowed
            for (int t = 8; t < SWING; t += 8) {
                final float pitch = 0.5f + 0.9f * t / SWING;
                Delayed.schedule(t, () -> {
                    if (!player.isAlive()) return;
                    Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, pitch);
                    Fx.sound(level, c, SoundEvents.BEACON_AMBIENT, 3f, pitch);
                    for (LivingEntity e : Targeting.enemiesAround(player, c, RADIUS))
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2), player);
                });
            }
            Delayed.schedule(SWING, () -> Fx.sound(level, c, SoundEvents.BELL_RESONATE, 3f, 1.4f));
            Delayed.schedule(CUT, () -> {
                Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.5f);
                Fx.sound(level, c, SoundEvents.GLASS_BREAK, 3f, 0.6f);
                Fx.sound(level, c, SoundEvents.TRIDENT_THUNDER.value(), 2f, 1.6f);
            });
            Delayed.schedule(DETONATE, () -> detonate(player, level, c));
            return true;
        }

        private static void detonate(ServerPlayer player, ServerLevel level, Vec3 c) {
            if (!player.isAlive() || player.level() != level) return;   // the cast fizzles

            AbyssCinematics.quake(level, c, (float) RADIUS, 60, 1.8f);
            Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 8f, 0.45f);
            Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 6f, 0.5f);
            Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 8f, 0.5f);
            Fx.sound(level, c, SoundEvents.DRAGON_FIREBALL_EXPLODE, 6f, 0.6f);
            Presence.crackRing(level, c, RADIUS * 0.15, 60);
            Presence.crackRing(level, c, RADIUS * 0.35, 120);
            Presence.crackRing(level, c, RADIUS * 0.6, 180);
            AtomicBlast.detonate(player, c);
            for (LivingEntity ally : Targeting.alliesAround(player, c, RADIUS))
                if (ally != player) ally.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0), player);

            int hit = 0;
            for (LivingEntity e : Targeting.enemiesAround(player, c, RADIUS)) {
                double d = e.position().distanceTo(c);
                float near = (float) Mth.clamp(1 - d / RADIUS, 0, 1);
                if (!e.hurt(Targeting.spell(player), Mth.lerp(near, MIN_DAMAGE, MAX_DAMAGE))) continue;
                Vec3 out = e.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : out.normalize();
                e.push(out.x * (0.6 + near * 1.6), 0.5 + near * 0.6, out.z * (0.6 + near * 1.6));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), player);
                hit++;
            }
            for (int i = 1; i <= 3; i++)
                Delayed.schedule(i * 14, () -> Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 4f, 0.35f));
            player.displayClientMessage(Component.translatable("abyssorigins.atomic.cast", hit)
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    // ---- Usurp --------------------------------------------------------------------------------

    /** Steals a target's beneficial effects and up to 30 of their origin resource. */
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
            AbyssVfx.beam(level, from, to, 0.35f, 0xC79BFF, Eminence.COLOR_HI, 14);
            AbyssVfx.burst(level, from, 0.9f, 0xC79BFF, Eminence.COLOR);
            AbyssVfx.helix(level, player, 2.2f, Eminence.COLOR, 0xC79BFF, 24);
            Fx.sound(level, target.position(), SoundEvents.EVOKER_CAST_SPELL, 1f, 0.7f);
            Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.6f);
            player.displayClientMessage(Component.translatable("abyssorigins.message.usurped", taken, Math.round(power))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return true;
        }
    }
}
