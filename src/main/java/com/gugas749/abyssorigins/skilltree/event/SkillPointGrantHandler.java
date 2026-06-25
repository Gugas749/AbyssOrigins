package com.gugas749.abyssorigins.skilltree.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Grants 1 skill point every time the player's origin level increases.
 * Also grants starting skill points when a new origin is assigned (5 points).
 *
 * XP is earned by: killing mobs, completing advancements, or via OP command.
 * Level thresholds: level = xp / 100  (so 100 xp = level 2, 200 xp = level 3, etc.)
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class SkillPointGrantHandler {

    public static final int XP_PER_LEVEL = 100;
    public static final int STARTING_POINTS = 3;
    public static final int POINTS_PER_LEVEL = 1;

    /**
     * Called from OriginEventHandler.onPlayerLogin to grant starting points
     * if this is the first time the player has had an origin.
     */
    public static void grantStartingPoints(ServerPlayer player) {
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        if (skills.getSkillPoints() == 0 && skills.getUnlockedSkills().isEmpty()) {
            skills.addSkillPoints(STARTING_POINTS);
            ModPackets.syncSkills(player);
            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §eYou have §b" + STARTING_POINTS
                + " §eskill points to spend. Press §bK§e to open the skill tree."));
        }
    }

    /**
     * Checks if the player has levelled up based on their XP total,
     * and grants skill points accordingly. Call after any XP change.
     */
    public static void checkLevelUp(ServerPlayer player) {
        PlayerOriginData data  = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

        int newLevel = Math.min(50, 1 + data.getOriginXp() / XP_PER_LEVEL);

        if (newLevel > data.getOriginLevel()) {
            int gained = newLevel - data.getOriginLevel();
            data.setOriginLevel(newLevel);
            skills.addSkillPoints(gained * POINTS_PER_LEVEL);

            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §aOrigin level up! §eNow level §b" + newLevel
                + "§e. +" + (gained * POINTS_PER_LEVEL) + " skill point"
                + (gained > 1 ? "s" : "") + "."));

            ModPackets.syncPlayer(player);
            ModPackets.syncSkills(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerKill(PlayerEvent.PlayerChangedDimensionEvent event) {
        // placeholder — real XP grant comes from OriginCommand addxp
        // or future mob kill XP handler
    }
}
