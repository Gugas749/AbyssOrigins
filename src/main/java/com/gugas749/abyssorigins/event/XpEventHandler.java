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

/**
 * Grants origin XP when a player kills a mob, then checks for level-ups.
 *
 * XP granted per kill:
 *   Monster (hostile mob)  : 10 XP
 *   Other living entity    : 3  XP
 *
 * Level formula: level = 1 + (totalXp / XP_PER_LEVEL)
 * XP_PER_LEVEL = 100
 * Each level-up grants POINTS_PER_LEVEL = 1 skill point.
 * Starting points (3) are granted once when origin is first assigned — see OriginEventHandler.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class XpEventHandler {

    public static final int XP_PER_LEVEL      = 100;
    public static final int POINTS_PER_LEVEL  = 1;
    public static final int STARTING_POINTS   = 3;

    private static final int XP_MONSTER = 10;
    private static final int XP_PASSIVE  = 3;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // Only fires when killed by a player
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity killed = event.getEntity();

        // Don't grant XP for killing other players
        if (killed instanceof Player) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        int xpGained = killed instanceof Monster ? XP_MONSTER : XP_PASSIVE;
        grantXp(player, xpGained);
    }

    public static int xpForLevel(int level) {
        return 500 + (level - 1) * 1000;
    }

    public static int xpRequiredToReach(int targetLevel) {
        int total = 0;
        for (int lvl = 1; lvl < targetLevel; lvl++) {
            total += xpForLevel(lvl);
        }
        return total;
    }

    /**
     * Grant XP, check for level-up, sync to client.
     * Called from LivingDeathEvent and from the addxp command.
     */
    public static void grantXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;

        PlayerOriginData data   = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PlayerSkillData  skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

        int oldLevel = data.getOriginLevel();
        int newXp    = data.getOriginXp() + amount;
        data.setOriginXp(newXp);

        // Walk level-ups one at a time (player could gain enough XP to skip multiple levels)
        int level = oldLevel;
        while (level < 50) {
            int threshold = xpRequiredToReach(level + 1);
            if (newXp < threshold) break;
            level++;
        }

        data.setOriginLevel(level);

        if (level > oldLevel) {
            int levelsGained  = level - oldLevel;
            int pointsGranted = levelsGained * POINTS_PER_LEVEL;
            skills.addSkillPoints(pointsGranted);

            player.sendSystemMessage(Component.literal(
                    "§6[AbyssOrigins] §aOrigin level up! §eLevel §b" + level
                            + "§e. §a+" + pointsGranted + " skill point"
                            + (pointsGranted > 1 ? "s" : "") + "§e. Press §bK§e to open the skill tree."));
        }

        ModPackets.syncPlayer(player);
        ModPackets.syncSkills(player);
    }

    /**
     * Grants starting skill points when a player is assigned an origin for the first time.
     * Called from OriginEventHandler.onPlayerLogin and from OriginCommand.setOrigin.
     */
    public static void grantStartingPoints(ServerPlayer player) {
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

        // Only grant if this player has never had skill points before
        if (skills.getSkillPoints() == 0 && skills.getUnlockedSkills().isEmpty()) {
            skills.addSkillPoints(STARTING_POINTS);
            ModPackets.syncSkills(player);
            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §eYou have §b" + STARTING_POINTS
                + " §eskill points to spend. Press §bK§e to open the skill tree."));
        }
    }
}
