package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.PowerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Crown of All Bloodlines: runs every other origin's PowerHooks for the Eminence.
 * Their skill-gated mechanics all fire because the Eminence holds every origin skill,
 * and the same skills (Tempered, Fallen Grace, Dusk Dweller...) lift the bloodlines' weaknesses.
 */
public class EminenceHooks implements PowerHooks {

    /** Flat share of incoming damage the Eminence shrugs off, after every bloodline's own reactions. */
    private static final float SOVEREIGN_REDUCTION = 0.20f;

    private List<PowerHooks> subjects;

    private List<PowerHooks> subjects() {
        if (subjects == null)
            subjects = AbyssOriginsAPI.getAllOrigins().stream()
                .filter(o -> !Eminence.is(o)).map(Origin::getHooks).toList();
        return subjects;
    }

    private void each(Consumer<PowerHooks> action) {
        for (PowerHooks h : subjects()) {
            try { action.accept(h); }
            catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Eminence relay failed in {}", h.getClass().getSimpleName(), e); }
        }
    }

    @Override
    public void tick(ServerPlayer player) {
        each(h -> h.tick(player));
    }

    @Override
    public void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event) {
        each(h -> h.onAttack(player, target, event));
    }

    @Override
    public void onDamageDealt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        each(h -> h.onDamageDealt(player, target, source, amount));
    }

    @Override
    public void onHurt(ServerPlayer player, LivingIncomingDamageEvent event) {
        for (PowerHooks h : subjects()) {
            if (event.isCanceled()) return;
            try { h.onHurt(player, event); }
            catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Eminence relay failed in {}", h.getClass().getSimpleName(), e); }
        }
        // Sovereign Body: every blow lands lighter on the crown (void and /kill still bite in full)
        if (!event.isCanceled() && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            event.setAmount(event.getAmount() * (1f - SOVEREIGN_REDUCTION));
    }

    @Override
    public void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        each(h -> h.onKill(player, victim, source));
    }

    @Override
    public boolean onLethal(ServerPlayer player, DamageSource source) {
        for (PowerHooks h : subjects()) {
            try { if (h.onLethal(player, source)) return true; }
            catch (Exception e) { Abyssorigins.LOGGER.error("[AbyssOrigins] Eminence relay failed in {}", h.getClass().getSimpleName(), e); }
        }
        return false;
    }

    @Override
    public void onAbilityUsed(ServerPlayer player, Ability ability) {
        each(h -> h.onAbilityUsed(player, ability));
    }

    @Override
    public float cooldownMultiplier(ServerPlayer player, Ability ability) {
        float m = 1f;
        for (PowerHooks h : subjects()) m *= h.cooldownMultiplier(player, ability);
        return m;
    }
}
