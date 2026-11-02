package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Minimal server-tick scheduler for multi-stage abilities (delayed detonations,
 * second volleys). Tasks are dropped on server stop.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class Delayed {

    private static final class Task { int ticks; final Runnable action; Task(int t, Runnable a) { ticks = t; action = a; } }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Delayed() {}

    public static void schedule(int ticks, Runnable action) {
        PENDING.add(new Task(Math.max(1, ticks), action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) { TASKS.addAll(PENDING); PENDING.clear(); }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.ticks > 0) continue;
            it.remove();
            try { t.action.run(); }
            catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Delayed task failed", e); }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TASKS.clear();
        PENDING.clear();
    }
}
