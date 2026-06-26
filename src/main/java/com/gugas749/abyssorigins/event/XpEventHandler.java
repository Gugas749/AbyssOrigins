package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = Abyssorigins.MODID)
public class XpEventHandler {

    public static final int POINTS_PER_LEVEL = 1;
    public static final int STARTING_POINTS  = 3;

    private static final int XP_MONSTER = 10;
    private static final int XP_PASSIVE  = 3;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity killed = event.getEntity();
        if (killed instanceof Player) return;
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;
        grantXp(player, killed instanceof Monster ? XP_MONSTER : XP_PASSIVE);
    }

    public static int xpForLevel(int level) {
        return 500 + (level - 1) * 1000;
    }

    public static int xpRequiredToReach(int targetLevel) {
        int total = 0;
        for (int lvl = 1; lvl < targetLevel; lvl++) total += xpForLevel(lvl);
        return total;
    }

    public static void grantXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;

        PlayerOriginData data   = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PlayerSkillData  skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

        int oldLevel = data.getOriginLevel();
        int newXp    = data.getOriginXp() + amount;
        data.setOriginXp(newXp);

        int level = oldLevel;
        while (level < 50) {
            if (newXp < xpRequiredToReach(level + 1)) break;
            level++;
        }
        data.setOriginLevel(level);

        if (level > oldLevel) {
            int pointsGranted = (level - oldLevel) * POINTS_PER_LEVEL;
            skills.addSkillPoints(pointsGranted);
            player.sendSystemMessage(Component.translatable(
                "abyssorigins.message.level_up",
                String.valueOf(level),
                String.valueOf(pointsGranted)));
        }

        ModPackets.syncPlayer(player);
        ModPackets.syncSkills(player);
    }

    public static void grantStartingPoints(ServerPlayer player) {
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        if (skills.getSkillPoints() == 0 && skills.getUnlockedSkills().isEmpty()) {
            skills.addSkillPoints(STARTING_POINTS);
            ModPackets.syncSkills(player);
            player.sendSystemMessage(Component.translatable(
                "abyssorigins.message.skill_points_starting",
                String.valueOf(STARTING_POINTS)));
        }
    }
}
