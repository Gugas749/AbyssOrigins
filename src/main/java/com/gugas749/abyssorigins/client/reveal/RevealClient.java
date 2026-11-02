package com.gugas749.abyssorigins.client.reveal;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.client.ClientIdentities;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.ClientTimers;
import com.gugas749.abyssorigins.origin.vampire.VampireBlood;
import com.gugas749.abyssorigins.specialization.occultist.OccultVisionAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-local entity outlines ("reveals").
 *
 * Decides, for THIS client only, which entities should render with a glowing outline
 * and in which colour. Nothing here touches server state or other players' clients —
 * this is what makes Occult Vision a private sight instead of a server-wide Glowing.
 *
 * Sources:
 *  - Occult Vision:   other players coloured by origin (+ hostiles with Ascended)
 *  - Predator Senses: blood-bearing creatures while a vampire crouches
 *  - RevealPacket:    server-issued marks (Hunter's Mark, Mark for Death)
 *
 * Consumed by MinecraftMixin (shouldEntityAppearGlowing) and EntityMixin (getTeamColor).
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class RevealClient {

    private static final int HOSTILE_RGB = 0xFF4646;
    private static final int BLOOD_RGB   = 0xD0203A;

    /** entityId → rgb, rebuilt every client tick. */
    private static final Map<Integer, Integer> ACTIVE = new HashMap<>();
    /** entityId → {remainingTicks, rgb} from RevealPacket. */
    private static final Map<Integer, int[]> MARKS = new HashMap<>();

    private RevealClient() {}

    public static void mark(int entityId, int ticks, int rgb) {
        MARKS.put(entityId, new int[]{ ticks, rgb });
    }

    public static boolean shouldReveal(Entity e) { return ACTIVE.containsKey(e.getId()); }

    /** @return outline rgb, or -1 if this entity is not revealed by us. */
    public static int colorOf(Entity e) {
        Integer c = ACTIVE.get(e.getId());
        return c == null ? -1 : c;
    }

    public static void clear() { ACTIVE.clear(); MARKS.clear(); }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        ACTIVE.clear();
        if (me == null || mc.level == null) { MARKS.clear(); return; }

        // Server marks
        Iterator<Map.Entry<Integer, int[]>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, int[]> m = it.next();
            if (--m.getValue()[0] <= 0) { it.remove(); continue; }
            ACTIVE.put(m.getKey(), m.getValue()[1]);
        }

        // Occult Vision
        Specialization spec = ClientSpecializationData.getSpec();
        if (spec != null && ClientTimers.isActive(OccultVisionAbility.ID)) {
            double range = 48;
            if (ClientSpecializationData.hasSkill("occ_veil_sight")) range += 24;
            Origin mine = ClientOriginData.getOrigin();
            if (mine != null && mine.getPath().equals("voidborn")) range += 24; // Abyssal Seer resonance

            double r2 = range * range;
            for (Player p : mc.level.players()) {
                if (p == me || p.isSpectator() || p.distanceToSqr(me) > r2) continue;
                Origin o = ClientIdentities.originOf(p.getUUID());
                ACTIVE.put(p.getId(), o != null ? o.getThemeColor() & 0xFFFFFF : 0xFFFFFF);
            }
            if (ClientSpecializationData.hasSkill("occ_ascended_occultist")) {
                AABB box = me.getBoundingBox().inflate(range);
                for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Enemy && e.isAlive()))
                    ACTIVE.putIfAbsent(e.getId(), HOSTILE_RGB);
            }
        }

        // Predator Senses
        Origin origin = ClientOriginData.getOrigin();
        if (origin != null && origin.getPath().equals("vampire") && ClientOriginData.hasSkill("vamp_senses")
                && me.isShiftKeyDown()) {
            AABB box = me.getBoundingBox().inflate(20);
            for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box, e -> e != me && VampireBlood.hasBlood(e)))
                ACTIVE.putIfAbsent(e.getId(), BLOOD_RGB);
        }
    }
}
