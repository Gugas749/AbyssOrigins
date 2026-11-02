package com.gugas749.abyssorigins.specialization.warrior;

import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.PowerHooks;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.*;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Warrior mechanics. Momentum is the class identity: chained melee hits stack damage,
 * and the tree decides whether you cash it in for sustain, executions or armour-breaking.
 */
public class WarriorHooks implements PowerHooks {

    private static final String STACKS = "war.momentum", LAST = "war.momentum_t";

    public static int momentum(ServerPlayer p) { return PowerState.getInt(p, STACKS); }

    private static int maxStacks(ServerPlayer p) { return Skills.has(p, "warrior_weapon_master") ? 8 : 5; }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!Targeting.isMelee(event.getSource(), player)) return;

        // ---- Momentum
        long now = player.level().getGameTime();
        int decay = Skills.isOrigin(player, "human") ? 120 : 60;              // Veteran's Discipline
        boolean frozen = Skills.has(player, "warrior_warlord") && AbilityStates.isActive(player, WarriorAbilities.WAR_CRY);
        int stacks = momentum(player);
        if (!frozen && now - PowerState.get(player, LAST) > decay) stacks = 0;

        float mult = 1f + stacks * 0.05f;
        if (player.getMainHandItem().is(ItemTags.SWORDS) || player.getMainHandItem().is(ItemTags.AXES))
            mult *= 1.10f;                                                     // Battle-Hardened
        if (Skills.has(player, "warrior_executioner") && target.getHealth() / target.getMaxHealth() < 0.30f)
            mult *= 1.30f;
        event.setAmount(event.getAmount() * mult);

        int max = maxStacks(player);
        if (stacks >= max) {
            if (Skills.has(player, "warrior_shieldbreaker"))
                target.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 80, 0), player);
            if (Skills.isOrigin(player, "demon")) Skills.addResource(player, 3f); // Infernal Berserker
        }
        stacks = Math.min(max, stacks + 1);
        PowerState.set(player, STACKS, stacks);
        PowerState.set(player, LAST, now);

        if (stacks == max) {
            Vfx.sparks(player.serverLevel(), Fx.chest(target), 1.0f, Palette.WAR, Palette.WAR_HI);
            player.displayClientMessage(Component.translatable("abyssorigins.message.warrior_momentum", stacks), true);
        }
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        if (Skills.has(player, "warrior_bloodrage") && Targeting.isMelee(source, player)
                && player.getHealth() / player.getMaxHealth() < 0.40f)
            player.heal(amount * 0.15f);
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (Skills.has(player, "warrior_relentless")) AbilityController.reduceClassCooldown(player, 1, 120);
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        return ability.getId().equals(WarriorAbilities.WAR_CRY) && Skills.has(player, "warrior_warlord") ? 0.5f : 1f;
    }
}
