package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.SimpleTier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.BlockTags;

import java.util.ArrayList;
import java.util.List;

/**
 * Origin gear rules: only the matching origin may wield it (creative players are exempt).
 * Anyone else is rejected — hurt and weakened — and gains none of its powers or set bonus.
 */
public final class Gear {

    private Gear() {}

    /** Diamond-grade tier shared by all six relic weapons. */
    public static final SimpleTier RELIC = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8f, 3f, 15,
        () -> Ingredient.of(Items.DIAMOND));

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public static boolean attuned(Player p, String origin) {
        return p.isCreative() || Skills.isOrigin(p, origin) || Eminence.is(p);   // every relic answers the crown
    }

    /** Pieces of {@code set} currently worn. */
    public static int worn(Player p, ArmorSet set) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR)
            if (p.getItemBySlot(slot).getItem() instanceof OriginArmorItem a && a.set() == set) n++;
        return n;
    }

    /** True if the player wears the complete outfit of their own origin. */
    public static boolean fullSet(Player p, String origin) {
        ArmorSet set = ArmorSet.forOrigin(origin);
        return set != null && Skills.isOrigin(p, origin) && worn(p, set) == 4;
    }

    /** Number of worn pieces belonging to a different origin. */
    public static int foreignPieces(Player p) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = p.getItemBySlot(slot);
            if (s.getItem() instanceof OriginBound b && !attuned(p, b.originPath())) n++;
        }
        return n;
    }

    /**
     * Punish a non-attuned wielder. {@code severity}: 2 = swinging/using a relic,
     * 1 = wearing foreign pieces (damage scales with {@code amount}).
     */
    public static void reject(ServerPlayer p, OriginBound item, int severity, int amount) {
        if (!PowerState.tryTrigger(p, "gear_reject", severity >= 2 ? 10 : 60)) return;
        float dmg = severity >= 2 ? 3f : Math.max(1, amount);
        p.hurt(p.damageSources().magic(), dmg);
        int amp = severity >= 2 ? 1 : 0;
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, amp, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, amp, false, true));
        if (severity >= 2) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true));

        int color = originColor(item.originPath());
        Vfx.of(VfxType.BURST).at(Fx.chest(p)).scale(0.8f).colors(color, 0x200008).flags(Vfx.VOID).duration(12).send(p.serverLevel());
        Fx.sound(p.level(), p.position(), SoundEvents.WITHER_HURT, 0.5f, 1.6f);
        p.displayClientMessage(Component.translatable("abyssorigins.message.gear_rejected", originName(item.originPath()))
            .withStyle(ChatFormatting.RED), true);
    }

    // ---- Presentation -------------------------------------------------------------

    public static Origin origin(String path) {
        return AbyssOriginsAPI.getOrigin(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path));
    }

    public static Component originName(String path) {
        Origin o = origin(path);
        return o != null ? o.getDisplayName() : Component.literal(path);
    }

    public static int originColor(String path) {
        Origin o = origin(path);
        return o != null ? o.getThemeColor() & 0xFFFFFF : 0xC9C3E6;
    }

    /** Splits a long translated line into tooltip-sized lines. */
    public static List<Component> wrap(Component text, Style style, int maxChars) {
        List<Component> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.getString().split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > maxChars) {
                out.add(Component.literal(line.toString()).withStyle(style));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(Component.literal(line.toString()).withStyle(style));
        return out;
    }

    public static MutableComponent boundLine(String origin) {
        return Component.literal("◆ ").append(Component.translatable("abyssorigins.tooltip.bound", originName(origin)))
            .withStyle(Style.EMPTY.withColor(originColor(origin)));
    }
}
