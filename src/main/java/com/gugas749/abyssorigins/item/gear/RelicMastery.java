package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.api.Ascension;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.item.ModDataComponents;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A relic weapon's mastery, stored on the item (data component {@code abyssorigins:relic_mastery}), so every
 * relic grows on its own: two Sanguine Scythes can be at different mastery, and a relic keeps its mastery
 * when it changes hands.
 *
 * Mastery raises the relic's damage, attack speed and reach ({@link WeaponClass}) and unlocks its abilities:
 *  - {@link #PASSIVE_LEVEL}: the on-hit passive;
 *  - {@link #POWER_LEVEL}: the right-click power;
 *  - {@link #HONED_LEVEL} and {@link #MAX_LEVEL}: the power's cooldown shortens.
 *
 * XP comes from fighting with the relic in hand as its attuned wielder: hits, kills and power uses
 * (see GearEvents and OriginWeaponItem). Past {@link #UNASCENDED_CAP} it only grows while the wielder's
 * origin has ascended at least once (Noble, origin level 25); until then the XP bar fills and waits.
 */
public record RelicMastery(int level, int xp) {

    public static final int MAX_LEVEL = 30;
    public static final int PASSIVE_LEVEL = 5, POWER_LEVEL = 10, HONED_LEVEL = 20;
    /** Highest mastery a relic can reach while its wielder's origin has not ascended yet. */
    public static final int UNASCENDED_CAP = 20;

    public static final int XP_HIT = 1, XP_KILL = 8, XP_KILL_OTHER = 2, XP_KILL_BOSS = 150, XP_POWER = 3;

    public static final RelicMastery NONE = new RelicMastery(0, 0);

    public static final Codec<RelicMastery> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("level", 0).forGetter(RelicMastery::level),
        Codec.INT.optionalFieldOf("xp", 0).forGetter(RelicMastery::xp)
    ).apply(i, RelicMastery::new));

    public static final StreamCodec<ByteBuf, RelicMastery> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RelicMastery::level,
        ByteBufCodecs.VAR_INT, RelicMastery::xp,
        RelicMastery::new);

    public RelicMastery {
        level = Math.max(0, Math.min(MAX_LEVEL, level));
        xp = level >= MAX_LEVEL ? 0 : Math.max(0, xp);
    }

    public static RelicMastery of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RELIC_MASTERY.get(), NONE);
    }

    /** Highest mastery reachable by a wielder at origin level {@code originLevel}. */
    public static int capFor(int originLevel) {
        return Ascension.stageAt(originLevel) >= Ascension.NOBLE ? MAX_LEVEL : UNASCENDED_CAP;
    }

    public static int cap(Player p) {
        return capFor(p.getData(OriginAttachments.PLAYER_ORIGIN.get()).getOriginLevel());
    }

    /** XP needed to go from {@code level} to the next. Mastery 30 takes about 5 500 XP in all. */
    public static int xpForLevel(int level) { return 40 + 10 * level; }

    public boolean maxed()         { return level >= MAX_LEVEL; }
    public boolean passiveUnlocked() { return level >= PASSIVE_LEVEL; }
    public boolean powerUnlocked() { return level >= POWER_LEVEL; }

    /** Multiplier on the power's cooldown: 1, then 0.8 at {@link #HONED_LEVEL}, 0.65 at {@link #MAX_LEVEL}. */
    public float cooldownFactor()  { return level >= MAX_LEVEL ? 0.65f : level >= HONED_LEVEL ? 0.8f : 1f; }

    /** Progress toward the next level, 0..1 (1 when maxed). */
    public float progress()        { return maxed() ? 1f : xp / (float) xpForLevel(level); }

    // ---- Gaining XP (server) -----------------------------------------------------------------

    /**
     * Adds {@code amount} XP to the relic {@code stack}, levelling it up as needed (up to the wielder's
     * {@link #cap}), and tells the wielder. At the cap the XP bar fills and waits for the cap to rise.
     * The caller has already checked that {@code player} is the relic's attuned wielder.
     */
    public static void grant(ServerPlayer player, ItemStack stack, int amount) {
        if (amount <= 0 || !(stack.getItem() instanceof OriginWeaponItem w)) return;
        RelicMastery m = of(stack);
        if (m.maxed()) return;
        int cap = cap(player);
        int level = m.level, xp = m.xp + amount;
        while (level < cap && xp >= xpForLevel(level)) xp -= xpForLevel(level++);
        if (level >= cap && level < MAX_LEVEL) xp = Math.min(xp, xpForLevel(level));
        if (level == m.level && xp == m.xp) return;
        set(stack, new RelicMastery(level, xp));
        if (level > m.level) announce(player, stack, w, m.level, level);
        if (level >= cap && cap < MAX_LEVEL && xp >= xpForLevel(level) && m.xp < xpForLevel(m.level))
            player.sendSystemMessage(Component.translatable("abyssorigins.mastery.capped", stack.getHoverName(), cap, Ascension.NOBLE_LEVEL)
                .withStyle(ChatFormatting.GRAY));
    }

    /** Sets the mastery outright (staff command). */
    public static void set(ItemStack stack, RelicMastery m) {
        stack.set(ModDataComponents.RELIC_MASTERY.get(), m);
    }

    private static void announce(ServerPlayer p, ItemStack stack, OriginWeaponItem w, int from, int to) {
        int color = Gear.originColor(w.originPath());
        Component name = stack.getHoverName();
        p.displayClientMessage(Component.translatable("abyssorigins.mastery.level_up", name, to)
            .withStyle(s -> s.withColor(color)), true);
        String k = "abyssorigins.weapon." + w.power().id();
        if (from < PASSIVE_LEVEL && to >= PASSIVE_LEVEL)
            p.sendSystemMessage(Component.translatable("abyssorigins.mastery.unlock_passive", name, Component.translatable(k + ".passive"))
                .withStyle(ChatFormatting.DARK_AQUA));
        if (from < POWER_LEVEL && to >= POWER_LEVEL)
            p.sendSystemMessage(Component.translatable("abyssorigins.mastery.unlock_power", name, Component.translatable(k + ".power"))
                .withStyle(ChatFormatting.GOLD));
        if (from < HONED_LEVEL && to >= HONED_LEVEL)
            p.sendSystemMessage(Component.translatable("abyssorigins.mastery.honed", name).withStyle(ChatFormatting.GOLD));
        if (to >= MAX_LEVEL)
            p.sendSystemMessage(Component.translatable("abyssorigins.mastery.mastered", name).withStyle(ChatFormatting.GOLD));

        boolean milestone = to >= MAX_LEVEL || (from < PASSIVE_LEVEL && to >= PASSIVE_LEVEL)
            || (from < POWER_LEVEL && to >= POWER_LEVEL) || (from < HONED_LEVEL && to >= HONED_LEVEL);
        AbyssVfx.helix(p.serverLevel(), p, milestone ? 2.6f : 1.8f, color, 0xFFF4C8, milestone ? 30 : 18);
        Fx.sound(p.level(), p.position(), SoundEvents.ANVIL_USE, 0.5f, milestone ? 0.8f : 1.4f);
        if (milestone) Fx.sound(p.level(), p.position(), SoundEvents.PLAYER_LEVELUP, 0.7f, 0.9f);
    }
}
