package com.gugas749.abyssorigins.item.gear;

import net.minecraft.util.Mth;

/**
 * Relic weapon types and how their stats grow with mastery, from mastery 0 to {@link RelicMastery#MAX_LEVEL}.
 * Damage and attack speed are the totals the wielder ends up with (a bare hand is 1 damage, 4 speed);
 * reach is extra entity reach in blocks. Light weapons are fast and hit softer, heavy ones are slow and hit
 * harder: the dagger tops out at 2.3 attacks a second, the heavy cleaver at 30 damage.
 */
public enum WeaponClass {
    //          damage      speed        reach
    DAGGER     ( 5, 16,     1.90, 2.30,  0.0, 0.5),
    SWORD      ( 8, 22,     1.40, 1.80,  0.5, 1.0),
    SPEAR      ( 7, 20,     1.50, 1.75,  1.5, 3.0),
    SCYTHE     ( 9, 25,     1.00, 1.35,  1.0, 2.0),
    GREATSWORD (10, 27,     0.90, 1.15,  0.5, 1.25),
    HEAVY      (10, 30,     0.80, 1.00,  0.0, 0.5);

    private final double damage0, damageMax, speed0, speedMax, reach0, reachMax;

    WeaponClass(double damage0, double damageMax, double speed0, double speedMax, double reach0, double reachMax) {
        this.damage0 = damage0; this.damageMax = damageMax;
        this.speed0 = speed0; this.speedMax = speedMax;
        this.reach0 = reach0; this.reachMax = reachMax;
    }

    private static double f(int level) { return Mth.clamp(level / (double) RelicMastery.MAX_LEVEL, 0, 1); }

    /** Total attack damage at {@code level}, rounded to half points. */
    public double damage(int level) { return Math.round(Mth.lerp(f(level), damage0, damageMax) * 2) / 2.0; }

    /** Total attack speed (attacks per second) at {@code level}. */
    public double speed(int level)  { return Math.round(Mth.lerp(f(level), speed0, speedMax) * 100) / 100.0; }

    /** Extra entity reach in blocks at {@code level}. */
    public double reach(int level)  { return Math.round(Mth.lerp(f(level), reach0, reachMax) * 100) / 100.0; }
}
