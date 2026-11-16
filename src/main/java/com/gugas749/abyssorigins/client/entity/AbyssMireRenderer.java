package com.gugas749.abyssorigins.client.entity;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.eminence.ScytheAnimations;
import com.gugas749.abyssorigins.entity.AbyssMireEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.gugas749.abyssorigins.entity.AbyssMireEntity.*;

/**
 * The mire pool (GeckoLib: geo/entity/abyss_mire) spreads open and closes again, and the Sovereign Scythe is
 * drawn rising out of it or being dragged under. The part of the scythe below the surface is hidden by the
 * ground itself.
 */
public class AbyssMireRenderer extends GeoEntityRenderer<AbyssMireEntity> {

    /** Scythe origin height when fully submerged / fully risen (its model spans ~-1.0..+1.8 blocks around it). */
    private static final float SUNK = -1.9f, RISEN = 1.1f, HELD = 0.9f;

    public AbyssMireRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "abyss_mire")));
        this.shadowRadius = 0f;
    }

    @Override
    public void render(AbyssMireEntity mire, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = mire.tickCount + partial;
        float open = smooth(t / OPEN) * smooth((mire.total() - t) / 10f);
        if (open > 0.01f) {
            pose.pushPose();
            pose.scale(open, 0.6f + 0.4f * open, open);
            super.render(mire, entityYaw, partial, pose, buffers, light);
            pose.popPose();
        }

        ItemStack scythe = mire.scythe();
        if (scythe.isEmpty()) return;
        pose.pushPose();
        float yaw = mire.getYRot();
        if (mire.mode() == SUMMON) {
            if (!placeRising(pose, t, yaw)) { pose.popPose(); return; }
        } else {
            if (!placeSinking(pose, t, yaw, mire.start(), mire.tilt())) { pose.popPose(); return; }
        }
        pose.scale(ScytheAnimations.SCALE, ScytheAnimations.SCALE, ScytheAnimations.SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(scythe, ItemDisplayContext.NONE, light,
            OverlayTexture.NO_OVERLAY, pose, buffers, mire.level(), mire.getId());
        pose.popPose();
    }

    /** Blade first out of the mud, spinning slower as it clears. */
    private static boolean placeRising(PoseStack pose, float t, float yaw) {
        if (t < RISE_START) return false;
        float p = Mth.clamp((t - RISE_START) / (RISE_END - RISE_START), 0, 1);
        float e = 1 - (1 - p) * (1 - p) * (1 - p);                  // ease out
        float y = Mth.lerp(e, SUNK, RISEN) + (p >= 1 ? Mth.sin((t - RISE_END) * 0.4f) * 0.04f : 0);
        float spin = (1 - e) * (1 - e) * 540f;
        pose.translate(0, y, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw - 90 + spin));
        return true;
    }

    /** Dragged from where it lay to the middle and pulled under, turning. */
    private static boolean placeSinking(PoseStack pose, float t, float yaw, Vector3f start, float tilt) {
        if (t > SINK_END + 1) return false;
        Vector3f at = new Vector3f(start);
        float lean = tilt, spin = 0;
        if (t >= PULL_START) {
            float p = smooth((t - PULL_START) / (PULL_END - PULL_START));
            at.lerp(new Vector3f(0, HELD, 0), p);
            lean = Mth.lerp(p, tilt, 0);
            if (p < 1) at.add(Mth.sin(t * 2.1f) * 0.03f * (1 - p), 0, Mth.cos(t * 1.7f) * 0.03f * (1 - p));
        }
        if (t >= PULL_END) {
            float p = Mth.clamp((t - PULL_END) / (SINK_END - PULL_END), 0, 1);
            at.y = Mth.lerp(p * p, HELD, SUNK);
            spin = p * p * 360f;
        }
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw - 90 + spin));
        pose.mulPose(Axis.ZP.rotationDegrees(lean));
        return true;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
