package com.gugas749.abyssorigins.client.entity;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.entity.DementorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Translucent, so the robe's hems fade out (the texture's alpha falls off at the ends). No shadow.
 */
public class DementorRenderer extends GeoEntityRenderer<DementorEntity> {

    public DementorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "dementor")));
        this.shadowRadius = 0f;
    }

    @Override
    public RenderType getRenderType(DementorEntity animatable, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected float getDeathMaxRotation(DementorEntity animatable) {
        return 0f;
    }
}
