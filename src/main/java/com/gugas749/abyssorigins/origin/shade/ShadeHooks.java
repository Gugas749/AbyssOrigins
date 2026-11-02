package com.gugas749.abyssorigins.origin.shade;

import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.power.*;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Shade mechanics. Umbra pools in darkness and bleeds away in light. Shadow Meld's
 * breaking strike is the Shade's signature burst; Light-Bane is its price.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class ShadeHooks implements PowerHooks {

    private final Sustained meldHandler;

    public ShadeHooks(Sustained meldHandler) {
        this.meldHandler = meldHandler;
    }

    private static boolean dusk(ServerPlayer p) { return Skills.has(p, "shade_dusk_dweller"); }

    @Override
    public void tick(ServerPlayer player) {
        // Umbral Sight
        var nv = player.getEffect(MobEffects.NIGHT_VISION);
        if (nv == null || nv.getDuration() < 220)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 500, 0, false, false));

        int light = Targeting.lightAt(player);

        // Eternal Night: permanent invisibility in deep darkness
        // Duskweave Garb: sneaking in darkness hides you completely
        if (Gear.fullSet(player, "shade") && player.isShiftKeyDown() && light <= 7 && player.tickCount % 10 == 0)
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));

        if (Skills.has(player, "shade_void_shroud") && light <= 4 && player.tickCount % 10 == 0)
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));

        if (player.tickCount % 20 != 0) return;

        // Umbra flow
        int darkThreshold = Skills.has(player, "shade_deep_shadows") ? 9 : 7;
        if (light <= darkThreshold) {
            float umbra = Skills.has(player, "shade_deep_shadows") ? 2.25f : 1.5f;
            Skills.addResource(player, Gear.fullSet(player, "shade") ? umbra * 1.5f : umbra);
        } else if (light >= 12 && !dusk(player)) {
            Skills.originData(player).drainResource(0.5f);
        }

        // Light-Bane
        if (!dusk(player) && Targeting.inSunlight(player))
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));

        // Nightstalker
        if (Skills.has(player, "shade_nightstalker") && !player.level().isDay()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   30, 0, false, false, true));
        }
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;

        // Breaking Shadow Meld with a strike: the ambush
        if (AbilityStates.isActive(player, ShadeAbilities.MELD_ID)) {
            float mult = Skills.has(player, "shade_ambush") ? 2.5f : 2.0f;
            event.setAmount(event.getAmount() * mult);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), player);
            PowerState.flag(player, "meld_break", 2); // read by class hooks later in the same event (Assassin resonance)
            AbilityStates.stop(player, ShadeAbilities.MELD_ID);
            var level = player.serverLevel();
            Vfx.of(VfxType.SLASH).at(Fx.chest(target)).dir(target.position().subtract(player.position())).scale(1.5f).extra(160)
                .colors(Palette.SHADOW_HI, 0xE0E8FF).flags(Vfx.VERTICAL).duration(12).send(level);
            Vfx.of(VfxType.BURST).at(Fx.chest(target)).scale(1.0f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(Vfx.VOID).duration(12).send(level);
            Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        }

        if (Skills.has(player, "shade_phantom_blade")
                && (target.hasEffect(MobEffects.BLINDNESS) || target.hasEffect(ModEffects.STUNNED)))
            event.setAmount(event.getAmount() * 1.3f);
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        int light = Targeting.lightAt(player);
        if (Skills.has(player, "shade_eclipse_skin") && light <= 7)
            event.setAmount(event.getAmount() * 0.85f);
        if (Skills.has(player, "shade_void_shroud") && Targeting.inSunlight(player))
            event.setAmount(event.getAmount() * 1.5f);

        // Shadow Double: slip away when cornered (90s)
        if (Skills.has(player, "shade_shadow_clone")
                && player.getHealth() - event.getAmount() < player.getMaxHealth() * 0.4f
                && !AbilityStates.isActive(player, ShadeAbilities.MELD_ID)
                && PowerState.tryTrigger(player, "shade_double", 1800)) {
            ShadeAbilities.ShadowMeld.meld(player, 60, meldHandler);
        }
    }

    /** Shadow-Woven: mobs struggle to notice you in darkness. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(Skills.isOrigin(p, "shade") || Eminence.is(p))) return;
        if (Targeting.lightAt(p) <= 7) event.modifyVisibility(0.5);
    }
}
