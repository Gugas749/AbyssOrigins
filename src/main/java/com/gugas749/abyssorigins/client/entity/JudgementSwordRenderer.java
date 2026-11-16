package com.gugas749.abyssorigins.client.entity;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.entity.JudgementSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import static com.gugas749.abyssorigins.entity.JudgementSwordEntity.*;

/**
 * The Judgement blade: appears high above the mark, plunges point-first, sinks a little into the ground,
 * holds, then shrinks away into light. Model point is at its origin; drawn {@link #SCALE}x (~8 blocks tall).
 */
public class JudgementSwordRenderer extends GeoEntityRenderer<JudgementSwordEntity> {

    private static final float SCALE = 2.0f, EMBED = 1.4f;

    public JudgementSwordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "judgement_sword")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0f;
    }

    /** Height of the point above the impact spot at time {@code t} (ticks). */
    public static float height(float t) {
        if (t < DROP) return HEIGHT;
        if (t < IMPACT) {
            float p = (t - DROP) / (IMPACT - DROP);
            return HEIGHT * (1 - p * p) - EMBED * p * p;       // accelerating plunge
        }
        return -EMBED;
    }

    @Override
    public void render(JudgementSwordEntity sword, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = sword.tickCount + partial;
        float size = SCALE * Mth.clamp(t / 6f, 0.2f, 1f);                       // fade and scale in
        if (t > FADE) size *= 1 - Mth.clamp((t - FADE) / (TOTAL - FADE), 0, 1);
        if (size <= 0.01f) return;
        float wobble = t >= IMPACT && t < IMPACT + 6 ? Mth.sin((t - IMPACT) * 2.6f) * (1 - (t - IMPACT) / 6f) * 3f : 0;

        pose.pushPose();
        pose.translate(0, height(t), 0);
        pose.mulPose(Axis.ZP.rotationDegrees(wobble));
        pose.scale(size, size, size);
        super.render(sword, 0, partial, pose, buffers, 0xF000F0);             // always full-bright
        pose.popPose();
    }
}
