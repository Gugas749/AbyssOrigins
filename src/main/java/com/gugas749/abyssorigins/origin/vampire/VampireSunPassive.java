package com.gugas749.abyssorigins.origin.vampire;

import com.gugas749.abyssorigins.api.OriginPassive;
import com.gugas749.abyssorigins.client.ClientOriginData;
import com.gugas749.abyssorigins.item.ModItems;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class VampireSunPassive extends OriginPassive {

    private int tickCounter = 0;

    public VampireSunPassive() {
        super("vampire_sun", "Sun Vulnerability",
            "You burn in direct sunlight. Only an Umbrella or the Daywalker skill can protect you.");
    }

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide()) return;

        // Protection 1: Umbrella in main hand
        if (player.getMainHandItem().is(ModItems.UMBRELLA.get())) {
            if (player.isOnFire()) player.clearFire();
            return;
        }

        // Protection 2: Daywalker skill unlocked
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        if (skills.hasSkill("vamp_ascended_daywalker")) {
            if (player.isOnFire()) player.clearFire();
            return;
        }

        // NO helmet protection — only umbrella or daywalker
        if (++tickCounter < 20) return;
        tickCounter = 0;

        long dayTime = player.level().getDayTime() % 24000;
        boolean isDaytime = dayTime < 12500 || dayTime > 23500;

        if (isDaytime
                && player.level().canSeeSky(player.blockPosition())
                && !player.level().isRaining()
                && !player.level().isThundering()) {
            player.hurt(player.level().damageSources().onFire(), 2.0f);
            player.igniteForSeconds(1);
        }
    }

    @Override
    public void onEquip(Player player) {
        player.displayClientMessage(Component.literal(
            ChatFormatting.DARK_RED + "As a Vampire, you burn in sunlight. "
            + "Carry an \u00a7bUmbrella\u00a7c or unlock \u00a76Daywalker\u00a7c to survive."), false);
    }
}
