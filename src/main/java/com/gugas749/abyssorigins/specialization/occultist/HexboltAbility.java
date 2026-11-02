package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.effect.ModEffects;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Hexbolt: a curse-bolt at the creature you look at — magic damage + Hexed. */
public class HexboltAbility extends SpecializationAbility {

    public HexboltAbility() {
        super("occult_hexbolt",
            "abyssorigins.ability.occult_hexbolt.name",
            "abyssorigins.ability.occult_hexbolt.desc",
            14);
        icon("‡"); color(0xFF8A2BE2);
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.lookTarget(player, 24).orElse(null);
        if (target == null) return false;

        var level = player.serverLevel();
        boolean hexMark = Skills.has(player, "occ_hex_mark");
        float dmg = 5f + (Skills.has(player, "occ_dark_knowledge") ? 2f : 0f);

        target.hurt(Targeting.spell(player), dmg);
        target.addEffect(new MobEffectInstance(ModEffects.HEXED, hexMark ? 200 : 120, hexMark ? 1 : 0), player);

        // Resonances
        if (Skills.isOrigin(player, "voidborn"))
            target.addEffect(new MobEffectInstance(ModEffects.SUNDERED, 100, 0), player);
        if (Skills.isOrigin(player, "vampire")) {
            Skills.addResource(player, 8f);
            player.heal(2f);
        }

        Vec3 from = Fx.hand(player);
        Vec3 to = Fx.chest(target);
        Vfx.beam(level, from, to, 0.32f, Palette.OCCULT, Palette.OCCULT_HI, 12);
        Vfx.burst(level, to, 0.8f, Palette.OCCULT, Palette.OCCULT_HI);
        Vfx.of(VfxType.MARK).follow(target).colors(Palette.OCCULT, Palette.OCCULT_HI).duration(hexMark ? 200 : 120).send(level);
        Fx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.3f);
        Fx.sound(level, to, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.3f, 1.8f);
        return true;
    }
}
