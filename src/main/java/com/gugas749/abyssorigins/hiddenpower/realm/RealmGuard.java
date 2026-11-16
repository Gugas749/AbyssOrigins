package com.gugas749.abyssorigins.hiddenpower.realm;

import com.gugas749.abyssorigins.hiddenpower.Portals;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmData.Session;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Enforces access to the private realms. Run for every survival / adventure player standing in one
 * (creative and spectator staff may inspect freely):
 *
 *   pocket realm     - you must own a plot and stay within {@link PocketRealm#RADIUS} of it; strays
 *                      and void-fallers go back to their own platform. Anyone else may only be in a
 *                      plot while one of its owner's portals links it to the outside (a guest); once
 *                      it closes, or if they turn up anywhere else, they're sent back where they came from.
 *   punishment realm - targets are handled by {@link PunishRealm#restrain}; a punisher watching
 *                      their own active punishment stays near its cell; anyone else is sent home.
 */
public final class RealmGuard {

    private RealmGuard() {}

    public static void check(ServerPlayer p) {
        if (p.isSpectator() || p.isCreative() || !Realms.inRealm(p)) return;
        RealmData data = RealmData.get(p.server);
        UUID id = p.getUUID();
        ServerLevel level = p.serverLevel();

        if (level.dimension() == Realms.POCKET) {
            OptionalInt own = data.existingPlot(id), here = PocketRealm.plotAt(p.position());
            boolean home = own.isPresent() && here.isPresent() && own.getAsInt() == here.getAsInt();
            if (!home && here.isPresent() && Portals.linksPlot(here.getAsInt())) {       // a guest through an open portal
                if (p.getY() < PocketRealm.FLOOR_Y - 40) PocketRealm.rescue(p, level, here.getAsInt());
                return;
            }
            if (own.isEmpty() || (here.isPresent() && !home)) { Realms.travel(p, data.takePocketReturn(id)); return; }
            BlockPos c = PocketRealm.center(own.getAsInt());
            if (strayed(p, c, PocketRealm.RADIUS) || p.getY() < PocketRealm.FLOOR_Y - 40)
                PocketRealm.rescue(p, level, own.getAsInt());
            return;
        }

        if (data.session(id) != null) return;                          // a target: restrain() holds them
        Map.Entry<UUID, Session> watching = data.isObserving(id) ? data.sessionBy(id) : null;
        if (watching == null) {
            Location back = data.takeObserverReturn(id);
            Realms.travel(p, back != null ? back : data.takePocketReturn(id));
            return;
        }
        int cell = watching.getValue().cell;
        if (strayed(p, PunishRealm.center(cell), PunishRealm.OBSERVER_RADIUS) || p.getY() < PunishRealm.FLOOR_Y - 10) {
            Vec3 spot = PunishRealm.observerSpot(cell);
            Realms.travel(p, level, spot, 180f, 10f);
        }
    }

    private static boolean strayed(ServerPlayer p, BlockPos center, int radius) {
        double dx = p.getX() - (center.getX() + 0.5), dz = p.getZ() - (center.getZ() + 0.5);
        return dx * dx + dz * dz > (double) radius * radius;
    }
}
