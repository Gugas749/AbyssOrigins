package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.vfx.Palette;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * Origin + class progression.
 *
 * XP sources: hostile kill 10, other kill 3, boss kill 250, advancement 25.
 * Every origin XP grant also feeds class XP (scaled by Human / skill bonuses).
 * Each level grants 1 skill point in the matching tree.
 *
 * Curve: level L → L+1 costs 100 + 50·(L-1). Level 15 (tier 5) ≈ 600 hostile kills.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public class XpEventHandler {

    public static final int POINTS_PER_LEVEL = 1;
    public static final int STARTING_POINTS  = 3;
    public static final int MAX_LEVEL        = 50;

    private static final int XP_MONSTER     = 10;
    private static final int XP_PASSIVE     = 3;
    private static final int XP_BOSS        = 250;
    private static final int XP_ADVANCEMENT = 25;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity killed = event.getEntity();
        if (killed instanceof Player) return;
        if (!AbyssOriginsAPI.hasOrigin(player)) return;

        int xp = killed.getType().is(Tags.EntityTypes.BOSSES) ? XP_BOSS
               : killed instanceof Enemy ? XP_MONSTER : XP_PASSIVE;
        grantXp(player, xp);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AbyssOriginsAPI.hasOrigin(player)) return;
        // Recipe unlocks are advancements too — only count ones with a toast/display.
        if (event.getAdvancement().value().display().isEmpty()) return;
        grantXp(player, XP_ADVANCEMENT);
    }

    public static int xpForLevel(int level) {
        return 100 + Math.max(0, level - 1) * 50;
    }

    public static int xpRequiredToReach(int targetLevel) {
        int total = 0;
        for (int lvl = 1; lvl < targetLevel; lvl++) total += xpForLevel(lvl);
        return total;
    }

    private static int levelFor(int xp, int currentLevel) {
        int level = Math.max(1, currentLevel);
        while (level < MAX_LEVEL && xp >= xpRequiredToReach(level + 1)) level++;
        return level;
    }

    /** Earned XP: applies learning bonuses and feeds the class its share. */
    public static void grantXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        float bonus = Skills.has(player, "human_quick_learner") ? 1.15f : 1f;
        int gained = Math.round(amount * bonus);
        addOriginXp(player, gained);
        grantClassXp(player, gained);
    }

    /** Exactly {@code gained} origin XP — no bonuses, no class share (staff edits). Levels up as usual. */
    public static void addOriginXp(ServerPlayer player, int gained) {
        if (gained <= 0) return;

        PlayerOriginData data   = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        PlayerSkillData  skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

        int oldLevel = data.getOriginLevel();
        data.setOriginXp(data.getOriginXp() + gained);
        int level = levelFor(data.getOriginXp(), oldLevel);
        data.setOriginLevel(level);

        if (level > oldLevel) {
            int pointsGranted = (level - oldLevel) * POINTS_PER_LEVEL;
            skills.addSkillPoints(pointsGranted);
            player.sendSystemMessage(Component.translatable(
                "abyssorigins.message.level_up",
                String.valueOf(level),
                String.valueOf(pointsGranted)));
            celebrate(player);
        }

        ModPackets.syncPlayer(player);
        ModPackets.syncSkills(player);
    }

    /** Class XP — Humans learn crafts faster (Mortal Ingenuity, +25%). */
    public static void grantClassXp(ServerPlayer player, int amount) {
        float mult = Skills.isOrigin(player, "human") ? 1.25f : 1f;
        addClassXp(player, Math.round(amount * mult));
    }

    /** Exactly {@code amount} class XP (staff edits). No-op without a class. */
    public static void addClassXp(ServerPlayer player, int amount) {
        PlayerSpecializationData spec = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        if (!spec.hasSpec() || amount <= 0) return;

        int oldLevel = Math.max(1, spec.getSpecLevel());
        spec.setSpecXp(spec.getSpecXp() + amount);
        int level = levelFor(spec.getSpecXp(), oldLevel);

        if (level > oldLevel) {
            spec.setSpecLevel(level);
            spec.addSkillPoints((level - oldLevel) * POINTS_PER_LEVEL);
            player.sendSystemMessage(Component.translatable("abyssorigins.message.spec_level_up",
                String.valueOf(level), String.valueOf((level - oldLevel) * POINTS_PER_LEVEL)));
            celebrate(player);
        }
        ModPackets.syncSpecialization(player);
    }

    /** Sets the origin level outright (XP snaps to the level's floor); raising it awards the points. */
    public static void setOriginLevel(ServerPlayer player, int level) {
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
        level = Math.max(1, Math.min(MAX_LEVEL, level));
        int old = data.getOriginLevel();
        data.setOriginLevel(level);
        data.setOriginXp(xpRequiredToReach(level));
        if (level > old) player.getData(SkillAttachments.PLAYER_SKILLS.get()).addSkillPoints((level - old) * POINTS_PER_LEVEL);
        ModPackets.syncPlayer(player);
        ModPackets.syncSkills(player);
    }

    /** Class equivalent of {@link #setOriginLevel}. @return false without a class. */
    public static boolean setClassLevel(ServerPlayer player, int level) {
        PlayerSpecializationData spec = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        if (!spec.hasSpec()) return false;
        level = Math.max(1, Math.min(MAX_LEVEL, level));
        int old = Math.max(1, spec.getSpecLevel());
        spec.setSpecLevel(level);
        spec.setSpecXp(xpRequiredToReach(level));
        if (level > old) spec.addSkillPoints((level - old) * POINTS_PER_LEVEL);
        ModPackets.syncSpecialization(player);
        return true;
    }

    private static void celebrate(ServerPlayer player) {
        Vfx.helix(player.serverLevel(), player, 2.6f, 0xF2C14E, 0xFFF4C8, 30);
        Vfx.shockwave(player.serverLevel(), player.position(), 3.5f, 0xF2C14E, 0xFFF4C8, 18);
        Fx.sound(player.level(), player.position(), SoundEvents.PLAYER_LEVELUP, 0.6f, 0.7f);
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
