package com.gugas749.abyssorigins.skilltree.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.api.StatBonus;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.data.SkillNode;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Owns every permanent attribute modifier the mod puts on a player.
 *
 * Instead of applying modifiers one-by-one as skills unlock (which leaked
 * modifiers on reset and lost them on respawn), refresh() strips all of our
 * modifiers and rebuilds them from: origin base stats, the origin's unlocked
 * skill nodes, class base stats and the class's unlocked skill nodes.
 *
 * Call refresh() after any change to origin, class or unlocked skills, and on
 * login / respawn. Behavioural (non-stat) skill effects live in PowerHooks.
 */
public class SkillEffectHandler {

    private static final String P_ORIGIN = "origin/";
    private static final String P_CLASS  = "class/";
    private static final String P_SKILL  = "skill/";

    /** Modifier ids written by pre-0.1 versions (one flat id per skill) — removed on refresh. */
    private static final Set<String> LEGACY_IDS = Set.of(
        "human_tough_skin","human_iron_will","human_fighter","human_endurance","human_swift",
        "vamp_vitality","vamp_agility","vamp_fangs","vamp_resilience","vamp_resilience_hp",
        "vamp_swiftness","vamp_blood_frenzy_dmg","vamp_blood_frenzy_arm","vamp_blood_drain_skill",
        "vamp_shadow_stalker","vamp_shadow_cloak","vamp_immortal_flesh","vamp_immortal_kb",
        "vamp_blood_lord","vamp_blood_lord_as","vamp_crimson_tide","vamp_shadow_master",
        "vamp_ancient_one","vamp_ancient_one_arm","vamp_ancient_one_t","vamp_mist_form",
        "vamp_hybrid_dmg","vamp_hybrid_hp","vamp_hybrid_spd","vamp_assassin","vamp_assassin_arm",
        "vamp_assassin_hp","vamp_bloodshadow","vamp_nosferatu_dmg","vamp_nosferatu_hp",
        "vamp_nosferatu_spd","vamp_bloodpact","vamp_daywalker","vamp_progenitor","vamp_progenitor_hp"
    );

    private SkillEffectHandler() {}

    public static void refresh(ServerPlayer player) {
        removeAll(player);

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin != null) {
            List<StatBonus> base = new ArrayList<>(origin.getBaseStats());
            if (origin.getHealthModifier() != 1f)
                base.add(StatBonus.percent(Attributes.MAX_HEALTH, origin.getHealthModifier() - 1f));
            if (origin.getSpeedModifier() != 1f)
                base.add(StatBonus.percent(Attributes.MOVEMENT_SPEED, origin.getSpeedModifier() - 1f));
            if (origin.getDamageModifier() != 1f)
                base.add(StatBonus.percent(Attributes.ATTACK_DAMAGE, origin.getDamageModifier() - 1f));
            apply(player, P_ORIGIN + origin.getPath(), base);

            PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
            SkillTreeRegistry.getTree(origin.getId()).ifPresent(tree -> applyTree(player, tree, skills.getUnlockedSkills()));
        }

        PlayerSpecializationData spec = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        if (spec.hasSpec()) {
            Specialization s = SpecializationRegistry.get(spec.getSpecId()).orElse(null);
            if (s != null) {
                apply(player, P_CLASS + s.getPath(), s.getBaseStats());
                if (s.getSkillTree() != null) applyTree(player, s.getSkillTree(), spec.getUnlockedSkills());
            }
        }

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void applyTree(ServerPlayer player, SkillTree tree, List<String> unlocked) {
        for (String id : unlocked) {
            SkillNode node = tree.getSkill(id);
            if (node != null && !node.getStats().isEmpty()) apply(player, P_SKILL + id, node.getStats());
        }
    }

    private static void apply(ServerPlayer player, String prefix, List<StatBonus> bonuses) {
        for (int i = 0; i < bonuses.size(); i++) {
            StatBonus b = bonuses.get(i);
            AttributeInstance inst = player.getAttribute(b.attribute());
            if (inst == null) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, prefix + "/" + i);
            inst.removeModifier(id);
            inst.addPermanentModifier(new AttributeModifier(id, b.amount(), b.operation()));
        }
    }

    /** Strips every origin/class/skill modifier (current and legacy) from the player. */
    public static void removeAll(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance inst = player.getAttribute(holder);
            if (inst == null) return;
            List<ResourceLocation> doomed = new ArrayList<>();
            for (AttributeModifier m : inst.getModifiers()) {
                ResourceLocation id = m.id();
                if (!id.getNamespace().equals(Abyssorigins.MODID)) continue;
                String path = id.getPath();
                if (path.startsWith(P_ORIGIN) || path.startsWith(P_CLASS) || path.startsWith(P_SKILL)
                        || LEGACY_IDS.contains(path)) doomed.add(id);
            }
            doomed.forEach(inst::removeModifier);
        });
    }

    /** @deprecated kept for addon compatibility — use {@link #refresh(ServerPlayer)}. */
    @Deprecated
    public static void removeAllSkillEffects(ServerPlayer player) { removeAll(player); }
}
