package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.hiddenpower.realm.CrossPin;
import com.gugas749.abyssorigins.hiddenpower.realm.PunishRealm;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmData;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server hooks for the hidden powers: portals, the realm guard and the punishment's restraint. */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class HiddenPowerEvents {

    private HiddenPowerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Portals.tick(event.getServer());
        PunishRealm.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            pinLocal(event.getEntity());
            return;
        }
        RealmData.Session s = RealmData.get(p.server).session(p.getUUID());
        if (s != null) PunishRealm.restrain(p, s);
        if (p.tickCount % 10 == 0) RealmGuard.check(p);
    }

    /** Before the player moves this tick: undo any shove that arrived since the last one. */
    @SubscribeEvent
    public static void onPlayerTickStart(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            pinLocal(event.getEntity());
            return;
        }
        RealmData.Session s = RealmData.get(p.server).session(p.getUUID());
        if (s != null) PunishRealm.restrain(p, s);
    }

    /**
     * The crucified player's own client keeps them on the cross: their client is what moves them, so
     * without this a shove or lag shows them sliding off before the server pulls them back.
     */
    private static void pinLocal(Player p) {
        if (!p.level().isClientSide() || !p.isLocalPlayer() || !CrossPin.crucified(p)) return;
        BlockPos cross = CrossPin.cross(p);
        if (cross != null) CrossPin.hold(p, cross);
    }

    /** Nothing knocks a crucified player off the cross. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player p && (CrossPin.crucified(p)
                || p instanceof ServerPlayer sp && PunishRealm.isPunished(sp)))
            event.setCanceled(true);
    }

    /** A target who dies is freed (they respawn as normal); a watcher who dies loses their saved return point. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer p)) return;
        RealmData data = RealmData.get(p.server);
        if (data.session(p.getUUID()) != null) PunishRealm.release(p.server, p.getUUID(), false);
        if (data.isObserving(p.getUUID())) data.takeObserverReturn(p.getUUID());
    }

    /** An unlinked portal endpoint doesn't outlive the session (an open portal runs out its time). */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Portals.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Portals.clear();
    }

    // ---- A crucified player can't act (checked on both sides, like Presence) -----------------

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event)                   { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event)   { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock event)  { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event)  { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event)                  { deny(event.getPlayer(), event); }

    private static void deny(Player p, ICancellableEvent event) {
        if (p != null && p.hasEffect(ModEffects.CRUCIFIED)) event.setCanceled(true);
    }
}
