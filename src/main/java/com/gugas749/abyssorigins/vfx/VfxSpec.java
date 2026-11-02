package com.gugas749.abyssorigins.vfx;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable description of one effect instance — what the server sends and what the
 * client VFX engine instantiates.
 *
 * @param follow entity id to attach to, or -1
 * @param flags  {@link Vfx#HALF}, {@link Vfx#VOID}, {@link Vfx#VERTICAL}, {@link Vfx#STOP}...
 * @param seed   per-instance randomness so every client draws the same shape
 */
public record VfxSpec(VfxType type, Vec3 pos, Vec3 dir, float scale, float extra,
                      int color, int color2, int duration, int follow, int flags, int seed) {

    public boolean has(int flag) { return (flags & flag) != 0; }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        buf.writeDouble(pos.x); buf.writeDouble(pos.y); buf.writeDouble(pos.z);
        buf.writeFloat((float) dir.x); buf.writeFloat((float) dir.y); buf.writeFloat((float) dir.z);
        buf.writeFloat(scale);
        buf.writeFloat(extra);
        buf.writeInt(color);
        buf.writeInt(color2);
        buf.writeVarInt(duration);
        buf.writeVarInt(follow + 1);
        buf.writeVarInt(flags);
        buf.writeInt(seed);
    }

    public static VfxSpec read(FriendlyByteBuf buf) {
        VfxType type = VfxType.values()[buf.readVarInt()];
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 dir = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new VfxSpec(type, pos, dir, buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readInt(),
            buf.readVarInt(), buf.readVarInt() - 1, buf.readVarInt(), buf.readInt());
    }
}
