package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.hiddenpower.realm.Location;
import com.gugas749.abyssorigins.hiddenpower.realm.PocketRealm;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmData;
import com.gugas749.abyssorigins.hiddenpower.realm.Realms;
import com.gugas749.abyssorigins.network.packet.SyncTimersPacket;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Portals from {@link PortalAbility}. A portal is a linked pair of endpoints - not blocks or
 * entities: it exists only here, drawn as a rift that opens and stays open (with a few
 * drifting motes), and is gone after {@link #LIFETIME} ticks, when its creator closes it, or when
 * the server stops. Nothing is ever left in the world. It makes no sound of its own: the only sound
 * is the teleport when someone goes through.
 *
 * One portal per creator. Any player (not spectators) who steps into either endpoint is taken to
 * the other, in either direction and across dimensions. A traveller arrives facing out of the far
 * endpoint and must step out of it before it will carry them again; on top of that no portal takes
 * the same player twice within {@link #TRANSIT_COOLDOWN} ticks, so nobody bounces.
 *
 * An endpoint may sit in its creator's own pocket-realm plot: while the portal is open, anyone who
 * comes through is a guest of that plot ({@link #linksPlot}). Endpoints are never placed in the
 * punishment realm.
 *
 * The time left is shown to the creator through {@link AbilityStates}' timer sync, as a timer on the
 * power's HUD slot plus a countdown label - see {@link #timers}. It is re-sent every {@link #RESYNC}
 * ticks (and on login, respawn and dimension change), so the display tracks the real expiry.
 */
public final class Portals {

    public static final int LIFETIME = 300 * 20, TRANSIT_COOLDOWN = 60;
    /** How often the owner's countdown is re-sent, so a lagging client can't drift from the real time left. */
    private static final int RESYNC = 100;
    /** How often each endpoint's rift is re-broadcast, so players who arrive (or come through) later see it. */
    private static final int REDRAW = 20;
    public static final double HALF_WIDTH = 0.7, HEIGHT = 2.4;
    private static final int COLOR = 0x9B4DFF, COLOR_2 = 0x2A0F5C;

    /** One end of a portal: a vertical oval standing on {@code base} (bottom centre), facing {@code facing}. */
    public record Endpoint(ResourceKey<Level> dimension, Vec3 base, Vec3 facing) {
        Vec3 center() { return base.add(0, HEIGHT / 2, 0); }
        Vec3 right()  { return new Vec3(-facing.z, 0, facing.x); }

        /** A thin box in the portal's plane: you have to walk into the portal, not just past it. */
        AABB box() {
            Vec3 r = right().scale(HALF_WIDTH), f = facing.scale(0.35);
            Vec3 a = base.add(r).add(f), b = base.subtract(r).subtract(f).add(0, HEIGHT, 0);
            return new AABB(a, b);
        }

        float yaw() { return (float) (Mth.atan2(-facing.x, facing.z) * Mth.RAD_TO_DEG); }
    }

    /** An open portal: endpoint {@code a} (placed first) linked to endpoint {@code b}. */
    public static final class Portal {
        final UUID owner;
        final Endpoint a, b;
        int age;

        Portal(UUID owner, Endpoint a, Endpoint b) { this.owner = owner; this.a = a; this.b = b; }

        public int remaining() { return LIFETIME - age; }
        Endpoint end(int i)    { return i == 0 ? a : b; }
    }

    /** "Player is standing in the endpoint they arrived at" - cleared once they step out. */
    private record Arrival(UUID portalOwner, int end) {}

    private static final Map<UUID, Portal> OPEN = new LinkedHashMap<>();
    private static final Map<UUID, Endpoint> PENDING = new HashMap<>();
    private static final Map<UUID, Long> TRANSIT = new HashMap<>();
    private static final Map<UUID, Arrival> ARRIVED = new HashMap<>();

    private Portals() {}

    // ---- Queries -------------------------------------------------------------------------------

    public static Portal open(UUID owner)        { return OPEN.get(owner); }
    public static Endpoint pending(UUID owner)   { return PENDING.get(owner); }

    /** True while some open portal has an endpoint inside pocket-realm plot {@code plot}. */
    public static boolean linksPlot(int plot) {
        for (Portal p : OPEN.values())
            for (int i = 0; i < 2; i++) {
                Endpoint e = p.end(i);
                if (e.dimension() == Realms.POCKET && PocketRealm.plotAt(e.base()).orElse(-1) == plot) return true;
            }
        return false;
    }

    /** Timer source: the creator's open portal, under the power's id, so the HUD shows its time left. */
    public static void timers(ServerPlayer player, List<SyncTimersPacket.Entry> out) {
        Portal p = OPEN.get(player.getUUID());
        if (p != null) out.add(new SyncTimersPacket.Entry(HiddenPowers.PORTAL.getId(), p.remaining(), LIFETIME));
    }

    // ---- Lifecycle -------------------------------------------------------------------------------

    public static void setPending(ServerPlayer owner, Endpoint e) { PENDING.put(owner.getUUID(), e); }
    public static boolean cancelPending(UUID owner)               { return PENDING.remove(owner) != null; }

    /** Links {@code owner}'s pending endpoint to {@code second} and opens the portal. */
    public static void link(ServerPlayer owner, Endpoint first, Endpoint second) {
        close(owner.server, owner.getUUID(), false);
        PENDING.remove(owner.getUUID());
        Portal p = new Portal(owner.getUUID(), first, second);
        OPEN.put(owner.getUUID(), p);
        for (int i = 0; i < 2; i++) {
            ServerLevel level = owner.server.getLevel(p.end(i).dimension());
            if (level != null) rift(level, p.end(i), LIFETIME, false);       // opens, then holds
        }
        AbilityStates.sync(owner);
    }

    /** Closes {@code owner}'s portal. @return false if they had none. */
    public static boolean close(MinecraftServer srv, UUID owner, boolean effects) {
        Portal p = OPEN.remove(owner);
        if (p == null) return false;
        if (effects) collapse(srv, p);
        ServerPlayer player = srv.getPlayerList().getPlayer(owner);
        if (player != null) AbilityStates.sync(player);
        return true;
    }

    /** Closes both rifts (plays their closing animation). */
    private static void collapse(MinecraftServer srv, Portal p) {
        for (int i = 0; i < 2; i++) {
            Endpoint e = p.end(i);
            ServerLevel level = srv.getLevel(e.dimension());
            if (level != null) AbyssVfx.of(VfxType.RIFT).at(e.center()).flags(AbyssVfx.STOP).send(level);
        }
    }

    /** The endpoint's rift, held open for {@code ticks}; {@code open} = appear already open (a re-broadcast). */
    private static void rift(ServerLevel level, Endpoint e, int ticks, boolean open) {
        AbyssVfx.of(VfxType.RIFT).at(e.center()).dir(e.facing()).scale((float) HEIGHT).colors(COLOR, COLOR_2)
            .duration(ticks).extra(open ? 1 : 0).flags(AbyssVfx.SUSTAIN).send(level);
    }

    // ---- Tick ------------------------------------------------------------------------------------

    /** Server tick: ages and draws every portal, marks pending endpoints, and sends whoever steps in. */
    public static void tick(MinecraftServer srv) {
        if (srv.getTickCount() % 10 == 0) drawPending(srv);
        if (OPEN.isEmpty()) { ARRIVED.clear(); return; }
        long now = srv.overworld().getGameTime();
        List<Runnable> sends = new ArrayList<>();
        Set<UUID> stillInside = new HashSet<>();

        Iterator<Portal> it = OPEN.values().iterator();
        while (it.hasNext()) {
            Portal p = it.next();
            if (++p.age >= LIFETIME) {
                it.remove();
                collapse(srv, p);
                ServerPlayer owner = srv.getPlayerList().getPlayer(p.owner);
                if (owner != null) {
                    owner.displayClientMessage(Component.translatable("abyssorigins.message.portal_expired"), true);
                    AbilityStates.sync(owner);
                }
                continue;
            }
            if (p.age % RESYNC == 0) {                                          // keep the owner's countdown exact
                ServerPlayer owner = srv.getPlayerList().getPlayer(p.owner);
                if (owner != null) AbilityStates.sync(owner);
            }
            for (int i = 0; i < 2; i++) {
                Endpoint here = p.end(i), there = p.end(1 - i);
                ServerLevel level = srv.getLevel(here.dimension());
                if (level == null) continue;
                if (p.age % REDRAW == 0) rift(level, here, p.remaining(), true);
                if (p.age % 4 == 0) motes(level, here);

                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, here.box())) {
                    if (player.isSpectator() || !player.isAlive()) continue;
                    Arrival arrived = ARRIVED.get(player.getUUID());
                    if (arrived != null && arrived.portalOwner().equals(p.owner) && arrived.end() == i) {
                        stillInside.add(player.getUUID());                // hasn't stepped out yet
                        continue;
                    }
                    Long last = TRANSIT.get(player.getUUID());
                    if (last != null && now - last < TRANSIT_COOLDOWN) continue;
                    TRANSIT.put(player.getUUID(), now);
                    int arrivalEnd = 1 - i;
                    UUID portalOwner = p.owner;
                    sends.add(() -> {
                        if (send(srv, player, there)) {
                            ARRIVED.put(player.getUUID(), new Arrival(portalOwner, arrivalEnd));
                            stillInside.add(player.getUUID());
                        }
                    });
                }
            }
        }
        sends.forEach(Runnable::run);                    // teleport after iterating the levels' entity lists
        ARRIVED.keySet().retainAll(stillInside);
        if (now % 200 == 0) TRANSIT.values().removeIf(t -> now - t > TRANSIT_COOLDOWN);
    }

    /**
     * Carries {@code player} to endpoint {@code to}. Coming into the pocket realm from outside, their
     * way back is remembered (so if the link closes while they're inside, they're sent home);
     * leaving it, that memory is dropped.
     */
    private static boolean send(MinecraftServer srv, ServerPlayer player, Endpoint to) {
        ServerLevel level = srv.getLevel(to.dimension());
        if (level == null) return false;
        RealmData data = RealmData.get(srv);
        boolean wasInPocket = player.level().dimension() == Realms.POCKET;
        if (to.dimension() == Realms.POCKET && !Realms.inRealm(player)) data.setPocketReturn(player.getUUID(), Location.of(player));
        else if (wasInPocket && to.dimension() != Realms.POCKET) data.takePocketReturn(player.getUUID());
        Realms.travel(player, level, to.base(), to.yaw(), player.getXRot());
        return true;
    }

    // ---- Drawing ---------------------------------------------------------------------------------

    /** A few portal motes drifting around the rift. */
    private static void motes(ServerLevel level, Endpoint e) {
        Vec3 c = e.center();
        level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 4, HALF_WIDTH * 0.4, HEIGHT * 0.3, HALF_WIDTH * 0.4, 0.3);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 1, HALF_WIDTH * 0.3, HEIGHT * 0.25, HALF_WIDTH * 0.3, 0.01);
    }

    /** A placed-but-unlinked endpoint: a faint ring on the ground, shown only to its creator. */
    private static void drawPending(MinecraftServer srv) {
        PENDING.forEach((id, e) -> {
            ServerPlayer owner = srv.getPlayerList().getPlayer(id);
            if (owner == null || owner.level().dimension() != e.dimension()) return;
            for (int i = 0; i < 12; i++) {
                double a = Mth.TWO_PI * i / 12;
                Vec3 q = e.base().add(Math.cos(a) * HALF_WIDTH, 0.1, Math.sin(a) * HALF_WIDTH);
                owner.serverLevel().sendParticles(owner, Fx.dust(COLOR, 0.8f), true, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
        });
    }

    /** The creator logged out: an unlinked endpoint is dropped (an open portal stays until it expires). */
    public static void forget(UUID owner) { PENDING.remove(owner); }

    public static void clear() {
        OPEN.clear();
        PENDING.clear();
        TRANSIT.clear();
        ARRIVED.clear();
    }
}
