package com.gugas749.abyssorigins.power;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Target selection and friend/foe rules shared by every ability. */
public final class Targeting {

    private Targeting() {}

    /**
     * Valid victim for area abilities: hostile mobs, mobs targeting the caster, and
     * other players only when PvP is enabled and team rules allow it. Never pets
     * belonging to the caster, armour stands or the caster themselves.
     */
    public static boolean isEnemy(ServerPlayer caster, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == caster) return false;
        if (e instanceof ArmorStand) return false;
        if (e instanceof OwnableEntity own && caster.getUUID().equals(own.getOwnerUUID())) return false;
        if (e instanceof Player other) {
            return caster.serverLevel().getServer().isPvpAllowed() && caster.canHarmPlayer(other) && !other.isSpectator();
        }
        if (e instanceof Enemy) return true;
        return e instanceof Mob mob && mob.getTarget() == caster;
    }

    /**
     * Anything the caster may feed on or strike with a non-combat power: every living creature
     * (cows, pigs, villagers, monsters...) except the caster, their pets, armour stands and
     * players that PvP/team rules protect. Broader than {@link #isEnemy}, which only covers hostiles.
     */
    public static boolean isPrey(ServerPlayer caster, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == caster || e.isSpectator()) return false;
        if (e instanceof ArmorStand) return false;
        if (e instanceof OwnableEntity own && caster.getUUID().equals(own.getOwnerUUID())) return false;
        if (e instanceof Player other)
            return caster.serverLevel().getServer().isPvpAllowed() && caster.canHarmPlayer(other);
        return true;
    }

    public static List<LivingEntity> preyAround(ServerPlayer caster, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return caster.level().getEntitiesOfClass(LivingEntity.class, box,
            e -> isPrey(caster, e) && e.distanceToSqr(center) <= radius * radius);
    }

    /** Allies for support abilities: the caster's pets and players they cannot harm (or all players in PvE). */
    public static boolean isAlly(ServerPlayer caster, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return false;
        if (e == caster) return true;
        if (e instanceof OwnableEntity own && caster.getUUID().equals(own.getOwnerUUID())) return true;
        if (e instanceof Player other && !other.isSpectator()) {
            return !caster.serverLevel().getServer().isPvpAllowed() || !caster.canHarmPlayer(other)
                || caster.isAlliedTo(other);
        }
        return false;
    }

    public static boolean isUndead(Entity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    public static List<LivingEntity> enemiesAround(ServerPlayer caster, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return caster.level().getEntitiesOfClass(LivingEntity.class, box,
            e -> isEnemy(caster, e) && e.distanceToSqr(center) <= radius * radius);
    }

    public static List<LivingEntity> alliesAround(ServerPlayer caster, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return caster.level().getEntitiesOfClass(LivingEntity.class, box,
            e -> isAlly(caster, e) && e.distanceToSqr(center) <= radius * radius);
    }

    /** First living entity along the caster's line of sight, stopping at blocks. */
    public static Optional<LivingEntity> lookTarget(ServerPlayer caster, double range) {
        Vec3 eye  = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end  = eye.add(look.scale(range));

        BlockHitResult block = caster.level().clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDist = block.getType() == HitResult.Type.MISS ? range : eye.distanceTo(block.getLocation());

        AABB box = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestDist = maxDist;
        for (Entity e : caster.level().getEntities(caster, box, e -> e instanceof LivingEntity le && le.isAlive()
                && !(e instanceof ArmorStand) && !e.isSpectator())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceTo(hit.get());
            if (d <= bestDist) { bestDist = d; best = (LivingEntity) e; }
        }
        return Optional.ofNullable(best);
    }

    /** Point the caster is looking at (block hit or max range). */
    public static Vec3 lookPoint(ServerPlayer caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        BlockHitResult hit = caster.level().clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // ---- Damage helpers -----------------------------------------------------

    /** Ability damage attributed to the caster (mobs aggro, kills credit) without triggering melee hooks. */
    public static DamageSource spell(ServerPlayer caster) {
        return caster.damageSources().indirectMagic(caster, caster);
    }

    public static boolean isMelee(DamageSource source, ServerPlayer attacker) {
        return source.getDirectEntity() == attacker && source.getEntity() == attacker
            && source.is(DamageTypes.PLAYER_ATTACK);
    }

    public static boolean isProjectile(DamageSource source) {
        return source.getDirectEntity() != null && source.getDirectEntity() != source.getEntity();
    }

    /** True if {@code attacker} is behind {@code target} (more than ~100° from its facing). */
    public static boolean isBehind(Entity attacker, LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0, target.getYRot()).normalize();
        Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1);
        if (toAttacker.lengthSqr() < 1.0E-4) return false;
        return facing.dot(toAttacker.normalize()) < -0.2;
    }

    public static int lightAt(ServerPlayer p) {
        return p.level().getMaxLocalRawBrightness(p.blockPosition());
    }

    public static boolean inSunlight(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        long t = l.getDayTime() % 24000;
        boolean day = t < 12500 || t > 23500;
        return day && l.canSeeSky(p.blockPosition()) && !l.isRaining() && !l.isThundering();
    }
}
