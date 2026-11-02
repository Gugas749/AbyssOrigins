package com.gugas749.abyssorigins.client.vfx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.function.Supplier;

/**
 * Render types for the VFX engine. All are depth-tested (effects sit behind terrain)
 * but never write depth (overlapping effects blend instead of clipping each other).
 *
 *  BAND  — additive plasma ribbons      (abyss_band shader)
 *  FIELD — additive energy membranes    (abyss_field shader)
 *  GLOW  — additive vertex-colour glows (vanilla position_color)
 *  VOID  — sorted translucent dark matter (abyss_void shader)
 */
public final class VfxRenderTypes {

    private static final int BUFFER = 1 << 18;

    public static final RenderType BAND  = make("band",  () -> VfxShaders.band,       RenderStateShard.LIGHTNING_TRANSPARENCY,   DefaultVertexFormat.POSITION_TEX_COLOR, false);
    public static final RenderType FIELD = make("field", () -> VfxShaders.field,      RenderStateShard.LIGHTNING_TRANSPARENCY,   DefaultVertexFormat.POSITION_TEX_COLOR, false);
    public static final RenderType GLOW  = make("glow",  GameRenderer::getPositionColorShader, RenderStateShard.LIGHTNING_TRANSPARENCY, DefaultVertexFormat.POSITION_COLOR, false);
    public static final RenderType VOID  = make("void",  () -> VfxShaders.voidMatter, RenderStateShard.TRANSLUCENT_TRANSPARENCY, DefaultVertexFormat.POSITION_TEX_COLOR, true);

    private VfxRenderTypes() {}

    private static RenderType make(String name, Supplier<ShaderInstance> shader,
                                   RenderStateShard.TransparencyStateShard transparency,
                                   VertexFormat format, boolean sort) {
        return RenderType.create("abyssorigins_vfx_" + name, format, VertexFormat.Mode.QUADS, BUFFER, false, sort,
            RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(shader))
                .setTransparencyState(transparency)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOutputState(RenderStateShard.PARTICLES_TARGET)
                .createCompositeState(false));
    }
}
