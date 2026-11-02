package com.gugas749.abyssorigins.api;

/**
 * The two ability loadouts every player has. Each group has a fixed number of slots,
 * the same for every Origin / Class, so new powers only ever need a slot — never a new key.
 *
 *   ORIGIN — five general slots (R V Z B N by default) for any origin power the player knows.
 *   CLASS  — five general slots plus three class-specific slots, which only accept powers
 *            owned by the player's current class.
 *
 * Change the numbers here to grow a loadout; storage, sync, keys and UI all size from them.
 */
public enum SlotGroup {
    ORIGIN(5, 0),
    CLASS(5, 3);

    private final int general;
    private final int specific;

    SlotGroup(int general, int specific) {
        this.general  = general;
        this.specific = specific;
    }

    /** Slots that accept any power of this group the player knows. */
    public int general()  { return general; }
    /** Trailing slots reserved for the owner's own powers (class-specific). */
    public int specific() { return specific; }
    public int size()     { return general + specific; }

    public boolean isSpecific(int slot) { return slot >= general && slot < size(); }
    public boolean valid(int slot)      { return slot >= 0 && slot < size(); }

    public static SlotGroup of(Ability ability) {
        return ability instanceof SpecializationAbility ? CLASS : ORIGIN;
    }
}
