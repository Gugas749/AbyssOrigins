package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Human Hearts drop from humans: villagers, wandering traders, illagers and witches.
 * 8% base chance, +3% per Looting level.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class VillagerHeartDropHandler {

    private static final float BASE_CHANCE             = 0.08f;  // 8%
    private static final float LOOTING_BONUS_PER_LEVEL = 0.03f;  // +3% per level

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var e = event.getEntity();
        if (!(e instanceof Villager || e instanceof WanderingTrader || e instanceof AbstractIllager || e instanceof Witch)) return;

        int lootingLevel = 0;
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            var registry = event.getEntity().level()
                .registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
            Enchantment looting = registry.get(Enchantments.LOOTING);
            if (looting != null) {
                Holder<Enchantment> lootingHolder = registry.wrapAsHolder(looting);
                lootingLevel = EnchantmentHelper.getTagEnchantmentLevel(
                    lootingHolder, killer.getMainHandItem());
            }
        }

        float dropChance = BASE_CHANCE + (LOOTING_BONUS_PER_LEVEL * lootingLevel);

        if (event.getEntity().getRandom().nextFloat() < dropChance) {
            var entity = event.getEntity();
            event.getDrops().add(new ItemEntity(
                entity.level(),
                entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(ModItems.HUMAN_HEART.get())
            ));
            Abyssorigins.LOGGER.debug("[AbyssOrigins] Human Heart dropped (looting={})", lootingLevel);
        }
    }
}
