package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.item.gear.InvertedSpearItem;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.skilltree.registry.SkillEffectHandler;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The Inverted Spear of Heaven's null field. While the spear is held in someone's main hand, every player
 * within {@link #RADIUS} blocks of them - the wielder included - is {@link ModEffects#NULLIFIED}:
 *
 *  - no origin / class / hidden power can be activated, and running sustained powers are ended at once;
 *  - PowerHooks are not dispatched (so no skill-node mechanic, aura, on-hit or cheat-death fires);
 *  - origin and class passives stop ticking, and switchable auras read as off;
 *  - attribute bonuses from unlocked skill nodes are stripped (origin base stats stay).
 *
 * The effect is short and refreshed every few ticks, so it lingers ~1.5s after leaving the field; adding and
 * removing it is what rebuilds the player's stats (see the MobEffectEvent handlers below).
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class Nullification {

    public static final double RADIUS = 12;
    private static final int PERIOD = 5, LINGER = 30;
    public static final int FIELD_COLOR = 0xC9CED6, FIELD_HI = 0xFFFFFF;

    private Nullification() {}

    /** Works on both sides: the effect is synced to its owner. */
    public static boolean is(Player p) {
        return p != null && p.hasEffect(ModEffects.NULLIFIED);
    }

    public static boolean holdsSpear(Player p) {
        return p.getMainHandItem().getItem() instanceof InvertedSpearItem;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer wielder : event.getServer().getPlayerList().getPlayers()) {
            if (!holdsSpear(wielder) || wielder.isSpectator() || !wielder.isAlive()) continue;
            if (wielder.tickCount % PERIOD == 0) project(wielder);
        }
    }

    private static void project(ServerPlayer wielder) {
        AABB box = wielder.getBoundingBox().inflate(RADIUS);
        for (ServerPlayer p : wielder.serverLevel().getEntitiesOfClass(ServerPlayer.class, box,
                p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(wielder) <= RADIUS * RADIUS)) {
            p.addEffect(new MobEffectInstance(ModEffects.NULLIFIED, LINGER, 0, false, false, true), wielder);
        }
    }

    // ---- Effect lifecycle -------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !event.getEffectInstance().is(ModEffects.NULLIFIED)) return;
        if (event.getOldEffectInstance() != null) return;          // just a refresh
        AbilityStates.stopAll(p);
        // Stats are rebuilt next tick: the effect is not in the active map yet while this event runs
        Delayed.schedule(1, () -> { if (p.isAlive()) { SkillEffectHandler.refresh(p); ModPackets.syncAll(p); } });
        p.displayClientMessage(Component.translatable("abyssorigins.message.nullified").withStyle(ChatFormatting.GRAY), true);
        AbyssVfx.burst(p.serverLevel(), Fx.chest(p), 0.7f, FIELD_COLOR, FIELD_HI);
        Fx.sound(p.level(), p.position(), SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.6f);
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(ModEffects.NULLIFIED)) restore(event.getEntity());
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(ModEffects.NULLIFIED)) restore(event.getEntity());
    }

    private static void restore(net.minecraft.world.entity.LivingEntity e) {
        if (!(e instanceof ServerPlayer p)) return;
        Delayed.schedule(1, () -> {
            if (!p.isAlive() || is(p)) return;
            SkillEffectHandler.refresh(p);
            ModPackets.syncAll(p);
            p.displayClientMessage(Component.translatable("abyssorigins.message.nullified_end").withStyle(ChatFormatting.AQUA), true);
        });
    }
}
