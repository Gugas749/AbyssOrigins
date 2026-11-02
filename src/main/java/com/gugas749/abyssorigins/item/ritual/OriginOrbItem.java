package com.gugas749.abyssorigins.item.ritual;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Orb of Origins — a circle of unshaped energy. Consumed, it lets the player choose an origin. */
public class OriginOrbItem extends RitualItem {

    public OriginOrbItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.EPIC));
    }

    @Override protected Origin fixedOrigin(ItemStack stack) { return null; }
    @Override protected int color(ItemStack stack)          { return OriginRitual.NEUTRAL; }
    @Override public boolean isFoil(ItemStack stack)        { return true; }

    @Override
    protected boolean canBegin(Player player, ItemStack stack) {
        Origin current = AbyssOriginsAPI.getPlayerOrigin(player);
        boolean any = AbyssOriginsAPI.getAllOrigins().stream().anyMatch(o -> !o.isHidden() && o != current);
        if (!any) player.displayClientMessage(Component.translatable("abyssorigins.message.ritual_nothing"), true);
        return any;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.abyssorigins.origin_orb.desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.abyssorigins.ritual.use").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
