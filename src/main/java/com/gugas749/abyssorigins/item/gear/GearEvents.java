package com.gugas749.abyssorigins.item.gear;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.power.Targeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Relic weapon passives and parries, rejection of foreign gear, and the generic full-set
 * bonuses. Origin-specific set bonuses that tune a resource live in each origin's hooks
 * (they call {@link Gear#fullSet}).
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class GearEvents {

    private GearEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        // ---- Swinging a relic
        if (event.getSource().getEntity() instanceof ServerPlayer p && Targeting.isMelee(event.getSource(), p)
                && p.getMainHandItem().getItem() instanceof OriginWeaponItem w) {
            if (Gear.attuned(p, w.originPath())) w.power().onHit(p, target, event);
            else {
                event.setAmount(Math.min(event.getAmount(), 1f));     // the relic refuses to bite
                Gear.reject(p, w, 2, 1);
            }
        }

        // ---- Holding a relic / wearing a set while being hit
        if (target instanceof ServerPlayer v && !event.isCanceled()) {
            if (v.getMainHandItem().getItem() instanceof OriginWeaponItem w && Gear.attuned(v, w.originPath()))
                w.power().onHurt(v, event);

            // Infernal Harness: melee attackers catch fire
            if (!event.isCanceled() && Gear.fullSet(v, "demon")
                    && event.getSource().getEntity() instanceof LivingEntity a && event.getSource().getDirectEntity() == a)
                a.igniteForSeconds(3);
        }
    }

    /** Nocturne Regalia: +10% melee lifesteal. */
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && Targeting.isMelee(event.getSource(), p)
                && Gear.fullSet(p, "vampire"))
            p.heal(event.getNewDamage() * 0.10f);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;

        // Rejection: foreign armor hurts every few seconds; a foreign relic in hand saps strength
        int foreign = Gear.foreignPieces(p);
        if (foreign > 0) {
            for (EquipmentSlot slot : new EquipmentSlot[]{ EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack s = p.getItemBySlot(slot);
                if (s.getItem() instanceof OriginBound b && !Gear.attuned(p, b.originPath())) {
                    Gear.reject(p, b, 1, foreign);
                    break;
                }
            }
        }
        if (p.getMainHandItem().getItem() instanceof OriginWeaponItem w && !Gear.attuned(p, w.originPath())) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));
            if (p.tickCount % 80 == 0)
                p.displayClientMessage(Component.translatable("abyssorigins.message.gear_uneasy", Gear.originName(w.originPath())), true);
        }

        // Vanguard Plate: resolve under pressure
        if (Gear.fullSet(p, "human") && p.getHealth() < p.getMaxHealth() * 0.5f)
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
    }
}
