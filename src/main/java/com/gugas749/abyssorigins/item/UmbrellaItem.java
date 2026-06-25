package com.gugas749.abyssorigins.item;

import com.google.common.base.Suppliers;
import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.function.Supplier;

@EventBusSubscriber(modid = Abyssorigins.MODID)
public class UmbrellaItem extends Item {

    private final Supplier<ItemAttributeModifiers> mainHandAttributes;

    public UmbrellaItem() {
        super(new Properties().stacksTo(1));
        this.mainHandAttributes = Suppliers.memoize(() -> {
            ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
            b.add(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(BuiltInRegistries.ITEM.getKey(this),
                    -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND);
            return b.build();
        });
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return mainHandAttributes.get();
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!player.getMainHandItem().is(ModItems.UMBRELLA.get())) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;
        String path = origin.getId().getPath();
        if (!path.equals("vampire")) return;

        if (event.getSource().is(DamageTypes.ON_FIRE) || event.getSource().is(DamageTypes.IN_FIRE)) {
            event.setCanceled(true);
            if (player.isOnFire()) player.clearFire();
        }
    }
}
