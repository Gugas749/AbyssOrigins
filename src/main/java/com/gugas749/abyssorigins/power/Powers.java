package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.api.Specialization;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Dispatches PowerHooks for a player's origin and class, isolating failures. */
public final class Powers {

    private Powers() {}

    public static void forEach(ServerPlayer p, Consumer<PowerHooks> action) {
        Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
        if (o != null) run(o.getHooks(), action);
        Specialization s = Skills.specOf(p);
        if (s != null) run(s.getHooks(), action);
    }

    /** True if any hook returns true (short-circuits). */
    public static boolean any(ServerPlayer p, Predicate<PowerHooks> test) {
        Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
        if (o != null && safeTest(o.getHooks(), test)) return true;
        Specialization s = Skills.specOf(p);
        return s != null && safeTest(s.getHooks(), test);
    }

    /** Product of a multiplier across origin and class hooks. */
    public static float product(ServerPlayer p, ToDoubleFunction<PowerHooks> f) {
        double m = 1.0;
        Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
        if (o != null) m *= f.applyAsDouble(o.getHooks());
        Specialization s = Skills.specOf(p);
        if (s != null) m *= f.applyAsDouble(s.getHooks());
        return (float) m;
    }

    private static void run(PowerHooks h, Consumer<PowerHooks> action) {
        try { action.accept(h); }
        catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Power hook error in {}", h.getClass().getSimpleName(), e); }
    }

    private static boolean safeTest(PowerHooks h, Predicate<PowerHooks> test) {
        try { return test.test(h); }
        catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Power hook error in {}", h.getClass().getSimpleName(), e); return false; }
    }
}
