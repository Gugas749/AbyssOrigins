package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.gear.GearRenderers;
import com.gugas749.abyssorigins.power.Delayed;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Nullification;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The Inverted Spear of Heaven - a forked, chained blade that disables powers near it.
 * Never crafted; bound to no origin.
 *
 *  - Held in the main hand it projects a null field ({@link Nullification}): every player within {@link Nullification#RADIUS} blocks,
 *    the wielder included, loses access to their skills, passives and powers while near it.
 *  - Use: Heavenly Severance - the wielder dashes up to {@link #DASH} blocks ahead along a white crescent
 *    that damages everything in between (5s cooldown).
 *  - It never breaks.
 * Model: geo/item/weapon/inverted_spear_of_heaven (source: blockbench/weapon/inverted_spear_of_heaven.bbmodel), animations idle + slash.
 */
public class InvertedSpearItem extends SwordItem implements GeoItem {

    /** Damage 1 + 3 + 12 = 16, speed 4 - 2.2 = 1.8, reach +0.5. */
    private static final int DAMAGE = 12;
    private static final float SPEED = -2.2f;
    private static final double REACH = 0.5, DASH = 10;
    private static final int COOLDOWN = 100, WIND_UP = 3;
    public static final int STEEL = 0xE6EAF0, STEEL_HI = 0xFFFFFF;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SLASH = RawAnimation.begin().thenPlay("slash");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public InvertedSpearItem() {
        super(Gear.RELIC, new Item.Properties().rarity(Rarity.EPIC).fireResistant().attributes(attributes()));
        GeoItem.registerSyncedAnimatable(this);
    }

    private static ItemAttributeModifiers attributes() {
        return SwordItem.createAttributes(Gear.RELIC, DAMAGE, SPEED)
            .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "inverted_spear_reach"), REACH,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    // ---- Heavenly Severance ---------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);

        ServerLevel sl = sp.serverLevel();
        triggerAnim(sp, GeoItem.getOrAssignId(stack, sl), "main", "slash");
        sp.swing(hand, true);
        sp.getCooldowns().addCooldown(this, COOLDOWN);
        Fx.sound(sl, sp.position(), SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.8f, 1.7f);
        Delayed.schedule(WIND_UP, () -> { if (sp.isAlive() && sp.level() == sl) severance(sp); });
        return InteractionResultHolder.success(stack);
    }

    /** The dash: move the body along the look direction until something blocks it. */
    private static void severance(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = p.getLookAngle();
        Vec3 start = p.position();
        Vec3 dest = start;
        for (double d = 0.25; d <= DASH; d += 0.25) {
            Vec3 at = start.add(dir.scale(d));
            if (!level.noCollision(p, p.getBoundingBox().move(at.subtract(start)))) break;
            dest = at;
        }
        double dist = start.distanceTo(dest);
        Vec3 chestFrom = Fx.chest(p), chestTo = dest.add(0, p.getBbHeight() * 0.55, 0);

        // Everything along the path is hit once
        float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB path = p.getBoundingBox().minmax(p.getBoundingBox().move(dest.subtract(start))).inflate(1.0);
        Set<Integer> hit = new HashSet<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> Targeting.isPrey(p, e))) {
            if (!hit.add(e.getId())) continue;
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.push(dir.x * 0.4, 0.25, dir.z * 0.4);
            e.hurtMarked = true;
            AbyssVfx.of(VfxType.SLASH).at(Fx.chest(e)).dir(dir).scale(1.2f).extra(150).colors(STEEL, STEEL_HI)
                .flags(AbyssVfx.VERTICAL).duration(9).send(level);
            AbyssVfx.sparks(level, Fx.chest(e), 0.9f, STEEL, 0xB8C0C8);
        }

        // Afterimage at the start and the crescent along the path
        AbyssVfx.of(VfxType.SLASH).at(chestFrom).dir(dir).scale(1.6f).extra(170).colors(STEEL, STEEL_HI).duration(9).send(level);
        AbyssVfx.of(VfxType.CRESCENT).at(chestFrom).dir(dir).scale((float) Math.max(2, dist + 1.5)).extra(1.5f)
            .colors(STEEL, STEEL_HI).flags(AbyssVfx.VERTICAL).duration(12).send(level);
        if (dist > 0.5) AbyssVfx.beam(level, chestFrom, chestTo, 0.12f, 0x9AA2AE, STEEL_HI, 8);
        AbyssVfx.burst(level, chestFrom, 0.6f, 0x8E949C, STEEL_HI);

        if (dist > 0.1) {
            p.teleportTo(dest.x, dest.y, dest.z);
            p.fallDistance = 0;
        }
        AbyssVfx.of(VfxType.SLASH).at(chestTo).dir(dir).scale(2.0f).extra(200).colors(STEEL, STEEL_HI).flags(AbyssVfx.REVERSE).duration(10).send(level);
        AbyssVfx.of(VfxType.SPARKS).at(chestTo).dir(dir.scale(0.4)).scale(1.4f).colors(STEEL, 0xB8C0C8).send(level);
        AbyssVfx.shockwave(level, dest, 2.2f, 0xB8C0C8, STEEL_HI, 10);
        Fx.sound(level, dest, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        Fx.sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);
        Fx.sound(level, dest, SoundEvents.CHAIN_BREAK, 0.8f, 0.8f);
    }

    /** Give the stack its GeckoLib id while it is merely carried, so the first slash already animates on clients. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel sl && entity.tickCount % 20 == 0) GeoItem.getOrAssignId(stack, sl);
    }

    /** It never takes durability damage. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return 0;
    }

    // ---- GeckoLib ----------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> state.setAndContinue(IDLE))
            .triggerableAnim("slash", SLASH));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(GearRenderers.relic(this, "inverted_spear_of_heaven"));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object o)                            { return RenderUtil.getCurrentTick(); }

    // ---- Lore ------------------------------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        String k = "abyssorigins.weapon.inverted_spear_of_heaven";
        Style lore = Style.EMPTY.withColor(0x9AA2AE).withItalic(true);
        Style grey = Style.EMPTY.withColor(ChatFormatting.GRAY);
        lines.addAll(Gear.wrap(Component.translatable(k + ".lore"), lore, 42));
        lines.add(Component.empty());
        lines.add(Component.translatable(k + ".null").withStyle(ChatFormatting.WHITE));
        lines.addAll(Gear.wrap(Component.translatable(k + ".null.desc", (int) Nullification.RADIUS), grey, 42));
        lines.add(Component.translatable(k + ".severance", COOLDOWN / 20).withStyle(ChatFormatting.GOLD));
        lines.addAll(Gear.wrap(Component.translatable(k + ".severance.desc", (int) DASH), grey, 42));
        lines.add(Component.translatable(k + ".unbreakable").withStyle(ChatFormatting.DARK_GRAY));
    }
}
