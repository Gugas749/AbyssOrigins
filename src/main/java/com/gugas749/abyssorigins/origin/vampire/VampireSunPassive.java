package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.item.ModItems;
import net.minecraft.world.entity.player.Player;

public class VampireSunPassive extends OriginPassive {

    public VampireSunPassive() {
        super("vampire_sun", "Sun Vulnerability",
            "You burn in direct sunlight unless sheltered or holding an umbrella.");
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;

        // Holding umbrella = protected
        if (player.getMainHandItem().is(ModItems.UMBRELLA.get())) {
            if (player.isOnFire()) player.clearFire();
            return;
        }

        // Check if in direct sunlight: daytime, sky-exposed, no rain/thunder blocking
        if (isInDirectSunlight(player)) {
            player.igniteForSeconds(2);
        }
    }

    private boolean isInDirectSunlight(Player player) {
        if (!player.level().isDay()) return false;
        if (player.level().isRaining() || player.level().isThundering()) return false;
        // Sky light level 15 = direct sunlight
        return player.level().canSeeSky(player.blockPosition());
    }
}
