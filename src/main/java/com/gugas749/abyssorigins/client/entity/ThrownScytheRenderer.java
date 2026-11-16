package com.gugas749.abyssorigins.client.entity;

import com.gugas749.abyssorigins.client.eminence.ScytheAnimations;
import com.gugas749.abyssorigins.entity.ThrownScytheEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The thrown Sovereign Scythe: laid flat and spinning around its centre of mass. */
public class ThrownScytheRenderer extends EntityRenderer<ThrownScytheEntity> {

    /** Spin speed, degrees per tick. */
    private static final float SPIN = 46f;
    /** Rough centre of mass of the model (pixels) - the spin axis. */
    private static final float CX = -12f, CY = 18f;
    /** Bank of the spinning wheel, degrees. */
    private static final float TILT = 28f;

    public ThrownScytheRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(ThrownScytheEntity e, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = e.scythe();
        if (stack.isEmpty()) return;
        pose.pushPose();
        pose.translate(0, 0.25, 0);
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, e.xRotO, e.getXRot()) * 0.5f));
        pose.mulPose(Axis.ZP.rotationDegrees(TILT));                  // a tilted wheel reads from behind, not edge-on
        pose.mulPose(Axis.YP.rotationDegrees(-(e.tickCount + partial) * SPIN));
        pose.mulPose(Axis.XP.rotationDegrees(90));                     // shaft flat
        pose.scale(ScytheAnimations.SCALE, ScytheAnimations.SCALE, ScytheAnimations.SCALE);
        pose.translate(-CX / 16f, -CY / 16f, 0);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light,
            OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
        pose.popPose();
        super.render(e, entityYaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownScytheEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
