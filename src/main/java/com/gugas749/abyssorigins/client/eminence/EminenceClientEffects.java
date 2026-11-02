package com.gugas749.abyssorigins.client.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client side of the Presence: the earthquake camera tremor (on use only, at most 3s),
 * the kneeling player's shiver of fear (their own camera and their body as others see it),
 * and opening the kneel / resist screens.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class EminenceClientEffects {

    private static Vec3 quakeAt;
    private static float quakeRadius, quakeStrength;
    private static long quakeStart, quakeEnd;

    private static final Map<Integer, Long> KNEELING = new HashMap<>();   // entity id -> end (ms)
    private static final Set<Integer> PUSHED = new HashSet<>();

    private EminenceClientEffects() {}

    // ---- Packet entry points ----------------------------------------------------------------

    public static void quake(double x, double y, double z, float radius, int ticks, float strength) {
        quakeAt = new Vec3(x, y, z);
        quakeRadius = radius;
        quakeStrength = strength;
        quakeStart = Util.getMillis();
        quakeEnd = quakeStart + Math.min(ticks, 60) * 50L;     // never longer than three seconds
    }

    public static void kneel(int entityId, int ticks) {
        KNEELING.put(entityId, Util.getMillis() + ticks * 50L);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == entityId
                && (mc.screen instanceof PresenceChoiceScreen || mc.screen instanceof ResistScreen))
            mc.setScreen(null);
    }

    public static void openPrompt(String casterName, int ticks) {
        Minecraft.getInstance().setScreen(new PresenceChoiceScreen(casterName, ticks));
    }

    public static void openResist(long seed, int ticks) {
        Minecraft.getInstance().setScreen(new ResistScreen(seed, ticks));
    }

    // ---- Camera -------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = Util.getMillis();
        float t = now % 100_000L;
        float yaw = 0, pitch = 0, roll = 0;

        if (quakeAt != null && now < quakeEnd) {
            float life = 1f - (now - quakeStart) / (float) (quakeEnd - quakeStart);
            double dist = mc.player.position().distanceTo(quakeAt);
            float falloff = Mth.clamp(1f - (float) dist / (quakeRadius * 2f), 0f, 1f);
            float amp = 2.4f * quakeStrength * falloff * life * life;
            yaw   += amp * (Mth.sin(t * 0.061f) + 0.5f * Mth.sin(t * 0.173f));
            pitch += amp * (Mth.sin(t * 0.083f + 1.3f) + 0.5f * Mth.sin(t * 0.191f));
            roll  += amp * 0.7f * Mth.sin(t * 0.047f + 2.1f);
        } else {
            quakeAt = null;
        }

        if (mc.player.hasEffect(ModEffects.KNEELING)) {     // trembling with fear
            yaw   += 0.35f * Mth.sin(t * 0.047f) + 0.15f * Mth.sin(t * 0.113f);
            pitch += 0.30f * Mth.sin(t * 0.059f + 0.7f) + 0.12f * Mth.sin(t * 0.131f);
            roll  += 0.45f * Mth.sin(t * 0.037f + 1.9f);
        }

        if (yaw != 0 || pitch != 0 || roll != 0) {
            event.setYaw(event.getYaw() + yaw);
            event.setPitch(event.getPitch() + pitch);
            event.setRoll(event.getRoll() + roll);
        }
    }

    // ---- The kneeling body (seen by everyone, including yourself in third person) --------------

    @SubscribeEvent
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        int id = event.getEntity().getId();
        Long end = KNEELING.get(id);
        if (end == null || Util.getMillis() > end) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        PUSHED.add(id);
        float t = Util.getMillis() % 100_000L;
        float sx = 0.018f * Mth.sin(t * 0.09f) + 0.008f * Mth.sin(t * 0.23f);
        float sz = 0.018f * Mth.sin(t * 0.071f + 1.1f);
        pose.translate(sx, -0.22, sz);                        // sunk to the knees, shivering
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        if (PUSHED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (KNEELING.isEmpty()) return;
        long now = Util.getMillis();
        KNEELING.values().removeIf(end -> end < now);
    }

    public static void clear() {
        KNEELING.clear();
        PUSHED.clear();
        quakeAt = null;
    }
}
