package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.power.Skills;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Blood Dependency: mortal food is meaningless; blood is the only sustenance.
 *
 * The vanilla hunger bar is hidden for vampires (the blood bar replaces it) but its
 * value now MIRRORS blood: 100 blood = 20 hunger, never below 7 so sprinting always
 * works. This lets auto-feeding mods see a real deficit and feed Human Hearts, and
 * means natural regeneration only kicks in when the vampire is well fed.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class VampireHungerPassive extends OriginPassive {

    private static final int MIN_HUNGER = 7;

    public VampireHungerPassive() {
        super("vampire_hunger",
            "abyssorigins.passive.vampire_hunger.name",
            "abyssorigins.passive.vampire_hunger.desc");
    }

    static int hungerFor(float blood) {
        return Mth.clamp(MIN_HUNGER + Math.round(blood / 100f * (20 - MIN_HUNGER)), MIN_HUNGER, 20);
    }

    @Override public void onEquip(Player player) {
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        player.getFoodData().setFoodLevel(hungerFor(data.getResource()));
        player.getFoodData().setSaturation(0f);
    }

    @Override public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        player.getFoodData().setFoodLevel(hungerFor(data.getResource()));
        player.getFoodData().setSaturation(0f);
        if (data.getResource() <= 10f && player.tickCount % 100 == 0) {
            player.displayClientMessage(
                Component.translatable("abyssorigins.message.blood_critical"), true);
        }
    }

    @Override public void onRemove(Player player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);
    }

    /** Vampires refuse mortal food (blood items only). */
    @SubscribeEvent
    public static void onStartEating(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player p) || !Skills.isOrigin(p, "vampire")) return;
        ItemStack stack = event.getItem();
        if (!stack.has(DataComponents.FOOD) || isBlood(stack)) return;
        event.setCanceled(true);
        if (!p.level().isClientSide())
            p.displayClientMessage(Component.translatable("abyssorigins.message.vampire_food_refused"), true);
    }

    public static boolean isBlood(ItemStack stack) {
        return stack.is(ModItems.HUMAN_HEART.get());
    }
}
