package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;

/**
 * A bonus a Class grants when paired with a specific Origin — the glue that makes
 * origin + class combinations matter mechanically. Its effect is implemented in the
 * class's PowerHooks, gated by Specialization.hasResonance(player).
 */
public record Resonance(String originPath, Component name, Component description) {

    public static Resonance of(String originPath, String nameKey, String descKey) {
        return new Resonance(originPath, Component.translatable(nameKey), Component.translatable(descKey));
    }
}
