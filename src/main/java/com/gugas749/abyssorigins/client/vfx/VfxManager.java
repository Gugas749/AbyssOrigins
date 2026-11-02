package com.gugas749.abyssorigins.client.vfx;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.vfx.effects.LightEffects;
import com.gugas749.abyssorigins.client.vfx.effects.MotionEffects;
import com.gugas749.abyssorigins.client.vfx.effects.VoidEffects;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxSpec;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * Client VFX engine: owns live effects, ticks them, and renders them after particles.
 *
 * Uses its own fixed buffers per render type so effects can freely interleave
 * BAND / FIELD / GLOW / VOID geometry without flushing each other. Draw order:
 * VOID (dark matter) first, then the additive layers glow on top of it.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class VfxManager {

    private static final int MAX_EFFECTS = 320;
    private static final double MAX_DISTANCE_SQR = 128 * 128;
    private static final RenderType[] ORDER = {
        VfxRenderTypes.VOID, VfxRenderTypes.FIELD, VfxRenderTypes.BAND, VfxRenderTypes.GLOW
    };

    private static final List<VfxInstance> ACTIVE = new ArrayList<>();
    private static MultiBufferSource.BufferSource buffers;
    private static boolean reportedError;

    private VfxManager() {}

    // ---- Spawning -------------------------------------------------------------------

    public static void spawn(VfxSpec spec) {
        if (spec.has(Vfx.STOP)) {
            boolean byColor = spec.extra() == 1f;
            ACTIVE.removeIf(i -> i.spec().type() == spec.type() && i.spec().follow() == spec.follow()
                && (!byColor || i.spec().color() == spec.color()));
            return;
        }
        if (ACTIVE.size() >= MAX_EFFECTS) return;
        VfxInstance inst = create(spec);
        if (inst != null) ACTIVE.add(inst);
    }

    private static VfxInstance create(VfxSpec s) {
        return switch (s.type()) {
            case SHOCKWAVE -> new LightEffects.Shockwave(s);
            case DOME      -> new LightEffects.Dome(s);
            case PILLAR    -> new LightEffects.Pillar(s);
            case BURST     -> new LightEffects.Burst(s);
            case SIGIL     -> new LightEffects.Sigil(s);
            case HELIX     -> new LightEffects.Helix(s);
            case AURA      -> new LightEffects.Aura(s);
            case BEAM      -> new MotionEffects.Beam(s);
            case BOLT      -> new MotionEffects.Bolt(s);
            case SLASH     -> new MotionEffects.Slash(s);
            case TRAIL     -> new MotionEffects.Trail(s);
            case SPARKS    -> new MotionEffects.SparkBurst(s);
            case MARK      -> new MotionEffects.Mark(s);
            case WINGS     -> new MotionEffects.Wings(s);
            case VORTEX    -> new VoidEffects.Vortex(s);
            case TENDRILS  -> new VoidEffects.Tendrils(s);
            case RIFT      -> new VoidEffects.Rift(s);
        };
    }

    public static void clear() { ACTIVE.clear(); }

    public static int count() { return ACTIVE.size(); }

    // ---- Tick ---------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        if (mc.isPaused()) return;
        ACTIVE.removeIf(i -> !i.tick());
    }

    // ---- Render -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) return;
        if (!VfxShaders.ready()) return;

        Camera camera = event.getCamera();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource src = buffers();

        VfxCanvas canvas = new VfxCanvas(
            src.getBuffer(VfxRenderTypes.BAND), src.getBuffer(VfxRenderTypes.FIELD),
            src.getBuffer(VfxRenderTypes.GLOW), src.getBuffer(VfxRenderTypes.VOID),
            camera.getPosition(), camera.getLeftVector(), camera.getUpVector(), camera.getLookVector(), pt);

        Vec3 cam = camera.getPosition();
        for (VfxInstance inst : ACTIVE) {
            if (inst.spec().pos().distanceToSqr(cam) > MAX_DISTANCE_SQR && inst.spec().follow() < 0) continue;
            try {
                inst.render(canvas, pt);
            } catch (Exception e) {
                if (!reportedError) {
                    reportedError = true;
                    Abyssorigins.LOGGER.error("[AbyssOrigins] VFX render error in {}", inst.spec().type(), e);
                }
            }
        }
        for (RenderType type : ORDER) src.endBatch(type);
    }

    private static MultiBufferSource.BufferSource buffers() {
        if (buffers == null) {
            SequencedMap<RenderType, ByteBufferBuilder> fixed = new LinkedHashMap<>();
            for (RenderType type : ORDER) fixed.put(type, new ByteBufferBuilder(type.bufferSize()));
            buffers = MultiBufferSource.immediateWithBuffers(fixed, new ByteBufferBuilder(1 << 12));
        }
        return buffers;
    }
}
