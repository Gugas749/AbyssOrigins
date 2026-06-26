package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class VampireBloodDrainAbility extends OriginAbility {
    private static final float DRAIN_RANGE = 3.0f, DRAIN_DAMAGE = 2.0f, HEAL_AMOUNT = 2.0f, BLOOD_GAIN = 15.0f;

    public VampireBloodDrainAbility() {
        super("vampire_blood_drain",
            "abyssorigins.ability.vampire_blood_drain.name",
            "abyssorigins.ability.vampire_blood_drain.desc",
            60, 0);
    }

    @Override
    public void activate(Player player, Level level) {
        Vec3 pos = player.position();
        AABB box = new AABB(pos.x-DRAIN_RANGE, pos.y-DRAIN_RANGE, pos.z-DRAIN_RANGE, pos.x+DRAIN_RANGE, pos.y+DRAIN_RANGE, pos.z+DRAIN_RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        if (targets.isEmpty()) { player.displayClientMessage(Component.translatable("abyssorigins.message.blood_drain_no_target"), true); return; }
        LivingEntity target = targets.stream().min((a,b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player))).orElse(null);
        if (target == null) return;
        target.hurt(level.damageSources().magic(), DRAIN_DAMAGE);
        player.heal(HEAL_AMOUNT);
        player.getData(OriginAttachments.PLAYER_ORIGIN.get()).addResource(BLOOD_GAIN);
        if (level instanceof ServerLevel sl) {
            Vec3 tPos = target.position().add(0, target.getBbHeight()/2, 0), pPos = player.position().add(0, player.getEyeHeight(), 0);
            for (int i = 0; i < 20; i++) { Vec3 p = tPos.lerp(pPos, i/20.0); sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01); }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 0.8f);
        player.displayClientMessage(Component.translatable("abyssorigins.message.blood_drain_gained", String.valueOf((int)BLOOD_GAIN)), true);
        if (player instanceof ServerPlayer sp) ModPackets.syncPlayer(sp);
    }

    @Override public boolean canActivate(Player player) { return true; }
}
