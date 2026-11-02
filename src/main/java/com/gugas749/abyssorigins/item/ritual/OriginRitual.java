package com.gugas749.abyssorigins.item.ritual;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.network.packet.RitualPacket;
import com.gugas749.abyssorigins.power.Assignments;
import com.gugas749.abyssorigins.power.Delayed;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * The transformation shared by the Orb of Origins and the Seeds of Origin.
 *
 *   1. Channel  — while the item is consumed, energy spirals into the player.
 *   2. Awaken   — the item is gone; a pale aura flares around the player (visible to everyone,
 *                 and the player's camera swings to the front so they can watch it).
 *   3. Shape    — the Orb asks which origin to take (OriginChoiceScreen); a Seed already knows.
 *   4. Become   — the origin is assigned and the aura takes on the origin's colour.
 *
 * Server authoritative: a choice is only accepted from a player whose orb is awaiting one, and
 * only for a selectable origin. Closing the menu, dying, logging out or waiting too long ends
 * the ritual and returns the orb.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class OriginRitual {

    public static final int USE_TICKS = 40;
    private static final int AWAKEN_TICKS = 24;
    private static final int CHOICE_TIMEOUT = 20 * 120;
    private static final int AURA_LINGER = 20 * 6;
    /** Long enough to outlast any choice; stopped explicitly. */
    private static final int AURA_HOLD = CHOICE_TIMEOUT + 200;

    public static final int NEUTRAL = 0xB9A7FF, NEUTRAL_HI = 0xF4F0FF;

    private static final class Ritual {
        final ItemStack refund;
        final Origin fixed;
        boolean choosing;
        long expiresAt;
        Ritual(ItemStack refund, Origin fixed) { this.refund = refund; this.fixed = fixed; }
    }

    private static final Map<UUID, Ritual> RITUALS = new HashMap<>();

    private OriginRitual() {}

    public static boolean busy(ServerPlayer p) { return RITUALS.containsKey(p.getUUID()); }

    /** Origins an Orb may offer this player: every public one except what they already are. */
    public static List<Origin> choices(ServerPlayer p) {
        Origin current = AbyssOriginsAPI.getPlayerOrigin(p);
        List<Origin> out = new ArrayList<>();
        for (Origin o : AbyssOriginsAPI.getAllOrigins())
            if (!o.isHidden() && o != current) out.add(o);
        return out;
    }

    // ---- 1. Channel ------------------------------------------------------------------------

    public static void beginChannel(ServerPlayer p, int color) {
        ServerLevel level = p.serverLevel();
        Vfx.of(VfxType.HELIX).follow(p).scale(2.2f).colors(color, NEUTRAL_HI).duration(USE_TICKS).send(level);
        Vfx.of(VfxType.SIGIL).follow(p).scale(1.7f).extra(6).colors(color, NEUTRAL_HI).duration(USE_TICKS + 6).send(level);
        Fx.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.7f, 1.5f);
    }

    /** Motes drawn in from a ring around the player, faster as the item empties. */
    public static void channelTick(ServerPlayer p, int remaining) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0, 1.1, 0);
        if (remaining % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = level.random.nextDouble() * Math.PI * 2, r = 1.8 + level.random.nextDouble() * 0.6;
                double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r, y = c.y + (level.random.nextDouble() - 0.4) * 1.4;
                // count 0: the offset is the velocity — straight at the chest
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 0, (c.x - x) * 0.12, (c.y - y) * 0.12, (c.z - z) * 0.12, 1);
            }
        }
        if (remaining % 10 == 0)
            Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 0.8f + 0.8f * (1f - remaining / (float) USE_TICKS));
    }

    public static void interrupt(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vfx.stop(level, p, VfxType.HELIX);
        Vfx.stop(level, p, VfxType.SIGIL);
    }

    // ---- 2. Awaken --------------------------------------------------------------------------

    /**
     * The item has been consumed. {@code fixed} = the seed's origin, or null to let the player
     * choose. {@code refund} is handed back if the ritual ends without a transformation.
     */
    public static void awaken(ServerPlayer p, Origin fixed, ItemStack refund) {
        Ritual ritual = new Ritual(refund, fixed);
        RITUALS.put(p.getUUID(), ritual);

        ServerLevel level = p.serverLevel();
        Vec3 chest = p.position().add(0, 1.1, 0);
        Vfx.burst(level, chest, 1.4f, NEUTRAL, NEUTRAL_HI);
        Vfx.shockwave(level, p.position(), 3.2f, NEUTRAL, NEUTRAL_HI, 16);
        aura(level, p, NEUTRAL, NEUTRAL_HI, AURA_HOLD);
        Vfx.of(VfxType.HELIX).follow(p).scale(2.6f).colors(NEUTRAL, NEUTRAL_HI).duration(AWAKEN_TICKS + 20).send(level);
        Fx.sound(level, chest, SoundEvents.AMETHYST_BLOCK_BREAK, 1.2f, 0.6f);
        Fx.sound(level, chest, SoundEvents.BEACON_POWER_SELECT, 0.9f, 1.3f);
        PacketDistributor.sendToPlayer(p, RitualPacket.awaken());

        Delayed.schedule(AWAKEN_TICKS, () -> {
            if (RITUALS.get(p.getUUID()) != ritual || p.hasDisconnected()) return;
            if (fixed != null) {
                RITUALS.remove(p.getUUID());
                transform(p, fixed);
                return;
            }
            List<Origin> choices = choices(p);
            if (choices.isEmpty()) { cancel(p); return; }
            ritual.choosing = true;
            ritual.expiresAt = p.level().getGameTime() + CHOICE_TIMEOUT;
            PacketDistributor.sendToPlayer(p, RitualPacket.choose(choices.stream().map(o -> o.getId().toString()).toList()));
        });
    }

    // ---- 3. Shape ------------------------------------------------------------------------------

    /** From ChooseOriginPacket. Empty id = the player let the power fade. */
    public static void choose(ServerPlayer p, String originId) {
        Ritual ritual = RITUALS.get(p.getUUID());
        if (ritual == null || !ritual.choosing) return;                // no orb awaiting a choice: ignore
        if (originId.isEmpty()) { cancel(p); return; }

        ResourceLocation id = ResourceLocation.tryParse(originId);
        Origin origin = id == null ? null : AbyssOriginsAPI.getOrigin(id);
        if (origin == null || !choices(p).contains(origin)) return;    // not on offer: keep waiting
        RITUALS.remove(p.getUUID());
        transform(p, origin);
    }

    // ---- 4. Become ------------------------------------------------------------------------------

    private static void transform(ServerPlayer p, Origin origin) {
        ServerLevel level = p.serverLevel();
        Vfx.stop(level, p, VfxType.AURA, NEUTRAL);
        Assignments.setOrigin(p, origin);

        int c = origin.getThemeColor() & 0xFFFFFF, hi = lighten(c);
        Vec3 pos = p.position(), chest = pos.add(0, 1.1, 0);
        aura(level, p, c, hi, AURA_LINGER);
        Vfx.of(VfxType.PILLAR).at(pos).scale(7f).extra(0.9f).colors(c, 0xFFFFFF).duration(30).send(level);
        Vfx.of(VfxType.SIGIL).at(pos).scale(2.6f).extra(8).colors(c, hi).duration(50).send(level);
        Vfx.burst(level, chest, 1.8f, c, 0xFFFFFF);
        Vfx.shockwave(level, pos, 5f, c, hi, 22);
        Vfx.helix(level, p, 2.8f, c, hi, 30);
        Fx.sound(level, chest, SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.7f);
        Fx.sound(level, chest, SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 0.6f);
        Fx.sound(level, chest, SoundEvents.TOTEM_USE, 0.5f, 1.4f);

        p.sendSystemMessage(Component.translatable("abyssorigins.message.ritual_become", origin.getDisplayName())
            .withStyle(s -> s.withColor(c)));
        PacketDistributor.sendToPlayer(p, RitualPacket.transformed(origin.getId().toString()));
    }

    /** Ends the ritual without a transformation and hands the item back. */
    public static void cancel(ServerPlayer p) {
        Ritual ritual = RITUALS.remove(p.getUUID());
        if (ritual == null) return;
        if (!ritual.refund.isEmpty() && !p.getInventory().add(ritual.refund.copy())) p.drop(ritual.refund.copy(), false);
        if (!p.hasDisconnected()) {
            ServerLevel level = p.serverLevel();
            Vfx.stop(level, p, VfxType.AURA, NEUTRAL);
            Fx.sound(level, p.position(), SoundEvents.BEACON_DEACTIVATE, 0.7f, 1.2f);
            p.displayClientMessage(Component.translatable("abyssorigins.message.ritual_faded").withStyle(ChatFormatting.GRAY), true);
            PacketDistributor.sendToPlayer(p, RitualPacket.cancelled());
        }
    }

    private static void aura(ServerLevel level, ServerPlayer p, int c1, int c2, int ticks) {
        Vfx.of(VfxType.AURA).follow(p).scale(1.3f).colors(c1, c2).duration(ticks).send(level);
    }

    private static int lighten(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return ((r + (255 - r) / 2) << 16) | ((g + (255 - g) / 2) << 8) | (b + (255 - b) / 2);
    }

    // ---- Lifecycle --------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        Ritual r = RITUALS.get(p.getUUID());
        if (r != null && r.choosing && p.level().getGameTime() > r.expiresAt) cancel(p);
    }

    /** Fires before the player is saved, so the refunded item is kept. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) cancel(p);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) cancel(p);
    }
}
