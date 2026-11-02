package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.item.gear.Gear;
import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.vfx.Vfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Sun Vulnerability: 2 damage/s in direct sunlight (no rain, sky visible).
 * Umbrella or Daywalker grant immunity; Ancient One halves it; Blood Pact triples it.
 */
public class VampireSunPassive extends OriginPassive {
    public VampireSunPassive() {
        super("vampire_sun",
            "abyssorigins.passive.vampire_sun.name",
            "abyssorigins.passive.vampire_sun.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        if (player.getMainHandItem().is(ModItems.UMBRELLA.get()) || player.getOffhandItem().is(ModItems.UMBRELLA.get())
                || Skills.has(player, "vamp_ascended_daywalker")) {
            if (player.isOnFire()) player.clearFire();
            return;
        }
        if (player.tickCount % 20 != 0 || player.isCreative() || player.isSpectator()) return;

        long dayTime = player.level().getDayTime() % 24000;
        boolean isDaytime = dayTime < 12500 || dayTime > 23500;
        if (isDaytime && player.level().canSeeSky(player.blockPosition())
                && !player.level().isRaining() && !player.level().isThundering()) {
            float dmg = 2.0f;
            if (Skills.has(player, "vamp_ancient_one"))         dmg *= 0.5f;
            if (Skills.has(player, "vamp_forbidden_bloodpact")) dmg *= 3.0f;
            if (Gear.fullSet(player, "vampire"))         dmg *= 0.5f;   // Nocturne Regalia
            player.hurt(player.level().damageSources().onFire(), dmg);
            player.igniteForSeconds(1);
            if (player.level() instanceof ServerLevel sl)
                Vfx.sparks(sl, Fx.chest(player), 0.5f, 0xFFB050, 0xFFF0C0);
        }
    }
}
