package com.gugas749.abyssorigins.vfx;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.resources.ResourceLocation;

/**
 * AbyssOrigins' own 3D effects on AbyssCore's VFX engine. The server plays them with
 * {@code AbyssVfx.of(OriginVfx.X)}; the client draws them with the effects in client/vfx, registered by
 * ClientEventHandler. Everything else uses AbyssCore's built-in {@code VfxType}s.
 */
public final class OriginVfx {

    /**
     * The Eminence's I Am Atomic, start to finish on the ability's timeline: converging energy, the atom
     * above the caster, the cut, then the explosion (flash, dome, ground wave, column and cap).
     * pos = caster's feet, scale = blast radius, extra = atom height, duration = whole timeline.
     */
    public static final ResourceLocation ATOMIC = id("atomic");

    /**
     * An origin ascension ceremony on the stage's timeline (api.Ascension): energy gathering in the origin's
     * motif, the burst, and a halo (Noble) or crown (Lord) afterwards. follow = the ascending player,
     * pos = their feet at the start, scale = stage, extra = Ascension.Motif ordinal, duration = whole timeline.
     */
    public static final ResourceLocation ASCENSION = id("ascension");

    private OriginVfx() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path);
    }
}
