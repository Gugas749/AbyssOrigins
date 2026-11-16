package com.gugas749.abyssorigins.entity;

import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abysscore.api.cinematic.AbyssCinematics;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Seraph's Judgement: a giant sword of light (geo/entity/judgement_sword, source blockbench/entity/judgement_sword.bbmodel) that hangs high above the marked
 * ground, plunges point-first and stays embedded for a moment before dissolving.
 *
 * The entity itself never moves - it stands at the impact point and the renderer draws the blade at the
 * height its timeline says (JudgementSwordRenderer.height), so the fall looks the same on every client
 * whatever the network latency. Timeline (ticks): appear 0..{@link #DROP}, fall ..{@link #IMPACT},
 * embedded ..{@link #FADE}, dissolve ..{@link #TOTAL}. Not saved with the world.
 */
public class JudgementSwordEntity extends Entity implements GeoEntity {

    public static final int DROP = 12, IMPACT = 20, FADE = 64, TOTAL = 74;
    /** Blocks above the impact point where the blade appears. */
    public static final float HEIGHT = 26f;
    public static final double RADIUS = 5;
    private static final float DAMAGE = 14f;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;

    public JudgementSwordEntity(EntityType<? extends JudgementSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Marks {@code at} and calls the blade down onto it for {@code caster}. */
    public static void strike(ServerPlayer caster, Vec3 at) {
        ServerLevel level = caster.serverLevel();
        JudgementSwordEntity s = new JudgementSwordEntity(ModEntities.JUDGEMENT_SWORD.get(), level);
        s.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        s.owner = caster.getUUID();
        level.addFreshEntity(s);

        // Ground sigil and light pillar where the blade will land
        AbyssVfx.of(VfxType.SIGIL).at(at.add(0, 0.05, 0)).scale((float) RADIUS).extra(8).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(IMPACT + 14).send(level);
        AbyssVfx.of(VfxType.PILLAR).at(at).scale(HEIGHT + 6).extra(0.35f).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(IMPACT).send(level);
        Fx.sound(level, at, SoundEvents.BELL_BLOCK, 2.0f, 1.2f);
        Fx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.6f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ServerLevel level = (ServerLevel) level();
        if (tickCount == DROP) Fx.sound(level, position().add(0, 12, 0), SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.0f, 0.6f);
        if (tickCount == IMPACT) impact(level);
        if (tickCount == FADE) {
            AbyssVfx.burst(level, position().add(0, 3, 0), 1.6f, Palette.RADIANT, Palette.RADIANT_HI);
            AbyssVfx.of(VfxType.SPARKS).at(position().add(0, 2, 0)).scale(2.5f).colors(Palette.RADIANT, Palette.RADIANT_HI).send(level);
            Fx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.4f);
        }
        if (tickCount >= TOTAL) discard();
    }

    private void impact(ServerLevel level) {
        Vec3 c = position();
        if (level.getPlayerByUUID(owner) instanceof ServerPlayer caster) {
            for (LivingEntity e : Targeting.enemiesAround(caster, c, RADIUS)) {
                boolean undead = Targeting.isUndead(e);
                e.hurt(Targeting.spell(caster), undead ? DAMAGE * 2 : DAMAGE);
                if (undead) e.igniteForSeconds(6);
                Vec3 push = e.position().subtract(c).multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-4 ? Vec3.ZERO : push.normalize().scale(0.8);
                e.push(push.x, 0.7, push.z);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 30, 0), caster);
                AbyssVfx.burst(level, Fx.chest(e), 0.8f, Palette.RADIANT, Palette.RADIANT_HI);
            }
        }
        AbyssVfx.shockwave(level, c, (float) RADIUS + 1.5f, Palette.RADIANT, Palette.RADIANT_HI, 22);
        AbyssVfx.shockwave(level, c, (float) RADIUS * 0.5f, 0xFFE9A6, Palette.RADIANT_HI, 14);
        AbyssVfx.of(VfxType.BURST).at(c.add(0, 1, 0)).scale(2.6f).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(16).send(level);
        AbyssVfx.of(VfxType.PILLAR).at(c).scale(12).extra(1.6f).colors(Palette.RADIANT, Palette.RADIANT_HI).duration(26).send(level);
        AbyssVfx.of(VfxType.SPARKS).at(c.add(0, 0.5, 0)).scale(3f).dir(new Vec3(0, 0.3, 0)).colors(Palette.RADIANT, Palette.RADIANT_HI).send(level);
        AbyssCinematics.quake(level, c, 14f, 14, 0.7f);
        Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.7f);
        Fx.sound(level, c, SoundEvents.TRIDENT_THUNDER.value(), 1.6f, 1.1f);
        Fx.sound(level, c, SoundEvents.ANVIL_LAND, 1.2f, 0.5f);
    }

    /** The blade is drawn high above the entity: widen the culling box so it is not dropped off-screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, 0, 3).expandTowards(0, HEIGHT + 10, 0);
    }

    @Override public boolean isPickable()     { return false; }
    @Override public boolean isPushable()     { return false; }
    @Override public boolean shouldBeSaved()  { return false; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
