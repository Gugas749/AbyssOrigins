package com.gugas749.abyssorigins.skilltree.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Applies permanent attribute changes when a skill node is unlocked.
 * Tick-based / conditional effects are handled in SkillPassiveTickHandler.
 */
public class SkillEffectHandler {

    private SkillEffectHandler() {}

    public static void onSkillUnlocked(ServerPlayer player, String effectId) {
        Abyssorigins.LOGGER.info("[AbyssOrigins] Applying skill '{}' for {}", effectId, player.getName().getString());

        switch (effectId) {

            // ============================================================
            // HUMAN SKILLS
            // ============================================================
            case "human_tough_skin"   -> addAttr(player, Attributes.ARMOR,           "human_tough_skin",  2.0,  AttributeModifier.Operation.ADD_VALUE);
            case "human_iron_will"    -> addAttr(player, Attributes.MAX_HEALTH,       "human_iron_will",   4.0,  AttributeModifier.Operation.ADD_VALUE);
            case "human_quick_learner"-> Abyssorigins.LOGGER.debug("human_quick_learner — tick handler");
            case "human_fighter"      -> addAttr(player, Attributes.ATTACK_DAMAGE,   "human_fighter",     1.0,  AttributeModifier.Operation.ADD_VALUE);
            case "human_endurance"    -> addAttr(player, Attributes.MAX_HEALTH,       "human_endurance",   6.0,  AttributeModifier.Operation.ADD_VALUE);
            case "human_swift"        -> addAttr(player, Attributes.MOVEMENT_SPEED,  "human_swift",       0.04, AttributeModifier.Operation.ADD_VALUE);
            case "human_last_stand"   -> Abyssorigins.LOGGER.debug("human_last_stand — tick handler");

            // ============================================================
            // VAMPIRE — TIER 1
            // ============================================================
            case "vamp_vitality"      -> addAttr(player, Attributes.MAX_HEALTH,       "vamp_vitality",     4.0,  AttributeModifier.Operation.ADD_VALUE);  // +2 hearts
            case "vamp_agility"       -> addAttr(player, Attributes.MOVEMENT_SPEED,  "vamp_agility",      0.025,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_hunger"        -> Abyssorigins.LOGGER.debug("vamp_hunger — tick handler (drain reduction)");

            // ============================================================
            // VAMPIRE — TIER 2
            // ============================================================
            case "vamp_fangs"         -> addAttr(player, Attributes.ATTACK_DAMAGE,   "vamp_fangs",        0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_resilience"    -> { addAttr(player, Attributes.ARMOR,          "vamp_resilience",   2.0,  AttributeModifier.Operation.ADD_VALUE);
                                           addAttr(player, Attributes.MAX_HEALTH,     "vamp_resilience_hp",2.0,  AttributeModifier.Operation.ADD_VALUE); }
            case "vamp_swiftness"     -> addAttr(player, Attributes.MOVEMENT_SPEED,  "vamp_swiftness",    0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_senses"        -> Abyssorigins.LOGGER.debug("vamp_senses — special (future: life sense)");
            case "vamp_charm"         -> Abyssorigins.LOGGER.debug("vamp_charm — special (future: mesmerize)");

            // ============================================================
            // VAMPIRE — TIER 3
            // ============================================================
            case "vamp_blood_frenzy"  -> { addAttr(player, Attributes.ATTACK_DAMAGE, "vamp_blood_frenzy_dmg", 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                                           addAttr(player, Attributes.ARMOR,          "vamp_blood_frenzy_arm",-2.0, AttributeModifier.Operation.ADD_VALUE); }
            case "vamp_blood_drain_skill" -> { addAttr(player, Attributes.ATTACK_DAMAGE, "vamp_blood_drain_skill", 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE); }
            case "vamp_shadow_stalker"-> addAttr(player, Attributes.ATTACK_DAMAGE,   "vamp_shadow_stalker", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_shadow_cloak"  -> addAttr(player, Attributes.MOVEMENT_SPEED,  "vamp_shadow_cloak",  0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_immortal_flesh"-> { addAttr(player, Attributes.MAX_HEALTH,    "vamp_immortal_flesh",10.0, AttributeModifier.Operation.ADD_VALUE);
                                           addAttr(player, Attributes.KNOCKBACK_RESISTANCE, "vamp_immortal_kb", 0.20, AttributeModifier.Operation.ADD_VALUE); }
            case "vamp_rapid_regen"   -> Abyssorigins.LOGGER.debug("vamp_rapid_regen — tick handler");
            case "vamp_night_hunter"  -> Abyssorigins.LOGGER.debug("vamp_night_hunter — tick handler");
            case "vamp_bat_mastery"   -> Abyssorigins.LOGGER.debug("vamp_bat_mastery — handled in BatFormAbility");

            // ============================================================
            // VAMPIRE — TIER 4
            // ============================================================
            case "vamp_blood_lord"    -> { addAttr(player, Attributes.ATTACK_DAMAGE, "vamp_blood_lord",    0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                                           addAttr(player, Attributes.ATTACK_SPEED,  "vamp_blood_lord_as", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE); }
            case "vamp_crimson_tide"  -> addAttr(player, Attributes.ATTACK_DAMAGE,   "vamp_crimson_tide",  0.12, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_shadow_master" -> addAttr(player, Attributes.ATTACK_DAMAGE,   "vamp_shadow_master", 0.18, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_ancient_one"   -> { addAttr(player, Attributes.MAX_HEALTH,    "vamp_ancient_one",   8.0,  AttributeModifier.Operation.ADD_VALUE);
                                           addAttr(player, Attributes.ARMOR,          "vamp_ancient_one_arm",4.0, AttributeModifier.Operation.ADD_VALUE);
                                           addAttr(player, Attributes.ARMOR_TOUGHNESS,"vamp_ancient_one_t", 2.0, AttributeModifier.Operation.ADD_VALUE); }
            case "vamp_mist_form"     -> addAttr(player, Attributes.MOVEMENT_SPEED,  "vamp_mist_form",     0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "vamp_hybrid_predator"->{ addAttr(player, Attributes.ATTACK_DAMAGE, "vamp_hybrid_dmg",    0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                                           addAttr(player, Attributes.MAX_HEALTH,     "vamp_hybrid_hp",     4.0,  AttributeModifier.Operation.ADD_VALUE);
                                           addAttr(player, Attributes.MOVEMENT_SPEED, "vamp_hybrid_spd",    0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE); }
            case "vamp_specialist_assassin" -> { addAttr(player, Attributes.ATTACK_DAMAGE,"vamp_assassin",  0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                                                 addAttr(player, Attributes.ARMOR,         "vamp_assassin_arm",-4.0,AttributeModifier.Operation.ADD_VALUE);
                                                 addAttr(player, Attributes.MAX_HEALTH,    "vamp_assassin_hp", -4.0,AttributeModifier.Operation.ADD_VALUE); }
            case "vamp_synergy_bloodshadow" -> addAttr(player, Attributes.ATTACK_DAMAGE,"vamp_bloodshadow",  0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            // ============================================================
            // VAMPIRE — TIER 5
            // ============================================================
            case "vamp_nosferatu"     -> { addAttr(player, Attributes.ATTACK_DAMAGE, "vamp_nosferatu_dmg", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                                           addAttr(player, Attributes.MAX_HEALTH,     "vamp_nosferatu_hp",  6.0,  AttributeModifier.Operation.ADD_VALUE);
                                           addAttr(player, Attributes.MOVEMENT_SPEED, "vamp_nosferatu_spd", 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE); }
            case "vamp_forbidden_bloodpact" -> { addAttr(player, Attributes.ATTACK_DAMAGE,"vamp_bloodpact", 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE); }
            case "vamp_ascended_daywalker"  -> { addAttr(player, Attributes.MAX_HEALTH,   "vamp_daywalker", 4.0,  AttributeModifier.Operation.ADD_VALUE); }
            case "vamp_ascended_progenitor" -> { addAttr(player, Attributes.ATTACK_DAMAGE,"vamp_progenitor",0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                                                 addAttr(player, Attributes.MAX_HEALTH,   "vamp_progenitor_hp",8.0,AttributeModifier.Operation.ADD_VALUE); }

            default -> Abyssorigins.LOGGER.warn("[AbyssOrigins] Unknown skill effect: '{}'", effectId);
        }

        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    public static void removeAllSkillEffects(ServerPlayer player) {
        // All modifier IDs we've registered — used when origin is reset
        String[] ids = {
            "human_tough_skin","human_iron_will","human_fighter","human_endurance","human_swift",
            "vamp_vitality","vamp_agility","vamp_fangs","vamp_resilience","vamp_resilience_hp",
            "vamp_swiftness","vamp_blood_frenzy_dmg","vamp_blood_frenzy_arm","vamp_blood_drain_skill",
            "vamp_shadow_stalker","vamp_shadow_cloak","vamp_immortal_flesh","vamp_immortal_kb",
            "vamp_blood_lord","vamp_blood_lord_as","vamp_crimson_tide","vamp_shadow_master",
            "vamp_ancient_one","vamp_ancient_one_arm","vamp_ancient_one_t","vamp_mist_form",
            "vamp_hybrid_dmg","vamp_hybrid_hp","vamp_hybrid_spd","vamp_assassin","vamp_assassin_arm",
            "vamp_assassin_hp","vamp_bloodshadow","vamp_nosferatu_dmg","vamp_nosferatu_hp",
            "vamp_nosferatu_spd","vamp_bloodpact","vamp_daywalker","vamp_progenitor","vamp_progenitor_hp"
        };

        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("abyssorigins", id);
            removeAttr(player, Attributes.ARMOR,              loc);
            removeAttr(player, Attributes.MAX_HEALTH,         loc);
            removeAttr(player, Attributes.ATTACK_DAMAGE,      loc);
            removeAttr(player, Attributes.MOVEMENT_SPEED,     loc);
            removeAttr(player, Attributes.ATTACK_SPEED,       loc);
            removeAttr(player, Attributes.ARMOR_TOUGHNESS,    loc);
            removeAttr(player, Attributes.KNOCKBACK_RESISTANCE,loc);
        }
    }

    // ---- Helpers -------------------------------------------------------

    private static void addAttr(ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            String id, double amount, AttributeModifier.Operation op) {
        var inst = player.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("abyssorigins", id);
        inst.removeModifier(loc);
        inst.addPermanentModifier(new AttributeModifier(loc, amount, op));
    }

    private static void removeAttr(ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            ResourceLocation loc) {
        var inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(loc);
    }
}
