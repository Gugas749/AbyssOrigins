package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.ScytheData;
import com.gugas749.abyssorigins.entity.AbyssMireEntity;
import com.gugas749.abyssorigins.entity.ModEntities;
import com.gugas749.abyssorigins.entity.ThrownScytheEntity;
import com.gugas749.abyssorigins.item.ModDataComponents;
import com.gugas749.abyssorigins.item.ModDataComponents.ScytheBond;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.power.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Eminence's Sovereign Scythe - summoned and stored by Mire Call instead of kept in an inventory.
 *
 *  - Mire Call (the ability) opens a mud pool beside the Eminence and the scythe rises out of it into their
 *    hand. Cast again while carrying it to put it back.
 *  - Only one scythe exists per Eminence. Every summon bumps a generation counter that the live copy carries
 *    (ScytheBond); a copy from an older summon, someone else's, or one held by a player who is no longer the
 *    Eminence is deleted as soon as it is held.
 *  - If it leaves the inventory (tossed, dropped on death, thrown into a wall) it sinks into a mire pool where
 *    it lands and is stored, enchantments included ({@link ScytheData}). It is repaired on every summon.
 */
public final class SovereignScythe {

    /** How far from the caster's side the mire opens. */
    private static final double SIDE = 1.5;
    /** The rising scythe is handed over only if its owner stayed this close. */
    public static final double DELIVER_RANGE = 12;

    private SovereignScythe() {}

    public static ScytheData data(Player p) { return p.getData(OriginAttachments.SCYTHE); }

    public static boolean isScythe(ItemStack s) { return s.is(ModItems.SOVEREIGN_SCYTHE.get()); }

    /** The live, current copy of {@code p}'s scythe (right owner, latest summon). */
    public static boolean bonded(Player p, ItemStack s) {
        ScytheBond b = s.get(ModDataComponents.SCYTHE_BOND.get());
        return b != null && b.owner().equals(p.getUUID()) && b.generation() == data(p).generation();
    }

    @Nullable
    public static ServerPlayer owner(MinecraftServer server, @Nullable ScytheBond bond) {
        return bond == null || server == null ? null : server.getPlayerList().getPlayer(bond.owner());
    }

    // ---- Mire Call ------------------------------------------------------------------------------

    /** The ability: recall the scythe if it is out, otherwise summon it. */
    public static boolean call(ServerPlayer p) {
        ServerLevel level = p.serverLevel();

        // Carried: put it away
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isScythe(s) && bonded(p, s)) {
                inv.setItem(i, ItemStack.EMPTY);
                store(p, s);
                Vec3 at = sideSpot(p);
                Vec3 hand = Fx.hand(p).subtract(0, 0.6, 0);
                AbyssMireEntity.absorb(level, p.getUUID(), at, s, hand.subtract(at), p.getYRot(), 25f);
                return true;
            }
        }

        // Rising already
        if (!level.getEntities(ModEntities.ABYSS_MIRE.get(), m -> m.summoning(p)).isEmpty()) {
            p.displayClientMessage(Component.translatable("abyssorigins.scythe.rising").withStyle(ChatFormatting.DARK_GRAY), true);
            return false;
        }

        // In flight: it sinks where it is, and a new pool brings it to your side
        for (ThrownScytheEntity t : level.getEntities(ModEntities.THROWN_SCYTHE.get(), t -> t.ownedBy(p)))
            t.sinkHere();

        ScytheData d = data(p);
        ItemStack stack = d.stored().isEmpty() ? new ItemStack(ModItems.SOVEREIGN_SCYTHE.get()) : d.stored().copy();
        stack.setDamageValue(0);                                                      // repaired on summon
        stack.set(ModDataComponents.SCYTHE_BOND.get(), new ScytheBond(p.getUUID(), d.nextGeneration()));
        d.store(stack);
        AbyssMireEntity.summon(p, sideSpot(p), stack);
        return true;
    }

    /** The rising scythe reaches its owner's hand. False if they can't take it (it stays stored). */
    public static boolean deliver(ServerPlayer p, ItemStack stack) {
        if (!p.isAlive() || !attuned(p) || !bonded(p, stack)) return false;
        ItemStack s = stack.copyWithCount(1);
        Inventory inv = p.getInventory();
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty()) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
        } else {
            int free = inv.getFreeSlot();
            if (free >= 0) {                                     // the scythe takes the hand, the rest moves aside
                inv.setItem(free, held);
                p.setItemInHand(InteractionHand.MAIN_HAND, s);
            } else if (!inv.add(s)) {
                p.displayClientMessage(Component.translatable("abyssorigins.scythe.no_room").withStyle(ChatFormatting.DARK_GRAY), true);
                return false;
            }
        }
        ServerLevel level = p.serverLevel();
        Vec3 hand = Fx.hand(p);
        level.sendParticles(ParticleTypes.SQUID_INK, hand.x, hand.y, hand.z, 5, 0.2, 0.25, 0.2, 0.02);
        level.sendParticles(Fx.dust(Eminence.COLOR, 0.9f), hand.x, hand.y, hand.z, 8, 0.3, 0.3, 0.3, 0.01);
        Fx.sound(level, p.position(), SoundEvents.TRIDENT_RETURN, 1f, 0.6f);
        Fx.sound(level, p.position(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1f, 0.7f);
        return true;
    }

    /** Store {@code stack} as the scythe's saved state - only if it is the current copy. */
    public static void store(ServerPlayer owner, ItemStack stack) {
        if (bonded(owner, stack)) data(owner).store(stack);
    }

    private static boolean attuned(Player p) {
        return Eminence.is(p) || p.isCreative();
    }

    // ---- Item lifecycle ---------------------------------------------------------------------------

    /** Every tick it sits in a player's inventory (server side). */
    public static void inventoryTick(ServerPlayer p, ItemStack stack) {
        ScytheBond b = stack.get(ModDataComponents.SCYTHE_BOND.get());
        if (b == null) {
            if (!p.isCreative()) dissolve(p, stack, "unbound");   // a scythe nobody summoned
            return;
        }
        if (!b.owner().equals(p.getUUID())) {                     // wrong owner
            dissolve(p, stack, "not the owner");
            return;
        }
        if (b.generation() != data(p).generation()) {             // copy from an older summon
            dissolve(p, stack, "stale generation " + b.generation() + " != " + data(p).generation());
            return;
        }
        if (!attuned(p)) {                                        // no longer the Eminence: store it and remove it
            store(p, stack);
            dissolve(p, stack, "not the Eminence");
            return;
        }
        // Remember enchantments, renames... as they happen
        if (p.tickCount % 10 == 0 && !ItemStack.isSameItemSameComponents(stack, data(p).stored()))
            data(p).store(stack);
    }

    /** Dropped, tossed or spilled on death: it can't be picked up and sinks into a mire where it lands. */
    public static void itemEntityTick(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level)) return;
        ItemStack stack = item.getItem();
        item.setNeverPickUp();
        ScytheBond b = stack.get(ModDataComponents.SCYTHE_BOND.get());
        ServerPlayer owner = owner(level.getServer(), b);
        if (owner == null || !bonded(owner, stack)) {
            Abyssorigins.LOGGER.debug("[AbyssOrigins] Sovereign Scythe item fizzled (owner {})", owner == null ? "offline" : "stale copy");
            fizzle(level, item.position());
            item.discard();
            return;
        }
        boolean landed = item.onGround() || item.isInWater() || item.isInLava() || item.getAge() > 40
            || item.getY() < level.getMinBuildHeight();
        if (!landed) return;
        Abyssorigins.LOGGER.debug("[AbyssOrigins] Sovereign Scythe dropped by {} sinks into the mire", owner.getName().getString());
        store(owner, stack);
        Vec3 ground = ground(level, item.position(), 8);
        if (ground == null) ground = item.position();
        AbyssMireEntity.absorb(level, owner.getUUID(), ground, stack, item.position().subtract(ground).add(0, 0.2, 0), item.getYRot(), 90f);
        item.discard();
    }

    private static void dissolve(ServerPlayer p, ItemStack stack, String why) {
        Abyssorigins.LOGGER.debug("[AbyssOrigins] Sovereign Scythe dissolved in {}'s hands: {}", p.getName().getString(), why);
        stack.setCount(0);
        fizzle(p.serverLevel(), Fx.chest(p));
    }

    private static void fizzle(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 16, 0.3, 0.4, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 6, 0.2, 0.3, 0.2, 0.01);
        Fx.sound(level, at, SoundEvents.MUD_BREAK, 1f, 0.6f);
    }

    // ---- Placement --------------------------------------------------------------------------------

    /** Ground beside the caster's right hand, a little ahead; the caster's own feet if that's walled off. */
    public static Vec3 sideSpot(ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Level level = p.level();
        Vec3 spot = p.position().add(right.scale(SIDE)).add(ahead.scale(0.5));
        BlockPos feet = BlockPos.containing(spot.x, p.getY() + 0.1, spot.z);
        if (solid(level, feet) || solid(level, feet.above())) spot = p.position();
        Vec3 g = ground(level, spot.add(0, 0.5, 0), 5);
        return g != null ? g : p.position();
    }

    /** Top of the first solid block at or below {@code from} (within {@code depth} blocks). */
    @Nullable
    public static Vec3 ground(Level level, Vec3 from, int depth) {
        BlockPos pos = BlockPos.containing(from);
        for (int i = 0; i <= depth; i++, pos = pos.below()) {
            BlockState s = level.getBlockState(pos);
            VoxelShape shape = s.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                double top = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                if (top <= from.y + 0.01 || i > 0) return new Vec3(from.x, top, from.z);
            }
        }
        return null;
    }

    private static boolean solid(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    public static boolean ownedBy(@Nullable UUID owner, Player p) {
        return owner != null && owner.equals(p.getUUID());
    }
}
