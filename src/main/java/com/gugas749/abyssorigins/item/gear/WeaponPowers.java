package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/** The six origin relics. Each power plays with its origin's own resource and identity. */
public final class WeaponPowers {

    private WeaponPowers() {}

    private static Vec3 flatLook(ServerPlayer p) { return p.getLookAngle().multiply(1, 0, 1).normalize(); }

    // ---- Human · Oathkeeper (longsword) ---------------------------------------------

    /** Riposte: a parry stance that negates the next blow and counter-strikes. Passive: Giant Slayer. */
    public static final class Oathkeeper extends WeaponPower {
        public Oathkeeper() { super("oathkeeper", 12); }

        @Override
        public boolean activate(ServerPlayer p, ItemStack stack) {
            PowerState.flag(p, "oath_parry", 30);
            Vfx.of(VfxType.DOME).follow(p).scale(1.3f).colors(Palette.HUMAN, Palette.HUMAN_HI).duration(30).send(p.serverLevel());
            Fx.sound(p.level(), p.position(), SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.6f);
            return true;
        }

        @Override
        public void onHurt(ServerPlayer p, LivingIncomingDamageEvent e) {
            if (!(e.getSource().getEntity() instanceof LivingEntity attacker) || attacker == p) return;
            if (!PowerState.consume(p, "oath_parry")) return;
            e.setCanceled(true);
            Vfx.stop(p.serverLevel(), p, VfxType.DOME);
            if (attacker.distanceToSqr(p) < 36) {
                attacker.hurt(Targeting.spell(p), 8f);
                attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), p);
                Vfx.of(VfxType.SLASH).at(Fx.chest(attacker)).dir(attacker.position().subtract(p.position())).scale(1.4f)
                    .extra(160).colors(Palette.HUMAN, Palette.HUMAN_HI).flags(Vfx.VERTICAL).duration(12).send(p.serverLevel());
            }
            Vfx.burst(p.serverLevel(), Fx.chest(p), 0.9f, Palette.HUMAN, Palette.HUMAN_HI);
            Fx.sound(p.level(), p.position(), SoundEvents.SHIELD_BLOCK, 1f, 0.8f);
            Fx.sound(p.level(), p.position(), SoundEvents.ANVIL_LAND, 0.4f, 1.8f);
        }

        @Override
        public void onHit(ServerPlayer p, LivingEntity target, LivingIncomingDamageEvent e) {
            if (target.getMaxHealth() > p.getMaxHealth()) e.setAmount(e.getAmount() * 1.25f);
        }
    }

    // ---- Vampire · Sanguine Scythe ------------------------------------------------------

    /** Crimson Reap: a wide sweeping harvest that bleeds and feeds. Passive: Sanguine Thirst. */
    public static final class SanguineScythe extends WeaponPower {
        public SanguineScythe() { super("sanguine_scythe", 10); }

        @Override
        public boolean activate(ServerPlayer p, ItemStack stack) {
            ServerLevel level = p.serverLevel();
            Vec3 look = flatLook(p);
            Vec3 mouth = p.getEyePosition().subtract(0, 0.3, 0);
            for (LivingEntity e : Targeting.enemiesAround(p, p.position(), 4.5)) {
                Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
                if (to.lengthSqr() > 0.01 && to.normalize().dot(look) < -0.2) continue;
                e.hurt(Targeting.spell(p), 7f);
                e.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80, 1), p);
                Skills.addResource(p, 4f);
                p.heal(1f);
                Vfx.beam(level, Fx.chest(e), mouth, 0.18f, Palette.BLOOD, Palette.BLOOD_HI, 12);
            }
            Vfx.of(VfxType.SLASH).at(p.position().add(0, 1.0, 0)).dir(look).scale(3.6f).extra(220)
                .colors(Palette.BLOOD, Palette.BLOOD_HI).duration(14).send(level);
            Fx.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
            Fx.sound(level, p.position(), SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1.4f);
            return true;
        }

        @Override
        public void onHit(ServerPlayer p, LivingEntity target, LivingIncomingDamageEvent e) {
            p.heal(e.getAmount() * 0.15f);
            Skills.addResource(p, 2f);
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0), p);
        }
    }

    // ---- Demon · Hellbrand Cleaver ---------------------------------------------------------

    /** Brimstone Slam: spend 20 Wrath to erupt the ground. Passive: Searing Edge. */
    public static final class HellbrandCleaver extends WeaponPower {
        public HellbrandCleaver() { super("hellbrand_cleaver", 12); }

        @Override
        public boolean activate(ServerPlayer p, ItemStack stack) {
            if (!Skills.spendResource(p, 20f)) {
                p.displayClientMessage(Component.translatable("abyssorigins.message.not_enough_resource",
                    Component.translatable("resource.abyssorigins.wrath"), 20), true);
                return false;
            }
            ServerLevel level = p.serverLevel();
            for (LivingEntity e : Targeting.enemiesAround(p, p.position(), 4.5)) {
                e.hurt(Targeting.spell(p), 8f);
                e.igniteForSeconds(5);
                e.push(0, 0.6, 0);
                e.hurtMarked = true;
            }
            Vfx.shockwave(level, p.position(), 4.5f, Palette.INFERNO, Palette.INFERNO_HI, 18);
            Vfx.burst(level, p.position().add(0, 0.8, 0), 1.2f, Palette.INFERNO, Palette.INFERNO_HI);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i + Math.PI / 4;
                Vfx.of(VfxType.PILLAR).at(p.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5)).scale(3f).extra(0.4f)
                    .colors(Palette.INFERNO, Palette.INFERNO_HI).duration(16).send(level);
            }
            Fx.sound(level, p.position(), SoundEvents.GENERIC_EXPLODE, 0.6f, 0.8f);
            Fx.sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1f, 0.5f);
            return true;
        }

        @Override
        public void onHit(ServerPlayer p, LivingEntity target, LivingIncomingDamageEvent e) {
            target.igniteForSeconds(4);
            Skills.addResource(p, 4f);
        }
    }

    // ---- Seraph · Dawnspear -------------------------------------------------------------------

    /** Lance of Dawn: a piercing line of light. Passive: Consecrated Tip. */
    public static final class Dawnspear extends WeaponPower {
        private static final double RANGE = 16;

        public Dawnspear() { super("dawnspear", 8); }

        @Override
        public boolean activate(ServerPlayer p, ItemStack stack) {
            ServerLevel level = p.serverLevel();
            Vec3 eye = p.getEyePosition();
            Vec3 end = Targeting.lookPoint(p, RANGE);
            List<LivingEntity> hit = Targeting.enemiesAround(p, eye.lerp(end, 0.5), RANGE / 2 + 2).stream()
                .filter(e -> e.getBoundingBox().inflate(0.6).clip(eye, end).isPresent()).toList();
            for (LivingEntity e : hit) {
                boolean undead = Targeting.isUndead(e);
                e.hurt(Targeting.spell(p), undead ? 12f : 8f);
                if (undead) e.igniteForSeconds(4);
                Vfx.burst(level, Fx.chest(e), 0.8f, Palette.RADIANT, Palette.RADIANT_HI);
            }
            Vfx.beam(level, Fx.hand(p), end, 0.55f, Palette.RADIANT, Palette.RADIANT_HI, 14);
            Fx.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 2f);
            Fx.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
            return true;
        }

        @Override
        public void onHit(ServerPlayer p, LivingEntity target, LivingIncomingDamageEvent e) {
            if (Targeting.isUndead(target)) {
                e.setAmount(e.getAmount() * 1.4f);
                target.igniteForSeconds(3);
            }
            Skills.addResource(p, 2f);
        }
    }

    // ---- Voidborn · Event Horizon (greatsword) ------------------------------------------------

    /** Gravity Well: drag every foe into a collapsing point. Passive: Entropic Edge. */
    public static final class EventHorizon extends WeaponPower {
        public EventHorizon() { super("event_horizon", 14); }

        @Override
        public boolean activate(ServerPlayer p, ItemStack stack) {
            ServerLevel level = p.serverLevel();
            Vec3 center = p.position().add(flatLook(p).scale(2.5)).add(0, 1, 0);
            for (LivingEntity e : Targeting.enemiesAround(p, center, 7)) {
                Vec3 pull = center.subtract(e.position());
                double d = pull.length();
                if (d > 0.1) e.setDeltaMovement(pull.scale(Math.min(1.3, d * 0.28) / d).add(0, 0.2, 0));
                e.hurtMarked = true;
                e.hurt(Targeting.spell(p), 5f);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), p);
                e.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 100, 0), p);
            }
            Vfx.of(VfxType.VORTEX).at(center).scale(3.5f).colors(Palette.VOID, Palette.VOID_DARK).duration(20).send(level);
            Vfx.shockwave(level, center.subtract(0, 1, 0), 7, Palette.VOID, Palette.VOID_HI, 16);
            Fx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.6f);
            Fx.sound(level, center, SoundEvents.PORTAL_TRIGGER, 0.4f, 1.8f);
            return true;
        }

        @Override
        public void onHit(ServerPlayer p, LivingEntity target, LivingIncomingDamageEvent e) {
            MobEffectInstance cur = target.getEffect(ModEffects.SUNDERED);
            int amp = cur == null ? 0 : Math.min(2, cur.getAmplifier() + 1);
            target.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 80, amp), p);
        }
    }

    // ---- Shade · Shadowfang (dagger) ------------------------------------------------------------

    /** Nightstep: dash through the shadows and vanish. Passive: Unseen Fang. */
    public static final class Shadowfang extends WeaponPower {
        public Shadowfang() { super("shadowfang", 8); }

        @Override
        public boolean activate(ServerPlayer p, ItemStack stack) {
            ServerLevel level = p.serverLevel();
            Vec3 look = p.getLookAngle();
            Vec3 from = p.position();
            Vec3 dest = null;
            for (double back : new double[]{ 0.6, 1.4, 2.2 }) {
                Vec3 cand = Targeting.lookPoint(p, 7).subtract(look.scale(back)).subtract(0, p.getEyeHeight(), 0);
                if (level.noCollision(p, p.getBoundingBox().move(cand.subtract(from)))) { dest = cand; break; }
            }
            if (dest == null) {
                p.displayClientMessage(Component.translatable("abyssorigins.message.void_step_blocked"), true);
                return false;
            }
            p.teleportTo(dest.x, dest.y, dest.z);
            p.fallDistance = 0;
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
            PowerState.flag(p, "fang_empower", 60);

            Vfx.of(VfxType.RIFT).at(from.add(0, 1, 0)).dir(look).scale(2.2f).colors(Palette.SHADOW_HI, 0xE0E8FF).duration(12).send(level);
            Vfx.of(VfxType.BEAM).at(from.add(0, 1, 0)).to(dest.add(0, 1, 0)).extra(0.3f)
                .colors(Palette.SHADOW, Palette.SHADOW_HI).flags(Vfx.VOID).duration(10).send(level);
            Vfx.of(VfxType.BURST).at(dest.add(0, 1, 0)).scale(0.8f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(Vfx.VOID).duration(12).send(level);
            Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);
            return true;
        }

        @Override
        public void onHit(ServerPlayer p, LivingEntity target, LivingIncomingDamageEvent e) {
            boolean empowered = PowerState.consume(p, "fang_empower");
            if (empowered || Targeting.isBehind(p, target) || p.hasEffect(MobEffects.INVISIBILITY)) {
                e.setAmount(e.getAmount() * 1.8f);
                Vfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(target.position().subtract(p.position())).scale(1.1f).extra(130)
                    .colors(Palette.SHADOW_HI, 0xE0E8FF).flags(Vfx.VERTICAL).duration(10).send(p.serverLevel());
            }
            Skills.addResource(p, 3f);
        }
    }
}
