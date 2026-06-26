package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.world.entity.player.Player;

public class VampireSunPassive extends OriginPassive {
    private int tickCounter = 0;
    public VampireSunPassive() {
        super("vampire_sun",
            "abyssorigins.passive.vampire_sun.name",
            "abyssorigins.passive.vampire_sun.desc");
    }
    @Override public void onTick(Player player) {
        if (player.level().isClientSide()) return;
        if (player.getMainHandItem().is(ModItems.UMBRELLA.get())) {
            if (player.isOnFire()) player.clearFire(); return;
        }
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        if (skills.hasSkill("vamp_ascended_daywalker")) {
            if (player.isOnFire()) player.clearFire(); return;
        }
        if (++tickCounter < 20) return; tickCounter = 0;
        long dayTime = player.level().getDayTime() % 24000;
        boolean isDaytime = dayTime < 12500 || dayTime > 23500;
        if (isDaytime && player.level().canSeeSky(player.blockPosition())
                && !player.level().isRaining() && !player.level().isThundering()) {
            player.hurt(player.level().damageSources().onFire(), 2.0f);
            player.igniteForSeconds(1);
        }
    }
    @Override public void onEquip(Player player) {}
}
