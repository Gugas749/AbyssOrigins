package com.gugas749.abyssorigins.mixin.client;

import com.gugas749.abyssorigins.client.eminence.ScytheAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third-person Sovereign Scythe: right before the held item is drawn, its orientation is taken over by
 * {@link ScytheAnimations#thirdPerson} (shoulder rest, the chain's swings, the throw wind-up).
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {

    private ItemInHandLayerMixin(RenderLayerParent<T, M> parent) { super(parent); }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void abyssorigins$scythe(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, HumanoidArm arm,
                                     PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!ScytheAnimations.isScythe(stack) || !(getParentModel() instanceof HumanoidModel<?> model)) return;
        ScytheAnimations.thirdPerson(entity, arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm, arm == HumanoidArm.LEFT, pose);
    }
}
