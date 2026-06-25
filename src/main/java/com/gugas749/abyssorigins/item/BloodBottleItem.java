package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class BloodBottleItem extends Item {

    /** How much blood (out of 100) one bottle restores. */
    private static final float BLOOD_RESTORE = 25f;

    public BloodBottleItem() {
        super(new Properties()
            .stacksTo(16)
            .food(new FoodProperties.Builder()
                .nutrition(0)
                .saturationModifier(0f)
                .build())
        );
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32; // same as potions
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Only vampires can drink blood bottles beneficially
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null || !origin.getId().getPath().equals("vampire")) {
            player.displayClientMessage(
                Component.literal("§cThis is not meant for you...").withStyle(
                    net.minecraft.ChatFormatting.DARK_RED), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return returnBottle(stack, entity);
        }

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin != null && origin.getId().getPath().equals("vampire")) {
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            data.addResource(BLOOD_RESTORE);

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));

            player.displayClientMessage(
                Component.literal("§4\t\\2764 The blood restores your essence."), true);

            ModPackets.syncPlayer(player);
        }

        return returnBottle(stack, entity);
    }

    /** Returns a glass bottle after drinking, like vanilla potions. */
    private ItemStack returnBottle(ItemStack stack, LivingEntity entity) {
        stack.shrink(1);
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }
}
