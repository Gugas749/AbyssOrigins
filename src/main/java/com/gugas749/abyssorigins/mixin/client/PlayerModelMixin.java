package com.gugas749.abyssorigins.mixin.client;

import com.gugas749.abyssorigins.client.Crucifixion;
import com.gugas749.abyssorigins.client.eminence.ScytheAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses a crucified player in the cross's chains ({@link Crucifixion}). The first-person hand
 * render calls setupAnim with every argument 0 - that call is left alone.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;
    @Shadow @Final public ModelPart leftPants;
    @Shadow @Final public ModelPart rightPants;
    @Shadow @Final public ModelPart jacket;

    private PlayerModelMixin(ModelPart root) { super(root); }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void abyssorigins$uncrouch(T entity, float limbSwing, float limbAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (age != 0f && Crucifixion.isBound(entity)) {
            this.crouching = false;
            this.riding = false;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void abyssorigins$crucify(T entity, float limbSwing, float limbAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (age == 0f) return;
        if (!Crucifixion.isBound(entity)) {
            // The Sovereign Scythe: carried on the shoulder, swung through its chain
            if (!ScytheAnimations.holds(entity)) return;
            ScytheAnimations.poseModel(this, entity, age);
        } else {
            Crucifixion.pose(this, age);
        }
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        jacket.copyFrom(body);
    }
}
