package com.gugas749.abyssorigins.item;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A vampire's food. It is a genuine food item (nutrition, always edible) so that
 * auto-feeding mods — e.g. Sophisticated Backpacks' Feeding Upgrade — recognise it.
 * For vampires the hunger bar mirrors their blood (see VampireHungerPassive), so a
 * feeder sees the deficit and feeds hearts; the heart then restores 40 blood.
 */
public class HumanHeartItem extends Item {

    private static final float BLOOD_RESTORE = 40f;

    public HumanHeartItem() {
        super(new Properties()
            .stacksTo(16)
            .food(new FoodProperties.Builder()
                .nutrition(6)
                .saturationModifier(0.3f)
                .alwaysEdible()
                .build())
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return result;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        String path = origin != null ? origin.getId().getPath() : "";

        if (path.equals("vampire")) {
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            data.addResource(BLOOD_RESTORE);
            player.removeEffect(MobEffects.CONFUSION);
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.WEAKNESS);
            ModPackets.syncPlayer(player);
        } else {
            // Non-vampires get punished
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER,    200, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,  200, 0, false, true));
        }

        return result;
    }
}
