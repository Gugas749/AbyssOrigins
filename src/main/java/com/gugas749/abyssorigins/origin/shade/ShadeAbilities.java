package com.gugas749.abyssorigins.origin.shade;

import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.api.Sustained;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.AbilityStates;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Shade abilities: Shadow Meld (R) and Umbral Grasp (V). Both spend Umbra. */
public final class ShadeAbilities {

    private ShadeAbilities() {}

    public static final String MELD_ID = "shade_meld";

    // ---- Shadow Meld --------------------------------------------------------

    public static class ShadowMeld extends OriginAbility implements Sustained {
        public ShadowMeld() {
            super(MELD_ID,
                "abyssorigins.ability.shade_meld.name",
                "abyssorigins.ability.shade_meld.desc",
                20, 30);
            icon("☾"); color(0xFF7D93C9);
        }

        @Override
        public int cooldownTicks(ServerPlayer player) {
            return Skills.has(player, "shade_lord_of_night") ? getBaseCooldownTicks() / 2 : getBaseCooldownTicks();
        }

        @Override
        public boolean activate(ServerPlayer player) {
            meld(player, Skills.has(player, "shade_lingering_dark") ? 240 : 160, this);
            return true;
        }

        /** Also used by Shadow Double (free emergency meld). */
        public static void meld(ServerPlayer player, int ticks, Sustained handler) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks + 10, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, false, false));

            // Every mob hunting you loses the trail
            AABB box = player.getBoundingBox().inflate(16);
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, m -> m.getTarget() == player))
                mob.setTarget(null);

            var level = player.serverLevel();
            Vfx.of(VfxType.BURST).at(Fx.chest(player)).scale(1.3f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(Vfx.VOID).duration(14).send(level);
            Fx.sound(level, player.position(), SoundEvents.INK_SAC_USE, 1.0f, 0.5f);
            Fx.sound(level, player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7f, 0.6f);
            AbilityStates.start(player, MELD_ID, ticks, handler);
        }

        @Override
        public void tickActive(ServerPlayer player, int remaining) {
            if (Skills.has(player, "shade_lord_of_night") && player.tickCount % 20 == 0)
                player.heal(1f);
        }

        @Override
        public void onEnd(ServerPlayer player) {
            player.removeEffect(MobEffects.INVISIBILITY);
            Vfx.of(VfxType.BURST).at(Fx.chest(player)).scale(0.9f).colors(Palette.SHADOW, Palette.SHADOW_HI).flags(Vfx.VOID).duration(12).send(player.serverLevel());
        }
    }

    // ---- Umbral Grasp -------------------------------------------------------

    public static class UmbralGrasp extends OriginAbility {
        public UmbralGrasp() {
            super("shade_umbral_grasp",
                "abyssorigins.ability.shade_umbral_grasp.name",
                "abyssorigins.ability.shade_umbral_grasp.desc",
                18, 35);
            icon("☘"); color(0xFF4A5A8A);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            boolean reach = Skills.has(player, "shade_tendril_reach");
            List<LivingEntity> caught = Targeting.enemiesAround(player, player.position(), reach ? 9 : 6);
            if (caught.isEmpty()) return false;

            var level = player.serverLevel();
            for (LivingEntity e : caught) {
                e.hurt(Targeting.spell(player), 4f);
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, reach ? 60 : 40, 0), player);
                Vfx.of(VfxType.TENDRILS).at(e.position()).scale(e.getBbHeight() + 0.7f).extra(5).colors(Palette.SHADOW, Palette.SHADOW_HI)
                    .duration((reach ? 60 : 40) + 10).send(level);
                Vfx.of(VfxType.BEAM).at(player.position().add(0, 0.15, 0)).to(e.position().add(0, 0.15, 0)).extra(0.3f)
                    .colors(Palette.SHADOW, Palette.SHADOW_HI).flags(Vfx.VOID).duration(14).send(level);
            }
            if (Skills.has(player, "shade_soul_siphon")) player.heal(caught.size());

            Vfx.of(VfxType.SIGIL).at(player.position()).scale(reach ? 4f : 3f).extra(7).colors(Palette.SHADOW_HI, 0xB8C8F0).duration(30).send(level);
            Fx.sound(level, player.position(), SoundEvents.EVOKER_FANGS_ATTACK, 1.0f, 0.6f);
            Fx.sound(level, player.position(), SoundEvents.SCULK_CATALYST_BLOOM, 1.0f, 0.7f);
            return true;
        }
    }
}
