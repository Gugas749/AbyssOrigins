package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Blood Drain: bite the nearest blood-bearing enemy. Crimson Tide turns it into an
 * area drain. Does nothing — and costs nothing — if no valid target is in reach.
 */
public class VampireBloodDrainAbility extends OriginAbility {
    private static final float RANGE = 3.0f, TIDE_RANGE = 4.5f, DAMAGE = 3.0f, HEAL = 2.0f, BLOOD_GAIN = 15.0f;

    public VampireBloodDrainAbility() {
        super("vampire_blood_drain",
            "abyssorigins.ability.vampire_blood_drain.name",
            "abyssorigins.ability.vampire_blood_drain.desc",
            20, 0);
        icon("❣"); color(0xFFC0182E);
    }

    @Override
    public boolean activate(ServerPlayer player) {
        boolean tide = Skills.has(player, "vamp_crimson_tide");
        double range = tide ? TIDE_RANGE : RANGE;

        List<LivingEntity> candidates = Targeting.preyAround(player, player.position(), range).stream()
            .filter(VampireBlood::hasBlood)
            .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
            .toList();
        if (candidates.isEmpty()) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.blood_drain_no_target"), true);
            return false;
        }

        List<LivingEntity> victims = tide ? candidates : candidates.subList(0, 1);
        float heal = Skills.has(player, "vamp_vitality") ? HEAL + 2f : HEAL;
        var level = player.serverLevel();
        Vec3 mouth = player.getEyePosition().subtract(0, 0.3, 0);

        for (LivingEntity target : victims) {
            target.hurt(Targeting.spell(player), DAMAGE);
            player.heal(heal);
            Skills.addResource(player, BLOOD_GAIN);
            Vfx.beam(level, Fx.chest(target), mouth, 0.3f, Palette.BLOOD, Palette.BLOOD_HI, 14);
            Vfx.burst(level, Fx.chest(target), 0.6f, Palette.BLOOD, Palette.BLOOD_HI);
        }

        Fx.sound(level, player.position(), SoundEvents.GENERIC_DRINK, 1.0f, 0.6f);
        Fx.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, 0.6f, 1.6f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.blood_drain_gained",
            String.valueOf((int) (BLOOD_GAIN * victims.size()))), true);
        return true;
    }
}
