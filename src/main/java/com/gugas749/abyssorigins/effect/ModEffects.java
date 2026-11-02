package com.gugas749.abyssorigins.effect;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.effect.AbyssMobEffect.Kind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom buffs and debuffs shared by every origin and class.
 *
 *  Bleeding     — armour-ignoring damage over time (Vampire, Assassin, Warrior)
 *  Hexed        — takes +15% damage per level from all sources (Occultist, Assassin)
 *  Sundered     — -2 armour per level (Voidborn, Warrior)
 *  Sanctified   — takes -20% damage per level (Seraph, Guardian)
 *  Hunter's Mark— takes +25% projectile damage; level II: +15% from everything (Ranger)
 *  Stunned      — cannot move or jump, mobs lose their target (Guardian, Shade)
 *  Awed         — held in place by the Eminence's Presence while choosing / resisting
 *  Kneeling     — knelt before the Eminence: frozen, vision darkened, trembling (Presence)
 *  Sealed       — origin and class abilities cannot be used (Royal Decree)
 */
public class ModEffects {

    private static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, Abyssorigins.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0x9E1020, Kind.BLEEDING, ParticleTypes.CRIMSON_SPORE));

    public static final DeferredHolder<MobEffect, MobEffect> HEXED = EFFECTS.register("hexed",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0x8A2BE2, Kind.MARKER, ParticleTypes.WITCH));

    public static final DeferredHolder<MobEffect, MobEffect> SUNDERED = EFFECTS.register("sundered",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0x6B4FA8, Kind.MARKER)
            .addAttributeModifier(Attributes.ARMOR, rl("effect.sundered"), -2.0, AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredHolder<MobEffect, MobEffect> SANCTIFIED = EFFECTS.register("sanctified",
        () -> new AbyssMobEffect(MobEffectCategory.BENEFICIAL, 0xF6D77A, Kind.MARKER, ParticleTypes.END_ROD));

    public static final DeferredHolder<MobEffect, MobEffect> HUNTERS_MARK = EFFECTS.register("hunters_mark",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0x3FB56A, Kind.MARKER));

    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = EFFECTS.register("stunned",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0xC9C3E6, Kind.STUNNED)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, rl("effect.stunned_speed"), -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, rl("effect.stunned_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> AWED = EFFECTS.register("awed",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0xE8C872, Kind.STUNNED)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, rl("effect.awed_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, rl("effect.awed_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> KNEELING = EFFECTS.register("kneeling",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0x3A2A10, Kind.STUNNED)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, rl("effect.kneeling_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, rl("effect.kneeling_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> SEALED = EFFECTS.register("sealed",
        () -> new AbyssMobEffect(MobEffectCategory.HARMFUL, 0xF2C14E, Kind.MARKER, ParticleTypes.WAX_ON));

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path);
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
