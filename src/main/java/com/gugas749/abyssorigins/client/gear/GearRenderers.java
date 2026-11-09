package com.gugas749.abyssorigins.client.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.item.gear.OriginArmorItem;
import com.gugas749.abyssorigins.item.gear.OriginWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderers for the origin relics and outfits.
 *
 * Asset layout (all under assets/abyssorigins, replaceable with Blockbench exports):
 *   weapon: geo/item/weapon/ID.geo.json, animations/item/weapon/ID.animation.json, textures/item/weapon/ID(_glowmask).png
 *   outfit: geo/item/armor/SET.geo.json (bones armorHead/Body/RightArm/LeftArm/RightLeg/LeftLeg/RightBoot/LeftBoot),
 *           animations/item/armor/SET.animation.json, textures/item/armor/SET(_glowmask).png
 *   icon:   geo/item/armor_icon/SET_PIECE.geo.json — uses the outfit's texture and animation
 * Every animation file must define a looping "idle" animation.
 * The _glowmask texture is optional: without one the gear simply doesn't glow (GeckoLib's glow
 * layer is only added when the mask exists — added without one, it renders the model black).
 */
public final class GearRenderers {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private GearRenderers() {}

    public static <T extends GeoItem> AnimationController<T> idleController(T item) {
        return new AnimationController<>(item, "idle", 0, state -> state.setAndContinue(IDLE));
    }

    public static GeoRenderProvider relic(OriginWeaponItem item) {
        return new GeoRenderProvider() {
            private GeoItemRenderer<OriginWeaponItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new DefaultedItemGeoModel<OriginWeaponItem>(rl("weapon/" + id(item))));
                    if (hasGlowmask("weapon/" + id(item))) renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
                }
                return renderer;
            }
        };
    }

    public static GeoRenderProvider outfit(OriginArmorItem item) {
        return new GeoRenderProvider() {
            private GeoArmorRenderer<OriginArmorItem> armor;
            private GeoItemRenderer<OriginArmorItem> icon;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<T> original) {
                if (armor == null) {
                    armor = new GeoArmorRenderer<>(new DefaultedItemGeoModel<OriginArmorItem>(rl("armor/" + item.set().id)));
                    if (hasGlowmask("armor/" + item.set().id)) armor.addRenderLayer(new AutoGlowingGeoLayer<>(armor));
                }
                return armor;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (icon == null) {
                    ResourceLocation shared = rl("armor/" + item.set().id);
                    icon = new GeoItemRenderer<>(new DefaultedItemGeoModel<OriginArmorItem>(
                            rl("armor_icon/" + item.set().id + "_" + item.getType().getName()))
                        .withAltTexture(shared).withAltAnimations(shared));
                    if (hasGlowmask("armor/" + item.set().id)) icon.addRenderLayer(new AutoGlowingGeoLayer<>(icon));
                }
                return icon;
            }
        };
    }

    /** True if textures/item/PATH_glowmask.png exists. */
    private static boolean hasGlowmask(String path) {
        return Minecraft.getInstance().getResourceManager()
            .getResource(rl("textures/item/" + path + "_glowmask.png")).isPresent();
    }

    private static String id(OriginWeaponItem item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path);
    }
}
