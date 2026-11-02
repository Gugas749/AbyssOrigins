package com.gugas749.abyssorigins.item.ritual;

import com.gugas749.abyssorigins.api.Origin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Something consumed to take on an Origin. Holding use plays the consumption (energy drawn in,
 * crystal shards, chimes); finishing it hands over to {@link OriginRitual}. Subclasses decide
 * whether the origin is fixed (Seed) or chosen (Orb).
 */
public abstract class RitualItem extends Item {

    protected RitualItem(Properties properties) { super(properties); }

    /** The origin this item transforms into, or null to let the player choose. */
    protected abstract Origin fixedOrigin(ItemStack stack);

    /** Channelling colour (RGB). */
    protected abstract int color(ItemStack stack);

    /**
     * Whether the ritual can start; tell the player why not. Runs on both sides — the client
     * only knows its own origin, the server re-checks everything.
     */
    protected abstract boolean canBegin(Player player, ItemStack stack);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBegin(player, stack)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer sp) {
            if (OriginRitual.busy(sp)) return InteractionResultHolder.fail(stack);
            OriginRitual.beginChannel(sp, color(stack));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer sp) OriginRitual.channelTick(sp, remaining);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer sp) OriginRitual.interrupt(sp);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer sp) || OriginRitual.busy(sp) || !canBegin(sp, stack)) return stack;
        Origin fixed = fixedOrigin(stack);
        boolean creative = sp.getAbilities().instabuild;
        ItemStack refund = creative ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (!creative) stack.shrink(1);
        OriginRitual.awaken(sp, fixed, refund);
        return stack;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return OriginRitual.USE_TICKS; }
    /** Brought to the lips and drunk in — shards of crystal and chimes instead of crumbs. */
    @Override public UseAnim getUseAnimation(ItemStack stack)  { return UseAnim.EAT; }
    @Override public SoundEvent getEatingSound()               { return SoundEvents.AMETHYST_BLOCK_CHIME; }
}
