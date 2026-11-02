package com.gugas749.abyssorigins.specialization.guardian;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Guardian abilities: Shield Bash (F) and Aegis (G). */
public final class GuardianAbilities {

    private GuardianAbilities() {}

    public static final String AEGIS = "guardian_aegis";

    // ---- Shield Bash --------------------------------------------------------

    public static class ShieldBash extends SpecializationAbility {
        public ShieldBash() {
            super("guardian_shield_bash", "abyssorigins.ability.guardian_shield_bash.name", "abyssorigins.ability.guardian_shield_bash.desc", 10);
            icon("⛨"); color(0xFF5B8DEF);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            List<LivingEntity> hit = Targeting.enemiesAround(player, player.position(), 3.5).stream()
                .filter(e -> e.position().subtract(player.position()).multiply(1, 0, 1).normalize().dot(look) > 0.45)
                .toList();
            if (hit.isEmpty()) return false;

            boolean concussive = Skills.has(player, "guardian_concussive");
            var level = player.serverLevel();
            for (LivingEntity e : hit) {
                e.hurt(Targeting.spell(player), concussive ? 8f : 5f);
                e.knockback(1.2, -look.x, -look.z);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, concussive ? 50 : 30, 0), player);
                if (Skills.has(player, "guardian_provoke") && e instanceof Mob mob) mob.setTarget(player);
                Vfx.burst(level, Fx.chest(e), 0.6f, Palette.AEGIS, Palette.AEGIS_HI);
            }

            Vec3 front = player.position().add(look.scale(1.5)).add(0, 1, 0);
            Vfx.of(VfxType.SLASH).at(player.position().add(0, 1.0, 0)).dir(look).scale(2.4f).extra(120)
                .colors(Palette.AEGIS, Palette.AEGIS_HI).duration(12).send(level);
            Vfx.shockwave(level, player.position().add(look.scale(1.5)), 2.2f, Palette.AEGIS, Palette.AEGIS_HI, 12);
            Fx.sound(level, front, SoundEvents.SHIELD_BLOCK, 1.0f, 0.7f);
            Fx.sound(level, front, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 1.2f);
            return true;
        }
    }

    // ---- Aegis --------------------------------------------------------------

    public static class Aegis extends SpecializationAbility implements Sustained {
        private static final ResourceLocation UNBREAKABLE =
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "guardian_unbreakable_kb");

        public Aegis() {
            super(AEGIS, "abyssorigins.ability.guardian_aegis.name", "abyssorigins.ability.guardian_aegis.desc", 40);
            icon("⬡"); color(0xFFF2D47E);
        }

        static double radius(ServerPlayer p) { return Skills.has(p, "guardian_provoke") ? 9 : 6; }

        @Override
        public boolean activate(ServerPlayer player) {
            int duration = Skills.has(player, "guardian_unbreakable") ? 180 : 120;
            if (Skills.has(player, "guardian_unbreakable")) {
                AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kb != null && !kb.hasModifier(UNBREAKABLE))
                    kb.addTransientModifier(new AttributeModifier(UNBREAKABLE, 1.0, AttributeModifier.Operation.ADD_VALUE));
            }
            // Taunt: every hostile nearby turns on the Guardian
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10),
                    m -> m instanceof Enemy && m.isAlive()))
                mob.setTarget(player);

            if (Skills.isOrigin(player, "seraph")) Skills.addResource(player, 10f);   // Aegis of Dawn

            var lvl = player.serverLevel();
            Vfx.of(VfxType.DOME).follow(player).scale((float) radius(player)).colors(Palette.OATH, Palette.OATH_HI).flags(Vfx.HALF).duration(duration).send(lvl);
            Vfx.of(VfxType.SIGIL).follow(player).scale((float) radius(player) * 0.6f).extra(6).colors(Palette.OATH, Palette.OATH_HI).duration(duration).send(lvl);

            Fx.sound(player.level(), player.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.2f);
            Fx.sound(player.level(), player.position(), SoundEvents.ANVIL_LAND, 0.4f, 1.6f);
            AbilityStates.start(player, AEGIS, duration, this);
            return true;
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            double r = radius(player);
            var level = player.serverLevel();
            if (player.tickCount % 20 == 0) {
                boolean rally = Skills.has(player, "guardian_rally");
                for (LivingEntity ally : Targeting.alliesAround(player, player.position(), r)) {
                    ally.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, 30, 1), player);
                    if (rally) ally.heal(1f);
                }
                if (Skills.isOrigin(player, "seraph"))
                    for (LivingEntity e : Targeting.enemiesAround(player, player.position(), r))
                        if (Targeting.isUndead(e)) { e.hurt(Targeting.spell(player), 2f); e.igniteForSeconds(2); }
            }
        }

        @Override
        public void onEnd(ServerPlayer player) {
            AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.removeModifier(UNBREAKABLE);
            Vfx.stop(player.serverLevel(), player, VfxType.DOME);
            Vfx.stop(player.serverLevel(), player, VfxType.SIGIL);
            Fx.sound(player.level(), player.position(), SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.2f);
        }
    }
}
