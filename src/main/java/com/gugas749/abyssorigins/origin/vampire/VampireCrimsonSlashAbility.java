package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.power.Delayed;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Life Steal Slash (skill node "vampire_crimson_slash"): a red crescent
 * flies forward up to 12 blocks (stopping at walls). Everything it passes through loses 15% of its max health
 * (ignores armour; capped at {@link #MAX_DAMAGE} against huge creatures), and if it hit anything
 * the vampire heals 7% of their own max health.
 */
public class VampireCrimsonSlashAbility extends OriginAbility {
    public static final String ID = "vampire_crimson_slash";
    private static final double RANGE = 12, WIDTH = 1.6;
    private static final int TRAVEL = 8;                  // ticks the crescent takes to fly its distance
    private static final float TARGET_SHARE = 0.15f, HEAL_SHARE = 0.07f, MAX_DAMAGE = 40f;

    public VampireCrimsonSlashAbility() {
        super(ID, "abyssorigins.ability." + ID + ".name", "abyssorigins.ability." + ID + ".desc", 18, 30);
        icon("☽"); color(0xFFE0203A);
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(0, -0.35, 0).add(dir.scale(0.6));
        var wall = level.clip(new ClipContext(from, from.add(dir.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double dist = wall.getType() == HitResult.Type.MISS ? RANGE : Math.max(1.5, from.distanceTo(wall.getLocation()));

        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        AbyssVfx.of(VfxType.CRESCENT).at(from).dir(dir).scale((float) dist).extra(2.0f)
            .colors(Palette.BLOOD, Palette.BLOOD_HI).flags(player.getRandom().nextBoolean() ? AbyssVfx.REVERSE : 0).duration(TRAVEL + 8).send(level);
        AbyssVfx.of(VfxType.SLASH).at(from).dir(dir).scale(1.4f).extra(170).colors(Palette.BLOOD_DARK, Palette.BLOOD).duration(8).send(level);
        Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
        Fx.sound(level, player.position(), SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.8f, 1.5f);

        // The crescent moves forward over TRAVEL ticks, hitting each target only once
        Set<Integer> hit = new HashSet<>();
        boolean[] fed = { false };
        for (int i = 1; i <= TRAVEL; i++) {
            final double a = dist * ease((i - 1) / (double) TRAVEL), b = dist * ease(i / (double) TRAVEL);
            Delayed.schedule(i, () -> sweep(player, from, dir, a, b, hit, fed));
        }
        return true;
    }

    /** The same ease-out the client crescent uses over its first 60% of life. */
    private static double ease(double t) {
        double u = 1 - Math.min(1, t);
        return 1 - u * u * u;
    }

    private static void sweep(ServerPlayer player, Vec3 from, Vec3 dir, double a, double b, Set<Integer> hit, boolean[] fed) {
        if (!player.isAlive()) return;
        ServerLevel level = player.serverLevel();
        Vec3 p0 = from.add(dir.scale(a)), p1 = from.add(dir.scale(b));
        AABB box = new AABB(p0, p1).inflate(WIDTH);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> Targeting.isPrey(player, e))) {
            if (!hit.add(e.getId())) continue;
            float dmg = Math.min(MAX_DAMAGE, e.getMaxHealth() * TARGET_SHARE);
            e.invulnerableTime = 0;
            if (!e.hurt(Targeting.spell(player), dmg)) continue;
            AbyssVfx.sparks(level, Fx.chest(e), 1.2f, Palette.BLOOD, Palette.BLOOD_DARK);
            AbyssVfx.burst(level, Fx.chest(e), 0.6f, Palette.BLOOD_DARK, Palette.BLOOD_HI);
            Fx.sound(level, e.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.7f);
            if (!fed[0]) {
                fed[0] = true;
                player.heal(player.getMaxHealth() * HEAL_SHARE);
                // Blood ribbon VFX back to the vampire
                AbyssVfx.beam(level, Fx.chest(e), Fx.chest(player), 0.25f, Palette.BLOOD, Palette.BLOOD_HI, 10);
                AbyssVfx.helix(level, player, 1.6f, Palette.BLOOD, Palette.BLOOD_HI, 16);
                Fx.sound(level, player.position(), SoundEvents.GENERIC_DRINK, 0.6f, 0.7f);
            }
        }
    }
}
