package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.client.GearClient;
import com.gugas749.abyssorigins.client.gear.GearRenderers;
import com.gugas749.abysscore.api.geckolib.AbyssGeoBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * One piece of an origin outfit. Wearing all four (as that origin) grants the set bonus.
 * Worn: GeckoLib armor model geo/item/armor/&lt;set&gt;.geo.json (one model, bones shown per slot).
 * Inventory: geo/item/armor_icon/&lt;set&gt;_&lt;piece&gt;.geo.json sharing the set's texture and animation.
 */
public class OriginArmorItem extends ArmorItem implements OriginBound, AbyssGeoBase, GeoItem {

    private final ArmorSet set;
    private final AnimatableInstanceCache cache = createCache();

    public OriginArmorItem(ArmorSet set, ArmorItem.Type type) {
        super(ModArmorMaterials.BY_SET.get(set), type,
            new Properties().rarity(Rarity.RARE).durability(type.getDurability(set.durabilityMultiplier)));
        this.set = set;
        GeoItem.registerSyncedAnimatable(this);
    }

    public ArmorSet set()               { return set; }
    @Override public String originPath() { return set.origin; }

    // ---- GeckoLib ---------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(GearRenderers.idleController(this));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(GearRenderers.outfit(this));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object o)                            { return RenderUtil.getCurrentTick(); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        String k = "abyssorigins.armor_set." + set.id;
        lines.add(Gear.boundLine(set.origin));
        lines.add(Component.empty());
        int worn = FMLEnvironment.dist.isClient() ? GearClient.localWorn(set) : 0;
        lines.add(Component.translatable("abyssorigins.tooltip.set_bonus", Component.translatable(k + ".bonus"), worn)
            .withStyle(worn == 4 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        lines.addAll(Gear.wrap(Component.translatable(k + ".bonus.desc"),
            Style.EMPTY.withColor(worn == 4 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY), 42));
        if (FMLEnvironment.dist.isClient() && !GearClient.localAttuned(set.origin)) {
            lines.add(Component.empty());
            lines.add(Component.translatable("abyssorigins.tooltip.rejects_you").withStyle(ChatFormatting.RED));
        }
    }
}
