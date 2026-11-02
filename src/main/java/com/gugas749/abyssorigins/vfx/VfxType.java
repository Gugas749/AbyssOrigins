package com.gugas749.abyssorigins.vfx;

/**
 * Every 3D effect the client VFX engine can draw. Parameter meaning per type
 * (see {@link VfxSpec}): pos / dir / scale / extra / colors / duration / follow.
 */
public enum VfxType {
    /** Expanding ground ring + rising wall + flash + sparks. scale = max radius. */
    SHOCKWAVE,
    /** Energy-field sphere (hemisphere with {@link Vfx#HALF}). scale = radius; may follow an entity. */
    DOME,
    /** Column of light from pos upward. scale = height, extra = radius. */
    PILLAR,
    /** Travelling energy beam from pos to dir (absolute target point). extra = width. */
    BEAM,
    /** Crescent blade sweep around pos facing dir. scale = radius, extra = sweep degrees. {@link Vfx#VERTICAL} for an upright slash. */
    SLASH,
    /** Twin ribbons spiralling up around pos / follow. scale = height. */
    HELIX,
    /** Rotating rune circle on the ground. scale = radius, extra = star points. */
    SIGIL,
    /** Pair of feathered wings attached to follow. {@link Vfx#VOID} for membranous bat wings. */
    WINGS,
    /** Rift vortex: spiral accretion arms, lensing rings and a black core. scale = radius. */
    VORTEX,
    /** Impact: flash, star rays, expanding shell and sparks. scale = size. */
    BURST,
    /** Forked lightning from pos to dir (absolute). */
    BOLT,
    /** Shadow tendrils erupting from the ground at pos. scale = height, extra = count. */
    TENDRILS,
    /** Glowing ribbon trailing behind follow. scale = width. */
    TRAIL,
    /** Rotating reticle floating above follow. */
    MARK,
    /** Spray of glowing streaks. scale = count multiplier, dir = bias velocity. */
    SPARKS,
    /** Vertical portal slit at pos facing dir. scale = height. */
    RIFT,
    /** Lingering ground ring + rising wisps around follow. scale = radius. {@link Vfx#VOID} for smoke. */
    AURA
}
