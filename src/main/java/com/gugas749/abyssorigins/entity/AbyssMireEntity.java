package com.gugas749.abyssorigins.entity;

import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.origin.eminence.SovereignScythe;
import com.gugas749.abyssorigins.power.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A pool of black mud used to summon and put away the Sovereign Scythe.
 *
 * SUMMON: the pool opens beside the Eminence, the scythe rises out of it blade-first, spinning, with
 * tendrils, and is placed in their hand at {@link #DELIVER}. ABSORB: tendrils grab the scythe where it fell
 * (or from its owner's hand), drag it to the middle and pull it under. Either way the pool closes after.
 *
 * Visual only: the scythe's state is stored with its owner before the pool opens (ScytheData), so
 * nothing is lost if the pool is unloaded halfway - it isn't saved with the world.
 * Animations (geo/entity/abyss_mire.geo.json): boil (loop), summon, absorb.
 */
public class AbyssMireEntity extends Entity implements GeoEntity {

    public static final byte SUMMON = 0, ABSORB = 1;
    /** Ticks: the pool spreads, the scythe rises (RISE_START..RISE_END), is handed over, the pool closes. */
    public static final int OPEN = 8, RISE_START = 8, RISE_END = 36, DELIVER = 40, SUMMON_TOTAL = 56;
    /** Ticks: tendrils drag the scythe to the middle (PULL_*), then under (..SINK_END); the pool closes. */
    public static final int PULL_START = 6, PULL_END = 18, SINK_END = 40, ABSORB_TOTAL = 50;

    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(AbyssMireEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<ItemStack> SCYTHE = SynchedEntityData.defineId(AbyssMireEntity.class, EntityDataSerializers.ITEM_STACK);
    /** ABSORB: where the scythe starts, relative to the pool. */
    private static final EntityDataAccessor<Vector3f> START = SynchedEntityData.defineId(AbyssMireEntity.class, EntityDataSerializers.VECTOR3);
    /** ABSORB: how far from upright it starts (degrees; 90 = lying flat). */
    private static final EntityDataAccessor<Float> TILT = SynchedEntityData.defineId(AbyssMireEntity.class, EntityDataSerializers.FLOAT);

    private static final RawAnimation BOIL = RawAnimation.begin().thenLoop("boil");
    private static final RawAnimation RISE = RawAnimation.begin().thenPlayAndHold("summon");
    private static final RawAnimation PULL = RawAnimation.begin().thenPlayAndHold("absorb");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;

    public AbyssMireEntity(EntityType<? extends AbyssMireEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Opens a pool at {@code at} and raises {@code scythe} out of it for {@code p}. */
    public static void summon(ServerPlayer p, Vec3 at, ItemStack scythe) {
        ServerLevel level = p.serverLevel();
        AbyssMireEntity m = new AbyssMireEntity(ModEntities.ABYSS_MIRE.get(), level);
        m.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        m.owner = p.getUUID();
        m.entityData.set(MODE, SUMMON);
        m.entityData.set(SCYTHE, scythe.copyWithCount(1));
        level.addFreshEntity(m);
        Fx.sound(level, at, SoundEvents.MUD_PLACE, 1.4f, 0.5f);
        Fx.sound(level, at, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.2f, 0.6f);
        Fx.sound(level, at, SoundEvents.SCULK_CATALYST_BLOOM, 1f, 0.5f);
    }

    /** Opens a pool at {@code at} that drags {@code scythe} (starting at {@code from}, relative) under. */
    public static void absorb(ServerLevel level, UUID owner, Vec3 at, ItemStack scythe, Vec3 from, float yaw, float tilt) {
        AbyssMireEntity m = new AbyssMireEntity(ModEntities.ABYSS_MIRE.get(), level);
        m.moveTo(at.x, at.y, at.z, yaw, 0);
        m.owner = owner;
        m.entityData.set(MODE, ABSORB);
        m.entityData.set(SCYTHE, scythe.copyWithCount(1));
        m.entityData.set(START, new Vector3f((float) from.x, (float) from.y, (float) from.z));
        m.entityData.set(TILT, tilt);
        level.addFreshEntity(m);
        Fx.sound(level, at, SoundEvents.MUD_PLACE, 1.4f, 0.5f);
        Fx.sound(level, at, SoundEvents.HONEY_BLOCK_SLIDE, 1.2f, 0.5f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(MODE, SUMMON);
        b.define(SCYTHE, ItemStack.EMPTY);
        b.define(START, new Vector3f());
        b.define(TILT, 0f);
    }

    public byte mode()          { return entityData.get(MODE); }
    public ItemStack scythe()   { return entityData.get(SCYTHE); }
    public Vector3f start()     { return entityData.get(START); }
    public float tilt()         { return entityData.get(TILT); }
    public int total()          { return mode() == SUMMON ? SUMMON_TOTAL : ABSORB_TOTAL; }

    /** A pool still raising {@code p}'s scythe. */
    public boolean summoning(Player p) {
        return mode() == SUMMON && tickCount < DELIVER && SovereignScythe.ownedBy(owner, p);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        int t = tickCount;
        if (t >= total()) {
            discard();
            return;
        }
        boil(level, t);

        if (mode() == SUMMON) {
            if (t == RISE_START) Fx.sound(level, position(), SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.9f, 0.5f);
            if (t == DELIVER && !scythe().isEmpty()) {
                ServerPlayer p = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
                if (p != null && p.level() == level && p.distanceToSqr(this) < SovereignScythe.DELIVER_RANGE * SovereignScythe.DELIVER_RANGE)
                    SovereignScythe.deliver(p, scythe());
                // Delivered or not, the scythe leaves the pool; if undelivered it stays stored with its owner
                entityData.set(SCYTHE, ItemStack.EMPTY);
                Vec3 top = position().add(0, 1.4, 0);
                level.sendParticles(ParticleTypes.SQUID_INK, top.x, top.y, top.z, 8, 0.25, 0.6, 0.25, 0.03);
            }
        } else {
            if (t == PULL_START) Fx.sound(level, position(), SoundEvents.SLIME_SQUISH, 1.2f, 0.5f);
            if (t == PULL_END) Fx.sound(level, position(), SoundEvents.HONEY_DRINK, 1.4f, 0.4f);
            if (t == SINK_END) {
                Fx.sound(level, position(), SoundEvents.MUD_BREAK, 1.4f, 0.4f);
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.2, getZ(), 18, 0.4, 0.1, 0.4, 0.06);
            }
        }
    }

    /** Bubble, ink and smoke particles, plus a little gold dust. */
    private void boil(ServerLevel level, int t) {
        float open = Math.min(1f, t / (float) OPEN) * Math.min(1f, (total() - t) / 10f);
        double r = 1.15 * open;
        if (t % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * r;
                double x = getX() + Math.cos(a) * d, z = getZ() + Math.sin(a) * d;
                level.sendParticles(ParticleTypes.SQUID_INK, x, getY() + 0.1, z, 0, 0, 0.6, 0, 0.08);
                level.sendParticles(Fx.dust(0x0A0810, 1.4f), x, getY() + 0.15, z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        if (t % 5 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 2, r * 0.4, 0.05, r * 0.4, 0.01);
            if (random.nextInt(3) == 0)
                level.sendParticles(Fx.dust(Eminence.COLOR, 0.6f), getX(), getY() + 0.25, getZ(), 2, r * 0.4, 0.05, r * 0.4, 0);
        }
        if (t % 7 == 0) Fx.sound(level, position(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.9f, 0.4f + random.nextFloat() * 0.3f);
        if (t % 13 == 0) Fx.sound(level, position(), SoundEvents.LAVA_POP, 0.6f, 0.4f);
    }

    // ---- GeckoLib ----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "boil", 0, s -> s.setAndContinue(BOIL)));
        controllers.add(new AnimationController<>(this, "main", 0, s -> s.setAndContinue(mode() == SUMMON ? RISE : PULL)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ---- Entity overrides -------------------------------------------------------------------------------------

    @Override public boolean shouldBeSaved()    { return false; }
    @Override public boolean isPickable()       { return false; }
    @Override public boolean isPushable()       { return false; }
    @Override public boolean isPushedByFluid()  { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
