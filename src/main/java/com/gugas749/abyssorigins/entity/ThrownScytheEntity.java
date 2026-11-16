package com.gugas749.abyssorigins.entity;

import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.origin.eminence.SovereignScythe;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * The Sovereign Scythe, thrown: it spins flat and damages every creature in its path (each is hit
 * once) without slowing. When it hits a block, a mire pool opens under it and pulls it under; Mire Call
 * brings it back from there.
 */
public class ThrownScytheEntity extends Projectile {

    /** Base damage of the throw, before enchantments (the scythe in hand hits for 11). */
    public static final float DAMAGE = 12f;
    private static final int MAX_LIFE = 100;

    private static final EntityDataAccessor<ItemStack> SCYTHE = SynchedEntityData.defineId(ThrownScytheEntity.class, EntityDataSerializers.ITEM_STACK);

    private final Set<Integer> struck = new HashSet<>();
    private boolean settled;

    public ThrownScytheEntity(EntityType<? extends ThrownScytheEntity> type, Level level) {
        super(type, level);
    }

    public static void launch(ServerPlayer p, ItemStack stack, float speed) {
        ThrownScytheEntity t = new ThrownScytheEntity(ModEntities.THROWN_SCYTHE.get(), p.level());
        t.setOwner(p);
        t.entityData.set(SCYTHE, stack.copyWithCount(1));
        Vec3 eye = p.getEyePosition();
        t.moveTo(eye.x, eye.y - 0.35, eye.z, p.getYRot(), 0);
        t.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, speed, 0.5f);
        p.level().addFreshEntity(t);
        Fx.sound(p.level(), p.position(), SoundEvents.TRIDENT_THROW.value(), 1.2f, 0.55f);
        Fx.sound(p.level(), p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.5f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SCYTHE, ItemStack.EMPTY);
    }

    public ItemStack scythe() { return entityData.get(SCYTHE); }

    public boolean ownedBy(Player p) { return getOwner() == p; }

    @Override
    public void tick() {
        super.tick();
        Vec3 m = getDeltaMovement();
        Vec3 from = position(), to = from.add(m);

        if (level() instanceof ServerLevel level) {
            if (!(getOwner() instanceof ServerPlayer owner) || owner.isRemoved()) {
                settled = true;                                  // owner left: the stored copy is kept
                discard();
                return;
            }
            if (tickCount > MAX_LIFE || getY() < level.getMinBuildHeight() - 8) {
                settled = true;
                SovereignScythe.store(owner, scythe());
                discard();
                return;
            }

            BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            for (Entity e : level.getEntities(this, getBoundingBox().expandTowards(m).inflate(1.2),
                    e -> e instanceof LivingEntity && !struck.contains(e.getId()) && Targeting.isPrey(owner, e))) {
                var box = e.getBoundingBox().inflate(0.9);
                if (box.contains(from) || box.clip(from, end).isPresent()) strike(level, owner, (LivingEntity) e);
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                setPos(block.getLocation());
                bite(level, owner, block);
                return;
            }
            if (tickCount % 2 == 0)
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 1, 0.2, 0.05, 0.2, 0.01);
        }

        setPos(to);
        float drag = isInWater() ? 0.8f : 0.99f;
        setDeltaMovement(m.scale(drag).add(0, -0.03, 0));
        double h = m.horizontalDistance();
        setYRot((float) (Mth.atan2(m.x, m.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(m.y, h) * Mth.RAD_TO_DEG));
    }

    private void strike(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        struck.add(target.getId());
        ItemStack stack = scythe();
        DamageSource source = damageSources().trident(this, owner);
        float dmg = EnchantmentHelper.modifyDamage(level, stack, target, source, DAMAGE);
        if (target.hurt(source, dmg)) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, stack);
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.5);
            target.push(push.x, 0.15, push.z);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), owner);
        }
        AbyssVfx.sparks(level, Fx.chest(target), 0.8f, 0x2A2236, Eminence.COLOR);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
        Fx.sound(level, target.position(), SoundEvents.TRIDENT_HIT, 1f, 0.6f);
    }

    /** Hit a block: a mire pool opens below it and absorbs it. */
    private void bite(ServerLevel level, ServerPlayer owner, BlockHitResult hit) {
        settled = true;
        ItemStack stack = scythe();
        SovereignScythe.store(owner, stack);
        Vec3 at = hit.getLocation();
        Vec3 ground = SovereignScythe.ground(level, at.add(0, 0.2, 0), 10);
        if (ground == null) ground = at;
        Fx.sound(level, at, SoundEvents.TRIDENT_HIT_GROUND, 1.2f, 0.6f);
        level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.05);
        AbyssMireEntity.absorb(level, owner.getUUID(), ground, stack, at.subtract(ground), getYRot(), 80f);
        discard();
    }

    /** Mire Call while it is in flight: it sinks where it is. */
    public void sinkHere() {
        if (settled || !(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer owner)) return;
        settled = true;
        SovereignScythe.store(owner, scythe());
        Vec3 ground = SovereignScythe.ground(level, position(), 10);
        if (ground == null) ground = position();
        AbyssMireEntity.absorb(level, owner.getUUID(), ground, scythe(), position().subtract(ground), getYRot(), 85f);
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        // Unloaded mid-flight (or anything else): keep its state with its master
        if (!settled && level() instanceof ServerLevel && getOwner() instanceof ServerPlayer owner)
            SovereignScythe.store(owner, scythe());
        settled = true;
        super.remove(reason);
    }

    @Override public boolean shouldBeSaved()                   { return false; }
    @Override public boolean isPickable()                      { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }
    @Override protected void addAdditionalSaveData(CompoundTag tag)  { super.addAdditionalSaveData(tag); }
}
