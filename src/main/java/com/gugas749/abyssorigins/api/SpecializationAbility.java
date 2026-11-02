package com.gugas749.abyssorigins.api;

/**
 * An ability granted by a Class (Specialization). Bound into a class slot ({@link SlotGroup#CLASS}).
 * Class abilities never cost origin resource — they are gated by cooldown only.
 */
public abstract class SpecializationAbility extends Ability {

    public SpecializationAbility(String id, String nameKey, String descKey, int cooldownSeconds) {
        super(id, nameKey, descKey, cooldownSeconds, 0);
    }
}
