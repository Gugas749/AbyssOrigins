package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class VampireBloodDrainAbility extends OriginAbility {

    private static final float DRAIN_DAMAGE   = 4.0f; // 2 hearts from target
    private static final float BLOOD_RESTORE  = 20.0f;
    private static final double RANGE         = 4.0;

    public VampireBloodDrainAbility() {
        super("vampire_blood_drain", "Blood Drain",
            "Drain blood from a nearby enemy. Deals 2 hearts and restores 20 blood.", 20, 0);
    }

    @Override
    public void activate(Player player, Level level) {
        // Find closest living entity within range (not the player themselves)
        AABB box = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class, box,
            e -> e != player && e.isAlive()
        );

        if (targets.isEmpty()) {
            player.displayClientMessage(
                Component.literal("§cNo target in range."), true);
            return;
        }

        // Sort by distance, pick closest
        LivingEntity target = targets.stream()
            .min((a, b) -> Double.compare(
                a.distanceToSqr(player), b.distanceToSqr(player)))
            .orElse(null);

        if (target == null) return;

        // Deal damage
        target.hurt(player.damageSources().playerAttack(player), DRAIN_DAMAGE);

        // Restore blood
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        data.addResource(BLOOD_RESTORE);

        if (player instanceof ServerPlayer sp) ModPackets.syncPlayer(sp);

        player.displayClientMessage(
            Component.literal("§4\t\\2764 Blood Drain! +" + (int)BLOOD_RESTORE + " blood."), true);
    }

    @Override
    public boolean canActivate(Player player) { return true; }
}
