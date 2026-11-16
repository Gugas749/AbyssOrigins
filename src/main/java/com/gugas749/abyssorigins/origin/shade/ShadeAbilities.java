package com.gugas749.abyssorigins.origin.shade;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Shade abilities: Shadow Meld (R), Umbral Grasp (V) and Phase Walk (taught by the tree). All spend Umbra. */
public final class ShadeAbilities {

    private ShadeAbilities() {}

    public static final String MELD_ID = "shade_meld";

    // ---- Shadow Meld --------------------------------------------------------

    public static class ShadowMeld extends OriginAbility implements Sustained {
        public ShadowMeld() {
            super(MELD_ID,
                "abyssorigins.ability.shade_meld.name",
                "abyssorigins.ability.shade_meld.desc",
                20, 30);
            icon("☾"); color(0xFF7D93C9);
        }

        @Override
        public int cooldownTicks(ServerPlayer player) {
            return Skills.has(player, "shade_lord_of_night") ? getBaseCooldownTicks() / 2 : getBaseCooldownTicks();
        }

        @Override
        public boolean activate(ServerPlayer player) {
            meld(player, Skills.has(player, "shade_lingering_dark") ? 240 : 160, this);
            return true;
        }

        /** Also used by Shadow Double (free emergency meld). */
        public static void meld(ServerPlayer player, int ticks, Sustained handler) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks + 10, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, false, false));

            // Every mob targeting you loses its target
            AABB box = player.getBoundingBox().inflate(16);
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, m -> m.getTarget() == player))
                mob.setTarget(null);

            var level = player.serverLevel();
            AbyssVfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.3f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(AbyssVfx.VOID).duration(14).send(level);
            Fx.sound(level, player.position(), SoundEvents.INK_SAC_USE, 1.0f, 0.5f);
            Fx.sound(level, player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7f, 0.6f);
            AbilityStates.start(player, MELD_ID, ticks, handler);
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            if (Skills.has(player, "shade_lord_of_night") && player.tickCount % 20 == 0)
                player.heal(1f);
        }

        @Override
        public void onEnd(ServerPlayer player) {
            player.removeEffect(MobEffects.INVISIBILITY);
            AbyssVfx.of(VfxType.BURST).at(Fx.chest(player)).scale(0.9f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(AbyssVfx.VOID).duration(12).send(player.serverLevel());
        }
    }

    // ---- Umbral Grasp -------------------------------------------------------

    public static class UmbralGrasp extends OriginAbility {
        public UmbralGrasp() {
            super("shade_umbral_grasp",
                "abyssorigins.ability.shade_umbral_grasp.name",
                "abyssorigins.ability.shade_umbral_grasp.desc",
                18, 35);
            icon("☘"); color(0xFF4A5A8A);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            boolean reach = Skills.has(player, "shade_tendril_reach");
            List<LivingEntity> caught = Targeting.enemiesAround(player, player.position(), reach ? 9 : 6);
            if (caught.isEmpty()) return false;

            var level = player.serverLevel();
            for (LivingEntity e : caught) {
                e.hurt(Targeting.spell(player), 4f);
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, reach ? 60 : 40, 0), player);
                AbyssVfx.of(VfxType.TENDRILS).at(e.position()).scale(e.getBbHeight() + 0.7f).extra(5).colors(Palette.SHADOW, Palette.SHADOW_HI)
                    .duration((reach ? 60 : 40) + 10).send(level);
                AbyssVfx.of(VfxType.BEAM).at(player.position().add(0, 0.15, 0)).to(e.position().add(0, 0.15, 0)).extra(0.3f)
                    .colors(Palette.SHADOW, Palette.SHADOW_HI).flags(AbyssVfx.VOID).duration(14).send(level);
            }
            if (Skills.has(player, "shade_soul_siphon")) player.heal(caught.size());

            AbyssVfx.of(VfxType.SIGIL).at(player.position()).scale(reach ? 4f : 3f).extra(7).colors(Palette.SHADOW_HI, 0xB8C8F0).duration(30).send(level);
            Fx.sound(level, player.position(), SoundEvents.EVOKER_FANGS_ATTACK, 1.0f, 0.6f);
            Fx.sound(level, player.position(), SoundEvents.SCULK_CATALYST_BLOOM, 1.0f, 0.7f);
            return true;
        }
    }

    // ---- Phase Walk (skill node "shade_phase_walk") -------------------------------------

    /**
     * For 6 seconds the shade flies and passes through walls, floors and ceilings (ModEffects.PHASING,
     * PlayerPhaseMixin). When it ends (or the key is pressed again) the body is solid again; if that happens
     * inside blocks it is pushed out to the nearest open space.
     */
    public static class PhaseWalk extends OriginAbility implements Sustained {
        public static final String ID = "shade_phase_walk";
        private static final int DURATION = 120;

        public PhaseWalk() {
            super(ID, "abyssorigins.ability." + ID + ".name", "abyssorigins.ability." + ID + ".desc", 35, 30);
            icon("◌"); color(0xFF5E6FA8);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            player.addEffect(new MobEffectInstance(ModEffects.PHASING, DURATION + 10, 0, false, false, true));
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlyingSpeed(0.04f);
            player.onUpdateAbilities();

            AbyssVfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.3f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(AbyssVfx.VOID).duration(14).send(level);
            AbyssVfx.of(VfxType.TENDRILS).at(player.position()).scale(2.2f).extra(6).colors(Palette.SHADOW, Palette.SHADOW_HI).duration(18).send(level);
            AbyssVfx.of(VfxType.TRAIL).follow(player).scale(1.0f).colors(Palette.SHADOW, Palette.SHADOW_HI).duration(DURATION).send(level);
            Fx.sound(level, player.position(), SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.6f);
            Fx.sound(level, player.position(), SoundEvents.SOUL_ESCAPE.value(), 1.2f, 0.6f);
            player.displayClientMessage(Component.translatable("abyssorigins.message.phase_walk", DURATION / 20), true);
            AbilityStates.start(player, getId(), DURATION, this);
            return true;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            // Milk or a cleanse must not end it inside a wall: the effect stays until the power ends
            if (!player.hasEffect(ModEffects.PHASING))
                player.addEffect(new MobEffectInstance(ModEffects.PHASING, remaining + 10, 0, false, false, true));
            if (!player.getAbilities().mayfly && !player.isSpectator()) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.fallDistance = 0;
            ServerLevel level = player.serverLevel();
            // Never move below the bottom of the world
            if (player.getY() < level.getMinBuildHeight() + 1) {
                player.teleportTo(player.getX(), level.getMinBuildHeight() + 1, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
            }
            if (remaining % 8 == 0)
                AbyssVfx.of(VfxType.AURA).follow(player).scale(1.1f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(AbyssVfx.VOID).duration(14).send(level);
            if (remaining % 16 == 0 && !level.noCollision(player, player.getBoundingBox()))
                Fx.sound(level, player.position(), SoundEvents.SCULK_BLOCK_SPREAD, 0.5f, 0.6f);
            if (remaining == 40) player.displayClientMessage(Component.translatable("abyssorigins.message.phase_walk_ending"), true);
        }

        @Override
        public void onEnd(ServerPlayer player) {
            player.removeEffect(ModEffects.PHASING);
            player.noPhysics = player.isSpectator();
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
            }
            player.getAbilities().setFlyingSpeed(0.05f);
            player.onUpdateAbilities();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            ejectFromWalls(player);

            ServerLevel level = player.serverLevel();
            AbyssVfx.stop(level, player, VfxType.TRAIL);
            AbyssVfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.0f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(AbyssVfx.VOID).duration(12).send(level);
            Fx.sound(level, player.position(), SoundEvents.SOUL_ESCAPE.value(), 1.0f, 1.2f);
        }

        /** Ending inside blocks: move to the nearest spot where the body fits (up first, then outwards). */
        private static void ejectFromWalls(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            if (level.noCollision(player, player.getBoundingBox()) && player.getY() >= level.getMinBuildHeight()) return;
            BlockPos base = player.blockPosition();
            for (int r = 0; r <= 4; r++)
                for (int dy = 0; dy <= 6; dy++)
                    for (int sign : new int[]{1, -1})
                        for (int dx = -r; dx <= r; dx++)
                            for (int dz = -r; dz <= r; dz++) {
                                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;   // ring r only
                                int y = dy * sign;
                                if (sign < 0 && dy == 0) continue;
                                Vec3 at = Vec3.atBottomCenterOf(base.offset(dx, y, dz));
                                if (level.noCollision(player, player.getBoundingBox().move(at.subtract(player.position())))) {
                                    player.teleportTo(at.x, at.y, at.z);
                                    return;
                                }
                            }
            // Nowhere to stand within reach: go straight up to the surface
            BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, base);
            player.teleportTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        }
    }
}
