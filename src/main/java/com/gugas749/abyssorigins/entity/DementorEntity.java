package com.gugas749.abyssorigins.entity;

import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * Dementor - the hidden power {@code hidden_dementor}'s summon. A hooded flying entity that moves
 * through walls and targets every player except its summoner.
 *
 * Behaviour:
 *  - spawns out of the ground with a shriek (brief darkness), then targets the nearest player (or the one its owner points at);
 *  - players near it get the Dread vignette, a frost overlay, a heartbeat sound (heard only by them) and
 *    action-bar whispers; water under it freezes;
 *  - in melee range it holds the victim in front of it and drains them (the Kiss): they can't move or
 *    turn, soul particles stream from them into its mouth, and they lose health and hunger;
 *  - between drains it circles the victim and plays head-twitch animations.
 * It sinks into the ground when its time runs out, its owner leaves, it is dismissed, or it takes lethal damage.
 * One without an owner (e.g. from {@code /summon}) targets every player until its time runs out.
 *
 * Animations (geo/entity/dementor.geo.json): idle, move, drain, spawn, despawn on the "main" controller,
 * twitch and shriek triggered on "fx".
 */
public class DementorEntity extends PathfinderMob implements GeoEntity {

    public static final int LIFETIME = 45 * 20, RISE_TICKS = 50, FADE_TICKS = 32;
    public static final int KISS_TICKS = 80, KISS_COOLDOWN = 100;
    public static final double HUNT_RANGE = 40, AURA_RANGE = 14;
    /**
     * How far in front of its victim it hovers while feeding. The drain pose lunges the hood and arms
     * forward, so this keeps the victim's camera just outside the hood: they look straight into it.
     */
    public static final double KISS_REACH = 2.0;
    /** Feeding pose (drain animation): its mouth is ~0.95 blocks ahead of it and 2.15 up. */
    private static final double MOUTH_AHEAD = 0.95, MOUTH_UP = 2.15, KISS_DROP = -0.5;

    public static final byte RISING = 0, HUNTING = 1, KISSING = 2, FADING = 3;
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(DementorEntity.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE    = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation MOVE    = RawAnimation.begin().thenLoop("move");
    private static final RawAnimation DRAIN   = RawAnimation.begin().thenLoop("drain");
    private static final RawAnimation SPAWN   = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation DESPAWN = RawAnimation.begin().thenPlayAndHold("despawn");
    private static final RawAnimation TWITCH  = RawAnimation.begin().thenPlay("twitch");
    private static final RawAnimation SHRIEK  = RawAnimation.begin().thenPlay("shriek");

    private static final String[] WHISPERS = { "1", "2", "3", "4", "5", "6", "7", "8" };

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable private UUID owner;
    @Nullable private UUID quarry;         // player the owner pointed at; targeted first
    @Nullable private UUID prey;           // current target (a new target triggers a shriek)
    @Nullable private UUID kissing;
    private int age, stateTicks, kissCooldown, nextTwitch = 60, nextBreath = 40, nextWhisper = 100;
    private final float orbitPhase;
    private Vec3 home = Vec3.ZERO;

    public DementorEntity(EntityType<? extends DementorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.xpReward = 0;
        this.orbitPhase = level.random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 60)
            .add(Attributes.ARMOR, 6)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FOLLOW_RANGE, 48)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Called once by the summoning power, before the entity is added to the level. */
    public void bind(ServerPlayer master, @Nullable ServerPlayer target) {
        this.owner = master.getUUID();
        this.quarry = target == null ? null : target.getUUID();
    }

    @Nullable public UUID ownerId() { return owner; }

    /** The owner points at a new victim: all of its Dementors retarget. */
    public void hunt(ServerPlayer target) {
        if (state() == FADING) return;
        quarry = target.getUUID();
        if (state() == KISSING) endKiss();
    }

    /** Sink back into the ground and vanish. */
    public void fade() {
        if (state() == FADING) return;
        if (state() == KISSING) endKiss();
        setState(FADING);
        if (level() instanceof ServerLevel sl) {
            Fx.sound(sl, position(), SoundEvents.SOUL_ESCAPE, 1.5f, 0.5f);
            Fx.sound(sl, position(), SoundEvents.PHANTOM_DEATH, 0.8f, 0.4f);
        }
    }

    public byte state()            { return entityData.get(STATE); }
    private void setState(byte s)  { entityData.set(STATE, s); stateTicks = 0; }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (home == Vec3.ZERO) home = position();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, RISING);
    }

    // ---- Tick -----------------------------------------------------------------------------------

    @Override
    public void tick() {
        noPhysics = true;                  // passes through walls, like a vex
        super.tick();
        noPhysics = false;
        setNoGravity(true);
        if (level().isClientSide) { clientFx(); return; }

        ServerLevel level = (ServerLevel) level();
        age++;
        stateTicks++;
        if (kissCooldown > 0) kissCooldown--;

        ServerPlayer master = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        boolean masterGone = owner != null && (master == null || !master.isAlive() || master.level() != level);
        if (state() != FADING && (age >= LIFETIME || masterGone))
            fade();

        switch (state()) {
            case RISING -> {
                setDeltaMovement(Vec3.ZERO);
                if (stateTicks == 4) Fx.sound(level, position(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.2f, 0.6f);
                if (stateTicks == RISE_TICKS - 14) shriek();
                if (stateTicks % 3 == 0)
                    level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.2, getZ(), 3, 0.6, 0.1, 0.6, 0.02);
                if (stateTicks >= RISE_TICKS) setState(HUNTING);
            }
            case HUNTING -> hunt(level, master);
            case KISSING -> kiss(level, master);
            case FADING -> {
                setDeltaMovement(Vec3.ZERO);
                if (stateTicks % 2 == 0)
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2, getZ(), 4, 0.4, 0.8, 0.4, 0.02);
                if (stateTicks >= FADE_TICKS) discard();
                return;
            }
            default -> {}
        }
        aura(level);
        haunt(level);
    }

    private void hunt(ServerLevel level, @Nullable ServerPlayer master) {
        ServerPlayer target = findTarget(level);
        UUID id = target == null ? null : target.getUUID();
        if (id != null && !id.equals(prey)) shriek();       // shriek on a new target
        prey = id;

        Vec3 goal;
        double speed;
        if (target == null) {
            // No victim: drift in a slow circle around the master (around its spawn point if unowned)
            double a = orbitPhase + age * 0.015;
            Vec3 centre = master != null ? master.position() : home;
            goal = centre.add(Math.cos(a) * 5, 0.4, Math.sin(a) * 5);
            speed = 0.14;
        } else {
            Vec3 away = horizontal(position().subtract(target.position()));
            boolean canKiss = kissCooldown == 0 && !isBeingKissed(level, target);
            if (canKiss) {
                // Hover with the hood level with the victim's head
                goal = target.position().add(away.scale(KISS_REACH)).add(0, KISS_DROP, 0);
                speed = distanceTo(target) > 10 ? 0.3 : 0.17;
            } else {
                // Another Dementor is draining, or this one just did: circle and wait
                double a = Math.atan2(away.z, away.x) + 0.035;
                goal = target.position().add(Math.cos(a) * 3.2, -0.3, Math.sin(a) * 3.2);
                speed = 0.16;
            }
            if (canKiss && getEyePosition().distanceTo(target.getEyePosition()) < KISS_REACH + 0.6) startKiss(level, target);
        }
        steer(goal, speed);
        face(target != null ? target.getEyePosition() : goal.add(getDeltaMovement().scale(10)));
    }

    private void startKiss(ServerLevel level, ServerPlayer victim) {
        kissing = victim.getUUID();
        setState(KISSING);
        Fx.sound(level, position(), SoundEvents.WARDEN_SONIC_CHARGE, 1.2f, 0.45f);
        victim.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5f, 1.3f);
        victim.displayClientMessage(Component.translatable("abyssorigins.message.dementor_kiss"), true);
    }

    private void endKiss() {
        kissing = null;
        kissCooldown = KISS_COOLDOWN;
        if (state() == KISSING) setState(HUNTING);
    }

    private void kiss(ServerLevel level, @Nullable ServerPlayer master) {
        ServerPlayer victim = kissing == null ? null : level.getServer().getPlayerList().getPlayer(kissing);
        if (victim == null || !valid(victim) || stateTicks > KISS_TICKS || distanceTo(victim) > 4) {
            if (victim != null && victim.isAlive())
                victim.displayClientMessage(Component.translatable("abyssorigins.message.dementor_release"), true);
            endKiss();
            return;
        }
        // Hold the face in front of the hood
        Vec3 away = horizontal(position().subtract(victim.position()));
        steer(victim.position().add(away.scale(KISS_REACH)).add(0, KISS_DROP, 0), 0.25);
        face(victim.getEyePosition());
        Vec3 mouth = mouth();

        // Lock the victim's movement and view
        victim.addEffect(new MobEffectInstance(ModEffects.STUNNED, 10, 0, false, false, false));
        victim.addEffect(new MobEffectInstance(ModEffects.DREAD, 60, 2, false, false, true));
        if (stateTicks % 4 == 0) victim.lookAt(EntityAnchorArgument.Anchor.EYES, mouth);

        // Soul particles from the victim's chest to its mouth (below their eyes, so the
        // particles rise through the victim's view instead of sitting on their camera)
        Vec3 from = victim.position().add(0, 1.0, 0).add(horizontal(position().subtract(victim.position())).scale(0.4));
        if (stateTicks % 2 == 0)
            for (int i = 0; i < 2; i++) {
                double t = ((stateTicks * 0.06) + i / 2.0) % 1.0;
                Vec3 p = from.lerp(mouth, t);
                level.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            }
        if (stateTicks % 20 == 10) {
            DamageSource src = master != null ? damageSources().indirectMagic(this, master) : damageSources().magic();
            victim.hurt(src, 1.5f);
            victim.causeFoodExhaustion(3f);
            victim.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.6f, 1.4f);
            Fx.sound(level, mouth, SoundEvents.SOUL_ESCAPE, 1.0f, 0.5f + random.nextFloat() * 0.2f);
        }
        if (stateTicks == KISS_TICKS / 2)
            victim.displayClientMessage(whisper(), true);
    }

    // ---- Nearby player effects -----------------------------------------------------------------

    /** Dread, frost and heartbeat for every nearby player except its owner. */
    private void aura(ServerLevel level) {
        if (age % 10 != 0) return;
        for (ServerPlayer p : players(level, AURA_RANGE)) {
            double d = distanceTo(p);
            int dread = d < 5 ? 2 : d < 9 ? 1 : 0;
            p.addEffect(new MobEffectInstance(ModEffects.DREAD, 40, dread, false, false, true));
            if (d < 9 && p.canFreeze())                    // frost overlay, capped below full freeze
                p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() - 10, p.getTicksFrozen() + (d < 4 ? 40 : 28)));
            int beat = d < 4 ? 10 : d < 8 ? 20 : 30;
            if (age % beat == 0)
                p.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, (float) Mth.clampedMap(d, 2, 14, 1.4, 0.4), 1.0f);
        }
        freezeWater(level);
    }

    /** Breath, twitches and whispers. */
    private void haunt(ServerLevel level) {
        if (--nextBreath <= 0) {
            nextBreath = 70 + random.nextInt(60);
            Fx.sound(level, getEyePosition(), SoundEvents.PHANTOM_AMBIENT, 1.2f, 0.35f + random.nextFloat() * 0.1f);
            Fx.sound(level, getEyePosition(), SoundEvents.SOUL_ESCAPE, 1.2f, 0.55f);
        }
        if (state() != RISING && --nextTwitch <= 0) {
            nextTwitch = 60 + random.nextInt(140);
            triggerAnim("fx", "twitch");
            Fx.sound(level, getEyePosition(), SoundEvents.BONE_BLOCK_BREAK, 1.0f, 0.45f);
        }
        if (--nextWhisper <= 0) {
            nextWhisper = 120 + random.nextInt(140);
            List<ServerPlayer> near = players(level, 16);
            if (!near.isEmpty()) near.get(random.nextInt(near.size())).displayClientMessage(whisper(), true);
        }
    }

    private void shriek() {
        triggerAnim("fx", "shriek");
        if (level() instanceof ServerLevel sl) {
            for (ServerPlayer p : players(sl, AURA_RANGE))
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
            Fx.sound(sl, getEyePosition(), SoundEvents.GHAST_SCREAM, 1.6f, 0.45f);
            Fx.sound(sl, getEyePosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.2f, 0.7f);
        }
    }

    private void freezeWater(ServerLevel level) {
        BlockPos base = blockPosition();
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -3, -2), base.offset(2, 0, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.WATER) && s.getFluidState().isSource() && level.getBlockState(p.above()).isAir()
                    && level.isUnobstructed(ice, p, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                level.setBlockAndUpdate(p, ice);
                level.scheduleTick(p, Blocks.FROSTED_ICE, Mth.nextInt(random, 60, 120));
            }
        }
    }

    private Component whisper() {
        return Component.translatable("abyssorigins.dementor.whisper." + WHISPERS[random.nextInt(WHISPERS.length)])
            .withStyle(s -> s.withColor(0x8FA3B8).withItalic(true));
    }

    // ---- Targets --------------------------------------------------------------------------------

    @Nullable
    private ServerPlayer findTarget(ServerLevel level) {
        if (quarry != null) {
            ServerPlayer q = level.getServer().getPlayerList().getPlayer(quarry);
            if (q != null && valid(q) && distanceTo(q) < HUNT_RANGE * 2) return q;
            quarry = null;
        }
        if (prey != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(prey);
            if (p != null && valid(p) && distanceTo(p) < HUNT_RANGE + 8) return p;
        }
        ServerPlayer best = null;
        double bestD = HUNT_RANGE;
        for (ServerPlayer p : players(level, HUNT_RANGE)) {
            double d = distanceTo(p);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private List<ServerPlayer> players(ServerLevel level, double range) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(position(), position()).inflate(range),
            p -> valid(p) && distanceTo(p) <= range);
    }

    /** Everyone but its owner, and never players who can't be hurt anyway. */
    private boolean valid(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative() && p.level() == level() && !p.getUUID().equals(owner);
    }

    private boolean isBeingKissed(ServerLevel level, ServerPlayer p) {
        return !level.getEntitiesOfClass(DementorEntity.class, p.getBoundingBox().inflate(6),
            d -> d != this && p.getUUID().equals(d.kissing)).isEmpty();
    }

    // ---- Movement -------------------------------------------------------------------------------

    private void steer(Vec3 goal, double speed) {
        Vec3 d = goal.subtract(position());
        double len = d.length();
        setDeltaMovement(len < 0.04 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len));
    }

    private void face(Vec3 at) {
        Vec3 d = at.subtract(getEyePosition());
        if (d.horizontalDistanceSqr() < 1.0E-4) return;
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        setXRot((float) (-Mth.atan2(d.y, Math.sqrt(d.horizontalDistanceSqr())) * Mth.RAD_TO_DEG));
    }

    private static Vec3 horizontal(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
    }

    /** Where its mouth is while it feeds: inside the front of the lunging hood. */
    private Vec3 mouth() {
        return position().add(Vec3.directionFromRotation(0, getYRot()).scale(MOUTH_AHEAD)).add(0, MOUTH_UP, 0);
    }

    // ---- Client ---------------------------------------------------------------------------------

    private void clientFx() {
        if (state() == FADING) return;
        // Black mist and snowflake particles from the hem
        double r = 0.55;
        if (tickCount % 2 == 0) level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * r * 2,
            getY() + 0.1 + random.nextDouble() * 0.4, getZ() + (random.nextDouble() - 0.5) * r * 2, 0, 0.01, 0);
        if (random.nextInt(8) == 0)
            level().addParticle(ParticleTypes.SQUID_INK, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 1.2,
                getZ() + (random.nextDouble() - 0.5), 0, -0.02, 0);
        if (random.nextInt(6) == 0)
            level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 2.8,
                getZ() + (random.nextDouble() - 0.5) * 2.5, 0, -0.01, 0);
    }

    /** True while it is gliding (the client sees interpolated positions, not the server's motion). */
    private boolean gliding() {
        double dx = getX() - xo, dz = getZ() - zo, dy = getY() - yo;
        return dx * dx + dy * dy + dz * dz > 0.0009;
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, s -> switch (state()) {
            case RISING  -> s.setAndContinue(SPAWN);
            case FADING  -> s.setAndContinue(DESPAWN);
            case KISSING -> s.setAndContinue(DRAIN);
            default      -> s.setAndContinue(gliding() ? MOVE : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "fx", 2, s -> PlayState.STOP)
            .triggerableAnim("twitch", TWITCH)
            .triggerableAnim("shriek", SHRIEK));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ---- Vanilla overrides ----------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (state() == RISING || state() == FADING) return false;
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && owner != null && owner.equals(attacker.getUUID())) return false;
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && amount >= getHealth()) {
            // Lethal damage despawns it instead of killing it
            super.hurt(source, getHealth() - 1);
            fade();
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override public boolean isPushable()                 { return false; }
    @Override protected void doPush(Entity entity)        {}
    @Override public boolean removeWhenFarAway(double d)  { return false; }
    @Override public boolean shouldDespawnInPeaceful()    { return false; }
    @Override protected boolean shouldDropLoot()          { return false; }
    @Override public boolean canFreeze()                  { return false; }
    @Override public boolean isSensitiveToWater()         { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override public float getVoicePitch()                { return 0.4f + random.nextFloat() * 0.15f; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PHANTOM_HURT; }
    @Override protected SoundEvent getDeathSound()        { return SoundEvents.PHANTOM_DEATH; }
    @Override protected SoundEvent getAmbientSound()      { return null; }      // haunt() does its breathing

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        if (quarry != null) tag.putUUID("Quarry", quarry);
        tag.putInt("Age", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
        age = tag.getInt("Age");
        if (state() == RISING) setState(HUNTING);       // don't rise twice after a reload
    }
}
