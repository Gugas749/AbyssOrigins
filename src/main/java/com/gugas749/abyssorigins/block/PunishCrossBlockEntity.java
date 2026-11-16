package com.gugas749.abyssorigins.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animates the cross: swaying chains and rags, flickering candle flames. */
public class PunishCrossBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PunishCrossBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PUNISH_CROSS_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, s -> s.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
