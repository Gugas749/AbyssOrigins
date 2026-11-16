package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.gear.GearRenderers;
import com.gugas749.abyssorigins.entity.ThrownScytheEntity;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.origin.eminence.SovereignScythe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Eminence's Sovereign Scythe. Never crafted: Mire Call summons it (SovereignScythe).
 *
 *  - Melee: a three-swing chain (ScytheCombat) - Reaping Sweep, Sovereign Strike, Harvest. Vanilla's sweep is
 *    off; the chain does its own cleaving.
 *  - Hold use and release to throw it (ThrownScytheEntity).
 *  - Enchantable like a sword; it never breaks (stops at 1 durability) and is repaired on every summon.
 */
public class SovereignScytheItem extends SwordItem implements OriginBound, GeoItem {

    /**
     * Fixed at a fully mastered relic's level (it has no mastery of its own): damage 1 + 3 + 21 = 25,
     * speed 4 - 2.6 = 1.4, reach +2.
     */
    private static final int DAMAGE = 21;
    private static final float SPEED = -2.6f;
    private static final double REACH = 2.0;
    /** Ticks of wind-up before a throw counts. */
    public static final int MIN_CHARGE = 8, FULL_CHARGE = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SovereignScytheItem() {
        super(Gear.RELIC, new Item.Properties().rarity(Rarity.EPIC).fireResistant().attributes(attributes()));
        GeoItem.registerSyncedAnimatable(this);
    }

    private static ItemAttributeModifiers attributes() {
        return SwordItem.createAttributes(Gear.RELIC, DAMAGE, SPEED)
            .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "sovereign_reach"), REACH,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    @Override public String originPath() { return Eminence.PATH; }

    // ---- Throwing -------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack)                 { return UseAnim.NONE; }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof ServerPlayer p)) return;
        int charge = getUseDuration(stack, entity) - timeLeft;
        if (charge < MIN_CHARGE || !SovereignScythe.bonded(p, stack)) return;
        float power = Math.min(1f, charge / (float) FULL_CHARGE);
        Abyssorigins.LOGGER.debug("[AbyssOrigins] {} throws the Sovereign Scythe (charge {})", p.getName().getString(), charge);
        stack.hurtAndBreak(1, p, LivingEntity.getSlotForHand(p.getUsedItemHand()));
        ThrownScytheEntity.launch(p, stack, 1.6f + 1.2f * power);
        p.setItemInHand(p.getUsedItemHand(), ItemStack.EMPTY);
    }

    // ---- Ownership and durability ------------------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer p) SovereignScythe.inventoryTick(p, stack);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        SovereignScythe.itemEntityTick(entity);
        return false;
    }

    /** Wears down to its last point and no further. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return Math.max(0, Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue()));
    }

    /** No vanilla sweep: the combo does its own cleaving (ScytheCombat). */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ability != ItemAbilities.SWORD_SWEEP && super.canPerformAction(stack, ability);
    }

    // ---- GeckoLib ----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(GearRenderers.idleController(this));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(GearRenderers.relic(this, "sovereign_scythe"));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object o)                            { return RenderUtil.getCurrentTick(); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        String k = "abyssorigins.weapon.sovereign_scythe";
        lines.add(Gear.boundLine(Eminence.PATH));
        lines.add(Component.empty());
        lines.add(Component.translatable(k + ".chain").withStyle(ChatFormatting.GOLD));
        lines.addAll(Gear.wrap(Component.translatable(k + ".chain.desc"), Style.EMPTY.withColor(ChatFormatting.GRAY), 42));
        lines.add(Component.translatable(k + ".throw").withStyle(ChatFormatting.GOLD));
        lines.addAll(Gear.wrap(Component.translatable(k + ".throw.desc"), Style.EMPTY.withColor(ChatFormatting.GRAY), 42));
        lines.add(Component.translatable(k + ".mire").withStyle(ChatFormatting.DARK_AQUA));
        lines.addAll(Gear.wrap(Component.translatable(k + ".mire.desc"), Style.EMPTY.withColor(ChatFormatting.GRAY), 42));
    }
}
