package com.gugas749.abyssorigins.api;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A declarative attribute modifier attached to an origin, a class, or a skill node.
 * SkillEffectHandler.refresh() rebuilds every bonus from scratch whenever the
 * player's origin, class or unlocked skills change — and after respawn.
 */
public record StatBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {

    public static StatBonus add(Holder<Attribute> attribute, double amount) {
        return new StatBonus(attribute, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Percentage of the base value, e.g. 0.10 = +10%. */
    public static StatBonus percent(Holder<Attribute> attribute, double amount) {
        return new StatBonus(attribute, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
