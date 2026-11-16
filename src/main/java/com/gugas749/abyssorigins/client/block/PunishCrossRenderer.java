package com.gugas749.abyssorigins.client.block;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.block.PunishCrossBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** GeckoLib renderer for the punishment cross; the candle flames glow (punish_cross_glowmask.png). */
public class PunishCrossRenderer extends GeoBlockRenderer<PunishCrossBlockEntity> {

    public PunishCrossRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new DefaultedBlockGeoModel<>(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "punish_cross")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** The model is ~3.3 blocks wide and ~4.3 tall: don't cull it when only the beam is on screen. */
    @Override
    public AABB getRenderBoundingBox(PunishCrossBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 4, 0);
    }
}
