package com.gugas749.abyssorigins.origin.voidborn;

import com.gugas749.abyssorigins.power.AuraToggles;
import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.PowerState;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import net.minecraft.core.Holder;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Voidborn mechanics: Entropic Touch erodes armour, Hollow Form weakens all healing,
 * and the tree trades that fragility for blink mobility and gravity control.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class VoidbornHooks implements PowerHooks {

    @Override
    public void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;

        // Faster essence gain in the End or below bedrock level
        float extra = 0f;
        if (player.level().dimension() == Level.END || player.getY() < 0) extra += 1f;
        if (Skills.has(player, "void_attunement")) extra += 0.25f;
        if (Gear.fullSet(player, "voidborn")) extra += 0.5f;           // Abyssal Carapace
        if (extra > 0) Skills.addResource(player, extra);

        if (AuraToggles.active(player, "void_entropy_field")) {
            for (LivingEntity e : Targeting.enemiesAround(player, player.position(), 4)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0), player);
                e.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 30, 0), player);
            }
            AbyssVfx.of(VfxType.AURA).follow(player).scale(4).colors(Palette.VOID_DARK, Palette.VOID).flags(AbyssVfx.VOID).duration(24).send(player.serverLevel());
        }
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;

        // Entropic Touch: armour erodes (stacks to III with Deep Erosion)
        MobEffectInstance cur = target.getEffect(ModEffects.SUNDERED);
        if (cur != null && Skills.has(player, "void_null_edge")) event.setAmount(event.getAmount() * 1.15f);   // Null Edge
        int amp = 0;
        if (Skills.has(player, "void_erosion_deep") && cur != null) amp = Math.min(2, cur.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 80, amp), player);

        if (PowerState.consume(player, "void_empowered")) {
            event.setAmount(event.getAmount() * 1.5f);
            player.removeEffect(MobEffects.INVISIBILITY);
            AbyssVfx.burst(player.serverLevel(), Fx.chest(target), 0.9f, Palette.VOID, Palette.VOID_HI);
        }
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        if (Targeting.isMelee(source, player) && Skills.has(player, "void_siphon")) Skills.addResource(player, 1f);   // Void Siphon
        if (Targeting.isMelee(source, player) && VoidbornAbilities.EchoStrikes.active(player))
            VoidbornAbilities.EchoStrikes.echo(player, target, amount);
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (src.is(DamageTypeTags.IS_FALL) && Skills.has(player, "void_abyss_walker")) {
            event.setCanceled(true);
            return;
        }
        // Phase Skin: 15% chance to ignore a melee hit
        boolean melee = src.getEntity() instanceof LivingEntity && src.getDirectEntity() == src.getEntity();

        // Recoil: melee attackers are Sundered and slowed
        if (melee && Skills.has(player, "void_recoil") && src.getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 60, 0), player);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), player);
        }

        // Starless: dropping below 40% health hides you (45s cooldown)
        if (Skills.has(player, "void_starless") && player.getHealth() - event.getAmount() < player.getMaxHealth() * 0.4f
                && PowerState.tryTrigger(player, "void_starless", 900)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false, true));
            AbyssVfx.of(VfxType.BURST).at(Fx.chest(player)).scale(0.9f).colors(Palette.VOID, Palette.VOID_HI).flags(AbyssVfx.VOID).duration(12).send(player.serverLevel());
            Fx.sound(player.level(), player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7f, 0.8f);
        }
        if (melee && Skills.has(player, "void_phase_skin") && player.getRandom().nextFloat() < 0.15f) {
            event.setCanceled(true);
            AbyssVfx.of(VfxType.RIFT).at(Fx.chest(player)).dir(player.getLookAngle()).scale(2.0f).colors(Palette.VOID, Palette.VOID_HI).duration(10).send(player.serverLevel());
            Fx.sound(player.level(), player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.6f, 1.6f);
        }
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (Skills.has(player, "void_hunger") && victim.hasEffect(ModEffects.SUNDERED))
            Skills.addResource(player, 10f);

        // Implosion: the victim collapses inward, hurting and Sundering those nearby
        if (Skills.has(player, "void_implosion")) {
            for (LivingEntity e : Targeting.enemiesAround(player, victim.position(), 3)) {
                if (e == victim) continue;
                e.hurt(Targeting.spell(player), 4f);
                e.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 80, 0), player);
            }
            AbyssVfx.of(VfxType.VORTEX).at(Fx.chest(victim)).scale(1.2f).colors(Palette.VOID, Palette.VOID_HI).duration(14).send(player.serverLevel());
        }
    }

    // ---- Static event subscribers (non-hook events) --------------------------

    /** Hollow Form: reduced healing. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        if (!Skills.isOrigin(p, "voidborn")) return;
        if (Gear.fullSet(p, "voidborn")) return;                         // Abyssal Carapace removes the penalty
        event.setAmount(event.getAmount() * (Skills.has(p, "void_heart_of_abyss") ? 0.5f : 0.75f));
    }

    /** Unanchored: Endermen stay neutral. */
    @SubscribeEvent
    public static void onEndermanAnger(EnderManAngerEvent event) {
        if (Skills.isOrigin(event.getPlayer(), "voidborn") || Eminence.is(event.getPlayer())) event.setCanceled(true);
    }

    /** Unanchored + Null Ward: immunities to gravity and decay. */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player p) || !(Skills.isOrigin(p, "voidborn") || Eminence.is(p))) return;
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        boolean immune = effect.is(MobEffects.LEVITATION);
        if (Skills.has(p, "void_null_ward"))
            immune |= effect.is(MobEffects.WITHER) || effect.is(MobEffects.DARKNESS) || effect.is(MobEffects.BLINDNESS);
        if (immune) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
}
