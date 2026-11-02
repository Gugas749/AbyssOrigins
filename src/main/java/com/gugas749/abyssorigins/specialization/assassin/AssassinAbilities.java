package com.gugas749.abyssorigins.specialization.assassin;

import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.network.packet.RevealPacket;
import com.gugas749.abyssorigins.power.*;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Assassin abilities: Shadowstep (F) and Venom Edge (G). */
public final class AssassinAbilities {

    private AssassinAbilities() {}

    public static final String SHADOWSTEP = "assassin_shadowstep";
    public static final String VENOM      = "assassin_venom_edge";

    // ---- Shadowstep ---------------------------------------------------------

    public static class Shadowstep extends SpecializationAbility {
        public Shadowstep() {
            super(SHADOWSTEP, "abyssorigins.ability.assassin_shadowstep.name", "abyssorigins.ability.assassin_shadowstep.desc", 16);
            icon("†"); color(0xFF3FB58A);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            LivingEntity target = Targeting.lookTarget(player, 12).orElse(null);
            if (target == null) return false;

            ServerLevel level = player.serverLevel();
            Vec3 facing = Vec3.directionFromRotation(0, target.getYRot()).normalize();
            Vec3 dest = target.position().subtract(facing.scale(target.getBbWidth() / 2 + 0.9));
            if (!level.noCollision(player, player.getBoundingBox().move(dest.subtract(player.position())))) {
                player.displayClientMessage(Component.translatable("abyssorigins.message.void_step_blocked"), true);
                return false;
            }

            Vec3 from = player.position();
            Vfx.of(VfxType.RIFT).at(from.add(0, 1, 0)).dir(dest.subtract(from)).scale(2.2f).colors(Palette.ASSASSIN, Palette.ASSASSIN_HI).duration(14).send(level);
            player.teleportTo(level, dest.x, dest.y, dest.z, target.getYRot(), player.getXRot());
            player.fallDistance = 0;
            Vfx.of(VfxType.BURST).at(dest.add(0, 1, 0)).scale(1.0f).colors(Palette.SHADOW, Palette.ASSASSIN).flags(Vfx.VOID).duration(12).send(level);
            Fx.sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.7f);

            PowerState.flag(player, "assassin_ss", 40); // next strike is a guaranteed backstab
            if (Skills.has(player, "assassin_vanish"))
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            if (Skills.has(player, "assassin_mark_for_death")) {
                target.addEffect(new MobEffectInstance(ModEffects.HEXED, 120, 0), player);
                RevealPacket.send(player, target, 120, 0x3FB58A);
                Vfx.of(VfxType.MARK).follow(target).colors(Palette.ASSASSIN, Palette.ASSASSIN_HI).duration(120).sendTo(player);
            }
            return true;
        }
    }

    // ---- Venom Edge ---------------------------------------------------------

    public static class VenomEdge extends SpecializationAbility implements Sustained {
        public VenomEdge() {
            super(VENOM, "abyssorigins.ability.assassin_venom_edge.name", "abyssorigins.ability.assassin_venom_edge.desc", 25);
            icon("⚗"); color(0xFF7CD957);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            int duration = Skills.has(player, "assassin_toxicology") ? 300 : 200;
            Vfx.burst(player.serverLevel(), Fx.chest(player), 0.7f, Palette.VENOM, Palette.VENOM_HI);
            Vfx.of(VfxType.AURA).follow(player).scale(0.9f).colors(Palette.VENOM, Palette.VENOM_HI).duration(duration).send(player.serverLevel());
            Fx.sound(player.level(), player.position(), SoundEvents.BREWING_STAND_BREW, 1.0f, 1.4f);
            AbilityStates.start(player, VENOM, duration, this);
            return true;
        }

        @Override
        public void onEnd(ServerPlayer player) {
            Vfx.stop(player.serverLevel(), player, VfxType.AURA, Palette.VENOM);
        }
    }
}
