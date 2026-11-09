package com.gugas749.abyssorigins.api;

/**
 * The two ability loadouts every player has. Each group has a fixed number of slots, the same
 * for every Origin / Class, so new powers only ever need a slot — never a new key.
 *
 *   ORIGIN — 5 slots (R V Z B N by default) for any origin power the player knows. Hidden
 *            powers ({@link HiddenAbility}) also live here, so they count against the same cap.
 *   CLASS  — 3 slots (F G + one unbound) for the powers of the player's current class.
 *
 * At most {@link #MAX_ACTIVE} powers can be active (bound) at once. Change the numbers here to
 * resize a loadout; storage, sync, keys and UI all size from them.
 */
public enum SlotGroup {
    ORIGIN(5),
    CLASS(3);

    /** Every slot of every loadout: the most powers a player can have bound at once. */
    public static final int MAX_ACTIVE = ORIGIN.size + CLASS.size;

    private final int size;

    SlotGroup(int size) { this.size = size; }

    public int size()              { return size; }
    public boolean valid(int slot) { return slot >= 0 && slot < size; }

    /** Class powers go in the class loadout; origin and hidden powers in the origin loadout. */
    public static SlotGroup of(Ability ability) {
        return ability instanceof SpecializationAbility ? CLASS : ORIGIN;
    }
}
