package com.gugas749.abyssorigins.hiddenpower.realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A saved spot in some dimension, with facing - where a player returns to after a realm visit. */
public record Location(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot) {

    public static Location of(ServerPlayer p) {
        return new Location(p.level().dimension(), p.position(), p.getYRot(), p.getXRot());
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("dim", dimension.location().toString());
        t.putDouble("x", pos.x);
        t.putDouble("y", pos.y);
        t.putDouble("z", pos.z);
        t.putFloat("yaw", yRot);
        t.putFloat("pitch", xRot);
        return t;
    }

    /** @return null if the tag is malformed. */
    public static Location load(CompoundTag t) {
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
        if (dim == null) return null;
        return new Location(ResourceKey.create(Registries.DIMENSION, dim),
            new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z")), t.getFloat("yaw"), t.getFloat("pitch"));
    }
}
