package com.gugas749.abyssorigins.specialization.warrior;

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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Warrior abilities: War Cry (F) and Whirlwind (G). */
public final class WarriorAbilities {

    private WarriorAbilities() {}

    public static final String WAR_CRY = "warrior_war_cry";

    // ---- War Cry ------------------------------------------------------------

    public static class WarCry extends SpecializationAbility implements Sustained {
        private static final ResourceLocation UNSTOPPABLE =
            ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "warrior_juggernaut_kb");

        public WarCry() {
            super(WAR_CRY, "abyssorigins.ability.warrior_war_cry.name", "abyssorigins.ability.warrior_war_cry.desc", 30);
            icon("⚑"); color(0xFFE0563C);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            int duration = Skills.has(player, "warrior_battle_trance") ? 240 : 160;

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,      duration, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
            if (Skills.has(player, "warrior_battle_trance"))
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true));
            if (Skills.has(player, "warrior_juggernaut")) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, true));
                AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kb != null && !kb.hasModifier(UNSTOPPABLE))
                    kb.addTransientModifier(new AttributeModifier(UNSTOPPABLE, 1.0, AttributeModifier.Operation.ADD_VALUE));
            }

            for (LivingEntity ally : Targeting.alliesAround(player, player.position(), 8))
                if (ally != player) ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true), player);
            for (LivingEntity enemy : Targeting.enemiesAround(player, player.position(), 6))
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), player);

            var level = player.serverLevel();
            Vec3 c = player.position().add(0, 0.2, 0);
            Vfx.shockwave(level, player.position(), 6.5f, Palette.WAR, Palette.WAR_HI, 20);
            Vfx.of(VfxType.SIGIL).at(player.position()).scale(2.2f).extra(4).colors(Palette.WAR, Palette.WAR_HI).duration(26).send(level);
            Vfx.helix(level, player, 2.2f, Palette.WAR, Palette.WAR_HI, 22);
            Fx.sound(level, c, SoundEvents.RAVAGER_ROAR, 0.9f, 1.1f);
            AbilityStates.start(player, WAR_CRY, duration, this);
            return true;
        }

        @Override
        public void onEnd(ServerPlayer player) {
            AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.removeModifier(UNSTOPPABLE);
        }
    }

    // ---- Whirlwind ----------------------------------------------------------

    public static class Whirlwind extends SpecializationAbility {
        public Whirlwind() {
            super("warrior_whirlwind", "abyssorigins.ability.warrior_whirlwind.name", "abyssorigins.ability.warrior_whirlwind.desc", 12);
            icon("✵"); color(0xFFC9C3E6);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            double radius = Skills.has(player, "warrior_cyclone") ? 5.0 : 3.5;
            float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2f;
            boolean infernal = Skills.isOrigin(player, "demon");
            var level = player.serverLevel();
            Vec3 c = player.position();

            for (LivingEntity e : Targeting.enemiesAround(player, c, radius)) {
                e.hurt(Targeting.spell(player), dmg);
                Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize().scale(0.7);
                e.push(push.x, 0.25, push.z);
                e.hurtMarked = true;
                if (Skills.has(player, "warrior_cyclone"))
                    e.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0), player);
                if (infernal) e.igniteForSeconds(4);
            }

            int c1 = infernal ? Palette.INFERNO : Palette.BLADE, c2 = infernal ? Palette.INFERNO_HI : Palette.BLADE_HI;
            Vfx.of(VfxType.SLASH).at(c.add(0, 1.0, 0)).dir(player.getLookAngle()).scale((float) radius).extra(540)
                .colors(c1, c2).duration(14).send(level);
            Vfx.of(VfxType.SLASH).at(c.add(0, 0.55, 0)).dir(player.getLookAngle()).scale((float) radius * 0.75f).extra(400)
                .colors(c1, c2).flags(Vfx.REVERSE).duration(14).send(level);
            Vfx.shockwave(level, c, (float) radius, c1, c2, 12);
            Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
            Fx.sound(level, c, SoundEvents.BREEZE_SHOOT, 0.7f, 0.8f);
            return true;
        }
    }
}
