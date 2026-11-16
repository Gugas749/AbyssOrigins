package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.GearClient;
import com.gugas749.abyssorigins.item.ModDataComponents;
import com.gugas749.abyssorigins.client.gear.GearRenderers;
import com.gugas749.abysscore.api.geckolib.AbyssGeoBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 * Damage, attack speed and reach come from its {@link WeaponClass} at the relic's own
 * {@link RelicMastery}, which also unlocks the passive and the power.
 * Rendered by GeckoLib: geo/item/weapon/&lt;id&gt;.geo.json (+ animation, texture, glowmask).
 */
public class OriginWeaponItem extends SwordItem implements OriginBound, AbyssGeoBase, GeoItem {

    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "relic_reach");

    private final String origin;
    private final WeaponPower power;
    private final WeaponClass type;
    private final AnimatableInstanceCache cache = createCache();

    public OriginWeaponItem(String origin, WeaponPower power, WeaponClass type) {
        super(Gear.RELIC, new Properties().rarity(Rarity.EPIC)
            .component(ModDataComponents.RELIC_MASTERY.get(), RelicMastery.NONE));
        this.origin = origin;
        this.power = power;
        this.type = type;
        GeoItem.registerSyncedAnimatable(this);
    }

    /** The item's own attribute component is left empty, so every stack's stats come from here, by its mastery. */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        int level = RelicMastery.of(stack).level();
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, type.damage(level) - 1,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, type.speed(level) - 4,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        double reach = type.reach(level);
        if (reach > 0)
            b.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID, reach, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        return b.build();
    }

    @Override public String originPath() { return origin; }
    public WeaponPower power()            { return power; }
    public WeaponClass type()             { return type; }

    /** Power cooldown in ticks for this stack (shorter at higher mastery). */
    public int cooldownTicks(ItemStack stack) {
        return Math.round(power.cooldownTicks() * RelicMastery.of(stack).cooldownFactor());
    }

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

    /** "Mastery 12 / 30 ▰▰▰▰▱▱▱▱▱▱", or "Mastered" at the top. */
    private Component masteryLine(RelicMastery m) {
        if (m.maxed()) return Component.literal("✦ ").append(Component.translatable("abyssorigins.mastery.mastered_tag"))
            .withStyle(ChatFormatting.GOLD);
        int filled = Math.round(10f * m.level() / RelicMastery.MAX_LEVEL);
        return Component.literal("✦ ").withStyle(ChatFormatting.YELLOW)
            .append(Component.translatable("abyssorigins.mastery.level", m.level(), RelicMastery.MAX_LEVEL).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("  " + "▰".repeat(filled)).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("▱".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);

        if (!Gear.attuned(sp, origin)) {
            Gear.reject(sp, this, 2, 1);
            return InteractionResultHolder.fail(stack);
        }
        if (com.gugas749.abyssorigins.power.Nullification.is(sp)) {
            sp.displayClientMessage(Component.translatable("abyssorigins.message.nullified"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!RelicMastery.of(stack).powerUnlocked()) {
            sp.displayClientMessage(Component.translatable("abyssorigins.mastery.power_locked",
                Component.translatable("abyssorigins.weapon." + power.id() + ".power"), RelicMastery.POWER_LEVEL)
                .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!power.activate(sp, stack)) return InteractionResultHolder.fail(stack);
        sp.getCooldowns().addCooldown(this, cooldownTicks(stack));
        RelicMastery.grant(sp, stack, RelicMastery.XP_POWER);
        stack.hurtAndBreak(2, sp, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        String k = "abyssorigins.weapon." + power.id();
        RelicMastery m = RelicMastery.of(stack);
        lines.add(Gear.boundLine(origin));
        lines.add(masteryLine(m));
        if (!m.maxed())
            lines.add(Component.translatable("abyssorigins.mastery.xp", m.xp(), RelicMastery.xpForLevel(m.level()))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (FMLEnvironment.dist.isClient() && !m.maxed() && m.level() >= RelicMastery.capFor(GearClient.localOriginLevel()))
            lines.add(Component.translatable("abyssorigins.mastery.capped_short", RelicMastery.UNASCENDED_CAP,
                com.gugas749.abyssorigins.api.Ascension.NOBLE_LEVEL).withStyle(ChatFormatting.RED));
        lines.add(Component.empty());

        Style dim = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
        float cd = cooldownTicks(stack) / 20f;
        String cooldown = cd == Math.rint(cd) ? String.valueOf((int) cd) : String.format(java.util.Locale.ROOT, "%.1f", cd);
        MutableComponent powerLine = Component.translatable("abyssorigins.tooltip.power", Component.translatable(k + ".power"), cooldown)
            .withStyle(m.powerUnlocked() ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY);
        if (!m.powerUnlocked()) powerLine.append(Component.translatable("abyssorigins.mastery.locked", RelicMastery.POWER_LEVEL));
        lines.add(powerLine);
        lines.addAll(Gear.wrap(Component.translatable(k + ".power.desc"), m.powerUnlocked() ? Style.EMPTY.withColor(ChatFormatting.GRAY) : dim, 42));

        MutableComponent passiveLine = Component.translatable("abyssorigins.tooltip.passive", Component.translatable(k + ".passive"))
            .withStyle(m.passiveUnlocked() ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GRAY);
        if (!m.passiveUnlocked()) passiveLine.append(Component.translatable("abyssorigins.mastery.locked", RelicMastery.PASSIVE_LEVEL));
        lines.add(passiveLine);
        lines.addAll(Gear.wrap(Component.translatable(k + ".passive.desc"), m.passiveUnlocked() ? Style.EMPTY.withColor(ChatFormatting.GRAY) : dim, 42));

        if (!m.maxed()) {
            int next = m.level() < RelicMastery.PASSIVE_LEVEL ? RelicMastery.PASSIVE_LEVEL
                : m.level() < RelicMastery.POWER_LEVEL ? RelicMastery.POWER_LEVEL
                : m.level() < RelicMastery.HONED_LEVEL ? RelicMastery.HONED_LEVEL : RelicMastery.MAX_LEVEL;
            String what = next == RelicMastery.PASSIVE_LEVEL ? "passive" : next == RelicMastery.POWER_LEVEL ? "power"
                : next == RelicMastery.HONED_LEVEL ? "honed" : "mastered";
            lines.add(Component.translatable("abyssorigins.mastery.next." + what, next).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (FMLEnvironment.dist.isClient() && !GearClient.localAttuned(origin)) {
            lines.add(Component.empty());
            lines.add(Component.translatable("abyssorigins.tooltip.rejects_you").withStyle(ChatFormatting.RED));
        }
    }
}
