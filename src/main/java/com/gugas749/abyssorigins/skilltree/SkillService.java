package com.gugas749.abyssorigins.skilltree;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.power.Skills;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import com.gugas749.abyssorigins.skilltree.registry.SkillEffectHandler;
import com.gugas749.abyssorigins.skilltree.registry.SkillTreeRegistry;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Server-authoritative skill unlocking and respec for both origin and class trees.
 * The client predicts unlocks for responsiveness; this re-validates everything.
 */
public final class SkillService {

    private SkillService() {}

    public static void unlockOriginSkill(ServerPlayer player, String nodeId) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;
        SkillTree tree = SkillTreeRegistry.getTree(origin.getId()).orElse(null);
        if (tree == null) return;

        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        int level = player.getData(OriginAttachments.PLAYER_ORIGIN.get()).getOriginLevel();

        if (tryUnlock(player, tree, nodeId, skills.getUnlockedSkills(), level, skills.getSkillPoints())) {
            SkillNode node = tree.getSkill(nodeId);
            skills.spendSkillPoints(node.getPointCost());
            skills.unlockSkill(nodeId);
            finish(player, node);
        }
        ModPackets.syncSkills(player);
    }

    public static void unlockClassSkill(ServerPlayer player, String nodeId) {
        PlayerSpecializationData data = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        Specialization spec = Skills.specOf(player);
        if (spec == null || spec.getSkillTree() == null) return;

        if (tryUnlock(player, spec.getSkillTree(), nodeId, data.getUnlockedSkills(), data.getSpecLevel(), data.getSkillPoints())) {
            SkillNode node = spec.getSkillTree().getSkill(nodeId);
            data.spendSkillPoints(node.getPointCost());
            data.unlockSkill(nodeId);
            finish(player, node);
        }
        ModPackets.syncSpecialization(player);
    }

    private static boolean tryUnlock(ServerPlayer player, SkillTree tree, String nodeId,
                                     List<String> unlocked, int level, int points) {
        SkillNode node = tree.getSkill(nodeId);
        if (node == null || unlocked.contains(nodeId)) return false;

        if (tree.excludedBy(unlocked).contains(nodeId)) {
            player.sendSystemMessage(Component.translatable("abyssorigins.message.skill_excluded"));
            return false;
        }
        if (level < node.getTier().getLevelRequired()) {
            player.sendSystemMessage(Component.translatable("abyssorigins.message.skill_level_required",
                node.getTier().getLevelRequired()));
            return false;
        }
        if (!node.prerequisitesMet(new java.util.HashSet<>(unlocked))) {
            player.sendSystemMessage(Component.translatable("abyssorigins.message.skill_prereqs_missing"));
            return false;
        }
        if (points < node.getPointCost()) {
            player.sendSystemMessage(Component.translatable("abyssorigins.message.skill_not_enough_points",
                String.valueOf(node.getPointCost()), String.valueOf(points)));
            return false;
        }
        return true;
    }

    private static void finish(ServerPlayer player, SkillNode node) {
        SkillEffectHandler.refresh(player);
        ModPackets.syncPlayer(player);
        if (node.grantsAbility()) {                    // a newly learned power drops into a free slot
            Loadouts.autoBind(player, node.getGrantedAbility());
            ModPackets.syncAbilities(player);
        }

        var level = player.serverLevel();
        Vfx.helix(level, player, 2.4f, 0x8E7BD8, 0xE6E1F5, 26);
        if (node.isCapstone())
            Vfx.of(VfxType.PILLAR).at(player.position()).scale(9).extra(1.1f).colors(0xF2C14E, 0xFFFFFF).duration(36).send(level);
        Fx.sound(level, player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f, node.isCapstone() ? 0.6f : 1.2f);
        if (node.isCapstone()) Fx.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 0.7f);

        player.sendSystemMessage(Component.translatable("abyssorigins.message.skill_unlocked", node.getDisplayName()));
    }

    // ---- Respec ------------------------------------------------------------

    /** Refunds every origin skill point spent. Returns points refunded. */
    public static int respecOrigin(ServerPlayer player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
        int refund = 0;
        if (origin != null) {
            SkillTree tree = SkillTreeRegistry.getTree(origin.getId()).orElse(null);
            if (tree != null) for (String id : skills.getUnlockedSkills()) {
                SkillNode n = tree.getSkill(id);
                if (n != null) refund += n.getPointCost();
            }
        }
        skills.clearSkills();
        skills.addSkillPoints(refund);
        Loadouts.prune(player);                        // forget powers the refunded nodes taught
        SkillEffectHandler.refresh(player);
        ModPackets.syncAll(player);
        return refund;
    }

    public static int respecClass(ServerPlayer player) {
        PlayerSpecializationData data = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        Specialization spec = Skills.specOf(player);
        int refund = 0;
        if (spec != null && spec.getSkillTree() != null) for (String id : data.getUnlockedSkills()) {
            SkillNode n = spec.getSkillTree().getSkill(id);
            if (n != null) refund += n.getPointCost();
        }
        data.clearSkills();
        data.addSkillPoints(refund);
        Loadouts.prune(player);
        SkillEffectHandler.refresh(player);
        ModPackets.syncAll(player);
        return refund;
    }
}
