package com.gugas749.abyssorigins.item.ritual;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.item.ModDataComponents;
import com.gugas749.abyssorigins.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Seed of Origin — the same consumption and awakening as the Orb, but the origin is already
 * decided (stored in the {@code abyssorigins:seed_origin} component), so no menu is shown.
 * Tinted with its origin's colour. A seed with no (or an unknown) origin is barren.
 */
public class OriginSeedItem extends RitualItem {

    private static final int BARREN = 0x8A8299;

    public OriginSeedItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.RARE));
    }

    /** A seed bound to {@code origin}. */
    public static ItemStack of(Origin origin) {
        ItemStack stack = new ItemStack(ModItems.ORIGIN_SEED.get());
        stack.set(ModDataComponents.SEED_ORIGIN.get(), origin.getId());
        return stack;
    }

    public static Origin originOf(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.SEED_ORIGIN.get());
        return id == null ? null : AbyssOriginsAPI.getOrigin(id);
    }

    /** Tint for layer 0 (ARGB). */
    public static int tint(ItemStack stack) {
        Origin o = originOf(stack);
        return 0xFF000000 | (o != null ? o.getThemeColor() : BARREN);
    }

    @Override protected Origin fixedOrigin(ItemStack stack) { return originOf(stack); }

    @Override
    protected int color(ItemStack stack) {
        Origin o = originOf(stack);
        return o != null ? o.getThemeColor() & 0xFFFFFF : BARREN;
    }

    @Override
    protected boolean canBegin(Player player, ItemStack stack) {
        Origin origin = originOf(stack);
        if (origin == null) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.seed_barren"), true);
            return false;
        }
        if (AbyssOriginsAPI.getPlayerOrigin(player) == origin) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.seed_already", origin.getDisplayName()), true);
            return false;
        }
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        Origin o = originOf(stack);
        return o == null ? Component.translatable("item.abyssorigins.origin_seed.barren")
                         : Component.translatable("item.abyssorigins.origin_seed.of", o.getDisplayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        Origin o = originOf(stack);
        if (o == null) {
            lines.add(Component.translatable("item.abyssorigins.origin_seed.barren.desc").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("item.abyssorigins.origin_seed.desc", o.getDisplayName()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.abyssorigins.ritual.use").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
