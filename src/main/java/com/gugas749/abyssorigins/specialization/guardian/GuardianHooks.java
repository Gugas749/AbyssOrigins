package com.gugas749.abyssorigins.specialization.guardian;

import com.gugas749.abyssorigins.power.AuraToggles;
import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.Optional;

/**
 * Guardian mechanics. Most of the Guardian's value is protecting OTHERS, so several
 * effects are static subscribers that look for a nearby Guardian when anyone is hurt.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class GuardianHooks implements PowerHooks {

    /** Prevents Martyrdom bouncing damage between two Guardians. */
    private static boolean redirecting = false;

    @Override
    public void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        if (Skills.has(player, "guardian_second_skin") && player.getHealth() < player.getMaxHealth() * 0.5f)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
        // Bastion: plant your feet
        if (Skills.has(player, "guardian_bastion") && player.isShiftKeyDown()
                && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false, true));
    }

    private static boolean isGuardian(Player p) { return Skills.isClass(p, "guardian"); }

    private static Optional<ServerPlayer> guardianNear(LivingEntity victim, String skill, boolean includeSelf) {
        if (!(victim.level() instanceof net.minecraft.server.level.ServerLevel level)) return Optional.empty();
        return level.getEntitiesOfClass(ServerPlayer.class, victim.getBoundingBox().inflate(8),
                p -> (includeSelf || p != victim) && p.isAlive() && isGuardian(p) && AuraToggles.active(p, skill))
            .stream().findFirst();
    }

    // ---- Bulwark & Retribution ----------------------------------------------

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer p) || !isGuardian(p)) return;
        int ticks = Skills.isOrigin(p, "human") ? 80 : 40;                       // Oathbound
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, true));

        if (Skills.has(p, "guardian_retribution") && event.getDamageSource().getEntity() instanceof LivingEntity attacker
                && event.getDamageSource().getDirectEntity() == attacker) {
            attacker.hurt(p.damageSources().thorns(p), 3f);
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), p);
            Vfx.burst(p.serverLevel(), Fx.chest(attacker), 0.5f, Palette.AEGIS, Palette.AEGIS_HI);
        }
    }

    // ---- Martyrdom ----------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAllyHurt(LivingIncomingDamageEvent event) {
        if (redirecting || !(event.getEntity() instanceof Player victim) || victim.level().isClientSide()) return;
        guardianNear(victim, "guardian_martyr", false).ifPresent(g -> {
            float share = event.getAmount() * 0.3f;
            event.setAmount(event.getAmount() - share);
            redirecting = true;
            try { g.hurt(g.damageSources().magic(), share); }
            finally { redirecting = false; }
            Vfx.beam(g.serverLevel(), Fx.chest(victim), Fx.chest(g), 0.14f, Palette.OATH, Palette.OATH_HI, 8);
        });
    }

    // ---- Eternal Vow --------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAllyLethal(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player victim) || victim.level().isClientSide()) return;
        guardianNear(victim, "guardian_eternal_vow", true).ifPresent(g -> {
            if (!PowerState.tryTrigger(g, "guardian_vow", 4800)) return;
            event.setCanceled(true);
            victim.setHealth(victim.getMaxHealth() * 0.3f);
            victim.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, 80, 1), g);
            Vfx.of(VfxType.PILLAR).at(victim.position()).scale(7).extra(1.0f).colors(Palette.OATH, Palette.OATH_HI).duration(36).send(g.serverLevel());
            Vfx.beam(g.serverLevel(), Fx.chest(g), Fx.chest(victim), 0.3f, Palette.OATH, Palette.OATH_HI, 16);
            Fx.sound(g.level(), victim.position(), SoundEvents.TOTEM_USE, 0.7f, 0.8f);
            g.displayClientMessage(Component.translatable("abyssorigins.message.guardian_vow", victim.getDisplayName()), true);
        });
    }
}
