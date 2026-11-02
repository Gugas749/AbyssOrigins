package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.network.packet.KneelPacket;
import com.gugas749.abyssorigins.network.packet.PresencePromptPacket;
import com.gugas749.abyssorigins.network.packet.ResistStartPacket;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server authority for Presence: who is choosing, who is resisting, and their deadlines.
 * The client only reports a choice and the minigame outcome; silence is submission
 * (choice timeout → kneel) and a stalled minigame is a failure.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class PresenceSessions {

    public static final int CHOICE_TICKS  = 8 * 20;
    public static final int RESIST_TICKS  = 20 * 20;
    public static final int KNEEL_TICKS   = 20 * 20;
    public static final int BROKEN_TICKS  = 40 * 20;

    private enum Stage { CHOOSING, RESISTING }

    private static final class Session {
        final UUID caster; final String casterName;
        Stage stage = Stage.CHOOSING;
        int ticksLeft = CHOICE_TICKS;
        Session(UUID caster, String casterName) { this.caster = caster; this.casterName = casterName; }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private PresenceSessions() {}

    public static boolean involved(ServerPlayer p) {
        return SESSIONS.containsKey(p.getUUID()) || p.hasEffect(ModEffects.KNEELING);
    }

    /** The Presence falls on {@code target}: hold them and ask them to kneel or resist. */
    public static void begin(ServerPlayer caster, ServerPlayer target) {
        SESSIONS.put(target.getUUID(), new Session(caster.getUUID(), caster.getGameProfile().getName()));
        target.addEffect(new MobEffectInstance(ModEffects.AWED, CHOICE_TICKS + RESIST_TICKS + 40, 0, false, false, true));
        PacketDistributor.sendToPlayer(target, new PresencePromptPacket(caster.getGameProfile().getName(), CHOICE_TICKS));
    }

    public static void onChoice(ServerPlayer target, boolean kneel) {
        Session s = SESSIONS.get(target.getUUID());
        if (s == null || s.stage != Stage.CHOOSING) return;
        if (kneel) {
            SESSIONS.remove(target.getUUID());
            kneel(target, KNEEL_TICKS, s, false);
            return;
        }
        s.stage = Stage.RESISTING;
        s.ticksLeft = RESIST_TICKS;
        PacketDistributor.sendToPlayer(target, new ResistStartPacket(target.getRandom().nextLong(), RESIST_TICKS));
        notifyCaster(target, s, Component.translatable("abyssorigins.presence.caster_resisting", target.getDisplayName())
            .withStyle(ChatFormatting.GOLD));
    }

    public static void onResult(ServerPlayer target, boolean success) {
        Session s = SESSIONS.get(target.getUUID());
        if (s == null || s.stage != Stage.RESISTING) return;
        SESSIONS.remove(target.getUUID());
        if (success) defy(target, s);
        else break_(target, s);
    }

    // ---- Outcomes ---------------------------------------------------------------------

    private static void kneel(ServerPlayer target, int ticks, Session s, boolean broken) {
        target.removeEffect(ModEffects.AWED);
        target.addEffect(new MobEffectInstance(ModEffects.KNEELING, ticks, 0, false, false, true));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new KneelPacket(target.getId(), ticks));
        Fx.sound(target.level(), target.position(), SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.6f);
        target.displayClientMessage(Component.translatable(broken ? "abyssorigins.presence.broken" : "abyssorigins.presence.knelt",
            s.casterName, ticks / 20).withStyle(broken ? ChatFormatting.DARK_RED : ChatFormatting.GOLD), false);
        notifyCaster(target, s, Component.translatable(broken ? "abyssorigins.presence.caster_broken" : "abyssorigins.presence.caster_knelt",
            target.getDisplayName()).withStyle(ChatFormatting.GOLD));
    }

    /** Failed resistance: half of current health is torn away, then a long kneel. */
    private static void break_(ServerPlayer target, Session s) {
        float half = target.getHealth() / 2f;
        target.setHealth(Math.max(0.5f, half));
        target.level().broadcastDamageEvent(target, target.damageSources().magic());
        Vfx.burst(target.serverLevel(), target.position().add(0, 1, 0), 1.2f, Eminence.COLOR, 0x5A0010);
        Fx.sound(target.level(), target.position(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 1f, 0.5f);
        kneel(target, BROKEN_TICKS, s, true);
    }

    private static void defy(ServerPlayer target, Session s) {
        target.removeEffect(ModEffects.AWED);
        Vfx.shockwave(target.serverLevel(), target.position(), 3f, 0xFFFFFF, Eminence.COLOR_HI, 14);
        Fx.sound(target.level(), target.position(), SoundEvents.TOTEM_USE, 0.6f, 1.4f);
        target.displayClientMessage(Component.translatable("abyssorigins.presence.defied", s.casterName)
            .withStyle(ChatFormatting.AQUA), false);
        notifyCaster(target, s, Component.translatable("abyssorigins.presence.caster_defied", target.getDisplayName())
            .withStyle(ChatFormatting.RED));
    }

    private static void notifyCaster(ServerPlayer target, Session s, Component msg) {
        ServerPlayer caster = target.server.getPlayerList().getPlayer(s.caster);
        if (caster != null) caster.displayClientMessage(msg, true);
    }

    // ---- Deadlines & cleanup -------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer target = server.getPlayerList().getPlayer(e.getKey());
            Session s = e.getValue();
            if (target == null || !target.isAlive()) { it.remove(); continue; }
            if (--s.ticksLeft > 0) continue;
            it.remove();
            if (s.stage == Stage.CHOOSING) kneel(target, KNEEL_TICKS, s, false);   // silence is submission
            else break_(target, s);                                                // the will gave out
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (SESSIONS.remove(event.getEntity().getUUID()) != null) event.getEntity().removeEffect(ModEffects.AWED);
    }

    /** Players who start seeing a kneeling player need the tremble/pose state too. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer kneeler && event.getEntity() instanceof ServerPlayer viewer) {
            MobEffectInstance k = kneeler.getEffect(ModEffects.KNEELING);
            if (k != null) PacketDistributor.sendToPlayer(viewer, new KneelPacket(kneeler.getId(), k.getDuration()));
        }
    }
}
