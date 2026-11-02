package com.gugas749.abyssorigins.specialization.ranger;

import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.network.packet.RevealPacket;
import com.gugas749.abyssorigins.power.Delayed;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ranger abilities: Hunter's Mark (F) and Volley (G). */
public final class RangerAbilities {

    private RangerAbilities() {}

    public static final int MARK_RGB = 0x5FD37A;

    // ---- Hunter's Mark ------------------------------------------------------

    public static class HuntersMark extends SpecializationAbility {
        public HuntersMark() {
            super("ranger_hunters_mark", "abyssorigins.ability.ranger_hunters_mark.name", "abyssorigins.ability.ranger_hunters_mark.desc", 15);
            icon("⌖"); color(0xFF5FD37A);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            LivingEntity target = Targeting.lookTarget(player, 48).orElse(null);
            if (target == null) return false;

            mark(player, target);
            if (Skills.has(player, "ranger_apex_hunter"))
                for (LivingEntity e : Targeting.enemiesAround(player, target.position(), 5))
                    if (e != target) mark(player, e);

            Vfx.of(VfxType.BEAM).at(Fx.hand(player)).to(Fx.chest(target)).extra(0.08f)
                .colors(Palette.HUNT, Palette.HUNT_HI).duration(10).sendTo(player);
            Fx.sound(player.level(), player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.4f);
            return true;
        }

        private static void mark(ServerPlayer player, LivingEntity target) {
            boolean predator = Skills.has(player, "ranger_predator_mark");
            int ticks = predator ? 400 : 240;
            target.addEffect(new MobEffectInstance(ModEffects.HUNTERS_MARK, ticks, predator ? 1 : 0), player);
            RevealPacket.send(player, target, ticks, MARK_RGB);    // visible to the Ranger only
            ServerLevel level = player.serverLevel();
            Vfx.of(VfxType.MARK).follow(target).colors(Palette.HUNT, Palette.HUNT_HI).duration(ticks).sendTo(player);
        }
    }

    // ---- Volley -------------------------------------------------------------

    public static class Volley extends SpecializationAbility {
        public Volley() {
            super("ranger_volley", "abyssorigins.ability.ranger_volley.name", "abyssorigins.ability.ranger_volley.desc", 20);
            icon("➶"); color(0xFF9BD36B);
        }

        @Override
        public boolean activate(ServerPlayer player) {
            fire(player);
            if (Skills.has(player, "ranger_storm_of_arrows"))
                Delayed.schedule(20, () -> { if (player.isAlive()) fire(player); });
            return true;
        }

        private static void fire(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            int count = Skills.has(player, "ranger_split_volley") ? 8 : 5;
            float spread = 40f;
            boolean broadhead = Skills.has(player, "ranger_broadhead");

            for (int i = 0; i < count; i++) {
                float yawOffset = -spread / 2 + spread * i / (count - 1);
                Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
                arrow.setBaseDamage(broadhead ? 4.0 : 2.0);
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                if (broadhead)                              arrow.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0));
                if (Skills.has(player, "ranger_trapper"))   arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                if (Skills.isOrigin(player, "voidborn"))    arrow.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 80, 0)); // Rift Arrows
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0f, 2.6f, 0.5f);
                level.addFreshEntity(arrow);
                Vfx.of(VfxType.TRAIL).follow(arrow).scale(0.3f).colors(Palette.HUNT, Palette.HUNT_HI).duration(40).send(level);
            }
            Vfx.burst(level, player.getEyePosition().add(player.getLookAngle().scale(1.2)), 0.6f, Palette.HUNT, Palette.HUNT_HI);
            Fx.sound(level, player.position(), SoundEvents.ARROW_SHOOT, 1.0f, 0.8f);
            Fx.sound(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 0.7f, 1.2f);
        }
    }
}
