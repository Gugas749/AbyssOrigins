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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VampireBatFormAbility extends OriginAbility {

    private static final int RESOURCE_COST = 25;
    private static final int DURATION      = 300;
    private static final int COOLDOWN_SECS = 60;
    private static final String NBT_TICKS  = "abyssorigins_bat_form_ticks";

    private final Map<UUID, Bat> decoyBats = new HashMap<>();

    public VampireBatFormAbility() {
        super("vampire_bat_form", "Bat Form",
            "Transform into a bat. Grants flight and invisibility for 15 seconds. Costs 25 blood.",
            COOLDOWN_SECS, RESOURCE_COST);
    }

    @Override
    public void activate(Player player, Level level) {
        if (level.isClientSide()) return;
        UUID id = player.getUUID();

        if (player.getPersistentData().contains(NBT_TICKS)) {
            endBatForm(player, level);
            return;
        }

        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        data.drainResource(RESOURCE_COST);
        player.getPersistentData().putInt(NBT_TICKS, DURATION);

        Bat bat = new Bat(EntityType.BAT, level);
        bat.setPos(player.getX(), player.getY(), player.getZ());
        bat.setInvulnerable(true); bat.setNoAi(true); bat.setSilent(true);
        bat.setPersistenceRequired(); bat.setResting(false);
        level.addFreshEntity(bat);
        decoyBats.put(id, bat);

        for (int i = 0; i < 5; i++) {
            Bat decoy = new Bat(EntityType.BAT, level);
            double angle = Math.PI * 2 * i / 5.0;
            decoy.setPos(player.getX() + Math.cos(angle) * 3, player.getY(),
                         player.getZ() + Math.sin(angle) * 3);
            level.addFreshEntity(decoy);
        }

        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(0.08f);
        player.onUpdateAbilities();
        if (player instanceof ServerPlayer sp) sp.onUpdateAbilities();

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION + 20, 0, false, false));
        player.setDeltaMovement(player.getDeltaMovement().add(0, 0.5, 0));
        player.hurtMarked = true;

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 1.5f, 1.0f);

        player.displayClientMessage(
            Component.translatable("abyssorigins.message.bat_form_activated"), false);

        if (player instanceof ServerPlayer sp) ModPackets.syncPlayer(sp);
    }

    public void tick(Player player, Level level) {
        if (level.isClientSide()) return;
        if (!player.getPersistentData().contains(NBT_TICKS)) return;

        int remaining = player.getPersistentData().getInt(NBT_TICKS) - 1;

        if (!player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        Bat bat = decoyBats.get(player.getUUID());
        if (bat != null && bat.isAlive()) {
            bat.setPos(player.getX(), player.getY(), player.getZ());
            bat.setDeltaMovement(player.getDeltaMovement());
            bat.setResting(false);
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 2, false, false));

        if (remaining == 3 * 20)
            player.displayClientMessage(
                Component.translatable("abyssorigins.message.bat_form_ending_soon"), false);
        else if (remaining == 20)
            player.displayClientMessage(
                Component.translatable("abyssorigins.message.bat_form_ending"), false);

        if (remaining <= 0) endBatForm(player, level);
        else player.getPersistentData().putInt(NBT_TICKS, remaining);
    }

    private void endBatForm(Player player, Level level) {
        player.getPersistentData().remove(NBT_TICKS);

        Bat bat = decoyBats.remove(player.getUUID());
        if (bat != null && bat.isAlive()) bat.discard();

        player.removeEffect(MobEffects.INVISIBILITY);

        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.getAbilities().setFlyingSpeed(0.05f);
            player.onUpdateAbilities();
            if (player instanceof ServerPlayer sp) sp.onUpdateAbilities();
        }

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BAT_DEATH, SoundSource.PLAYERS, 0.8f, 1.0f);

        player.displayClientMessage(
            Component.translatable("abyssorigins.message.bat_form_ended"), false);
    }

    @Override
    public boolean canActivate(Player player) {
        return player.getPersistentData().contains(NBT_TICKS) || true;
    }
}
