package com.gugas749.abyssorigins.hiddenpower.realm;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.power.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The two private dimensions behind the hidden powers, and safe travel in and out of them.
 *
 * Both are data-driven (data/abyssorigins/dimension/*.json). Neither is a shared space: the
 * pocket realm is split into one far-apart plot per player and the punishment realm into one
 * cell per active punishment, and {@link RealmGuard} keeps everyone inside the area they belong
 * to. Nothing outside the hidden powers can send a player to either (portals refuse them).
 */
public final class Realms {

    public static final ResourceKey<Level> POCKET     = key("pocket");
    public static final ResourceKey<Level> PUNISHMENT = key("punishment");

    private Realms() {}

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, path));
    }

    public static boolean isRealm(ResourceKey<Level> dim) { return dim == POCKET || dim == PUNISHMENT; }
    public static boolean inRealm(ServerPlayer p)          { return isRealm(p.level().dimension()); }

    /** The realm's level, or null (with a message to {@code p}) if its datapack entry is missing. */
    public static ServerLevel level(ServerPlayer p, ResourceKey<Level> key) {
        ServerLevel level = p.server.getLevel(key);
        if (level == null) {
            Abyssorigins.LOGGER.error("[AbyssOrigins] Dimension {} is not loaded - is the mod's data pack disabled?", key.location());
            p.displayClientMessage(Component.translatable("abyssorigins.message.realm_missing"), true);
        }
        return level;
    }

    /** Moves {@code p} with a space-distortion effect at both ends. */
    public static void travel(ServerPlayer p, ServerLevel to, Vec3 pos, float yRot, float xRot) {
        distort(p.serverLevel(), p.position());
        if (p.isPassenger()) p.stopRiding();
        p.teleportTo(to, pos.x, pos.y, pos.z, yRot, xRot);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        distort(to, pos);
    }

    /** Sends {@code p} to a saved location; falls back to world spawn if its dimension is gone. */
    public static void travel(ServerPlayer p, Location loc) {
        ServerLevel level = loc == null ? null : p.server.getLevel(loc.dimension());
        if (level == null || isRealm(loc.dimension())) { travel(p, worldSpawn(p.server)); return; }
        travel(p, level, loc.pos(), loc.yRot(), loc.xRot());
    }

    /** The overworld spawn, on its surface. */
    public static Location worldSpawn(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        BlockPos top = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ow.getSharedSpawnPos());
        return new Location(Level.OVERWORLD, Vec3.atBottomCenterOf(top), 0f, 0f);
    }

    public static void distort(ServerLevel level, Vec3 at) {
        Vec3 c = at.add(0, 1, 0);
        Fx.implode(level, ParticleTypes.REVERSE_PORTAL, c, 1.6, 40);
        Fx.burst(level, ParticleTypes.PORTAL, c, 50, 0.6, 0.4);
        Fx.sound(level, at, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.6f);
    }
}
