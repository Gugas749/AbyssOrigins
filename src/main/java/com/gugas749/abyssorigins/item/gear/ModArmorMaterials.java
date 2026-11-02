package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One armor material per origin outfit. Textures: textures/models/armor/&lt;set&gt;_layer_1/2.png */
public final class ModArmorMaterials {

    private static final DeferredRegister<ArmorMaterial> MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, Abyssorigins.MODID);

    public static final Map<ArmorSet, DeferredHolder<ArmorMaterial, ArmorMaterial>> BY_SET = new EnumMap<>(ArmorSet.class);

    static {
        for (ArmorSet set : ArmorSet.values())
            BY_SET.put(set, MATERIALS.register(set.id, () -> create(set)));
    }

    private ModArmorMaterials() {}

    private static ArmorMaterial create(ArmorSet set) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, set.defense[0]);
        defense.put(ArmorItem.Type.CHESTPLATE, set.defense[1]);
        defense.put(ArmorItem.Type.LEGGINGS, set.defense[2]);
        defense.put(ArmorItem.Type.BOOTS, set.defense[3]);
        defense.put(ArmorItem.Type.BODY, set.defense[1]);
        return new ArmorMaterial(defense, set.enchantability, set.equipSound.get(),
            () -> Ingredient.of(set.repair.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, set.id))),
            set.toughness, set.knockbackResistance);
    }

    public static void register(IEventBus bus) { MATERIALS.register(bus); }
}
