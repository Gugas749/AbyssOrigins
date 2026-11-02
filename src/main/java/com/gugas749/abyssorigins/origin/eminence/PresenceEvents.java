package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.ModEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Enforces Awed / Kneeling on both sides: no attacking, using, breaking or placing,
 * and a kneeling player is held in the crouched (kneeling) pose.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class PresenceEvents {

    private PresenceEvents() {}

    public static boolean held(Player p) {
        return p.hasEffect(ModEffects.AWED) || p.hasEffect(ModEffects.KNEELING);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        boolean kneeling = p.hasEffect(ModEffects.KNEELING);
        if (kneeling && p.getForcedPose() != Pose.CROUCHING) p.setForcedPose(Pose.CROUCHING);
        else if (!kneeling && p.getForcedPose() == Pose.CROUCHING) p.setForcedPose(null);
        if (held(p) && p.isSprinting()) p.setSprinting(false);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event)                  { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event)  { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event){ deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) { deny(event.getEntity(), event); }
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event)                 { deny(event.getPlayer(), event); }

    /** Unbowed: no crown kneels, falls silent, or freezes. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player p) || !Eminence.is(p)) return;
        var e = event.getEffectInstance().getEffect();
        if (e.is(ModEffects.AWED) || e.is(ModEffects.KNEELING) || e.is(ModEffects.SEALED) || e.is(ModEffects.STUNNED))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    private static void deny(Player p, ICancellableEvent event) {
        if (p != null && held(p)) event.setCanceled(true);
    }
}
