package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.GearClient;
import com.gugas749.abyssorigins.client.gear.GearRenderers;
import com.gugas749.abysscore.api.geckolib.AbyssGeoBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * A relic weapon bound to one origin. Right-click triggers its power (own cooldown);
 * melee hits trigger its passive (GearEvents). Wielders of another origin are rejected.
 * Rendered by GeckoLib: geo/item/weapon/&lt;id&gt;.geo.json (+ animation, texture, glowmask).
 */
public class OriginWeaponItem extends SwordItem implements OriginBound, AbyssGeoBase, GeoItem {

    private final String origin;
    private final WeaponPower power;
    private final AnimatableInstanceCache cache = createCache();

    /**
     * @param damage extra attack damage on top of the relic tier (sword-style)
     * @param speed  attack speed modifier (sword = -2.4)
     * @param reach  extra entity reach in blocks (scythe, spear)
     */
    public OriginWeaponItem(String origin, WeaponPower power, int damage, float speed, double reach) {
        super(Gear.RELIC, new Properties().rarity(Rarity.EPIC).attributes(attributes(damage, speed, reach)));
        this.origin = origin;
        this.power = power;
        GeoItem.registerSyncedAnimatable(this);
    }

    private static ItemAttributeModifiers attributes(int damage, float speed, double reach) {
        ItemAttributeModifiers mods = SwordItem.createAttributes(Gear.RELIC, damage, speed);
        if (reach > 0)
            mods = mods.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "relic_reach"), reach,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return mods;
    }

    @Override public String originPath() { return origin; }
    public WeaponPower power()            { return power; }

    // ---- GeckoLib ---------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(GearRenderers.idleController(this));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(GearRenderers.relic(this));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object o)                            { return RenderUtil.getCurrentTick(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);

        if (!Gear.attuned(sp, origin)) {
            Gear.reject(sp, this, 2, 1);
            return InteractionResultHolder.fail(stack);
        }
        if (!power.activate(sp, stack)) return InteractionResultHolder.fail(stack);
        sp.getCooldowns().addCooldown(this, power.cooldownTicks());
        stack.hurtAndBreak(2, sp, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        String k = "abyssorigins.weapon." + power.id();
        lines.add(Gear.boundLine(origin));
        lines.add(Component.empty());
        lines.add(Component.translatable("abyssorigins.tooltip.power",
                Component.translatable(k + ".power"), power.cooldownTicks() / 20)
            .withStyle(ChatFormatting.GOLD));
        lines.addAll(Gear.wrap(Component.translatable(k + ".power.desc"), Style.EMPTY.withColor(ChatFormatting.GRAY), 42));
        lines.add(Component.translatable("abyssorigins.tooltip.passive", Component.translatable(k + ".passive"))
            .withStyle(ChatFormatting.DARK_AQUA));
        lines.addAll(Gear.wrap(Component.translatable(k + ".passive.desc"), Style.EMPTY.withColor(ChatFormatting.GRAY), 42));
        if (FMLEnvironment.dist.isClient() && !GearClient.localAttuned(origin)) {
            lines.add(Component.empty());
            lines.add(Component.translatable("abyssorigins.tooltip.rejects_you").withStyle(ChatFormatting.RED));
        }
    }
}
