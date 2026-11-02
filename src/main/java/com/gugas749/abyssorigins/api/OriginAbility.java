package com.gugas749.abyssorigins.api;

/**
 * An ability granted by an Origin. Bound into one of the five origin slots ({@link SlotGroup#ORIGIN}).
 * May consume the origin's resource (blood, wrath, radiance...).
 */
public abstract class OriginAbility extends Ability {

    /**
     * @param nameKey  Lang key for the ability name
     * @param descKey  Lang key for the ability description
     */
    public OriginAbility(String id, String nameKey, String descKey,
                         int cooldownSeconds, int resourceCost) {
        super(id, nameKey, descKey, cooldownSeconds, resourceCost);
    }
}
