package com.gugas749.abyssorigins.skilltree.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Abyssorigins.MODID)
public class SkillPassiveTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return; // once per second

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        if (origin == null) return;

        PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());

        switch (origin.getId().getPath()) {
            case "human"   -> tickHumanSkills(player, skills);
            case "vampire" -> tickVampireSkills(player, skills);
        }
    }

    // ---- Human --------------------------------------------------------

    private static void tickHumanSkills(Player player, PlayerSkillData skills) {
        // Last Stand: Resistance II below 30% HP
        if (skills.hasSkill("human_last_stand")) {
            if (player.getHealth() / player.getMaxHealth() < 0.30f) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false));
            }
        }

        // Quick Learner: extra regen when well-fed
        if (skills.hasSkill("human_quick_learner")) {
            if (player.getFoodData().getFoodLevel() >= 18) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, false));
            }
        }
    }

    // ---- Vampire -------------------------------------------------------

    private static void tickVampireSkills(Player player, PlayerSkillData skills) {
        PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        // vamp_hunger (T1): reduce drain rate by 20%
        // vamp_rapid_regen (T3): additional 40% reduction
        float drainRefund = 0f;
        if (skills.hasSkill("vamp_hunger"))      drainRefund += 0.075f; // ~20% of base drain
        if (skills.hasSkill("vamp_rapid_regen")) drainRefund += 0.15f;  // additional 40%
        if (drainRefund > 0f) data.addResource(drainRefund);

        // vamp_night_hunter (T3): +15% to stats at night / underground
        boolean isDark = !player.level().isDay()
            || player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
        if (skills.hasSkill("vamp_night_hunter") && isDark) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,     30, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,   30, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,30, 0, false, false));
        }

        // vamp_shadow_cloak (T3): invisibility in low light
        if (skills.hasSkill("vamp_shadow_cloak")) {
            int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
            if (light < 8 && !player.hasEffect(MobEffects.INVISIBILITY)) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            }
        }

        // vamp_blood_frenzy (T3): bonus damage as HP drops (handled via effect)
        if (skills.hasSkill("vamp_blood_frenzy")) {
            float hpPct = player.getHealth() / player.getMaxHealth();
            if (hpPct < 0.5f) {
                int amplifier = hpPct < 0.25f ? 1 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, amplifier, false, false));
            }
        }

        // vamp_nosferatu (T5): all cooldowns reduced — handled as a flag in ActivateAbilityPacket
        // vamp_forbidden_bloodpact (T5): extra sun vulnerability tracked in VampireSunPassive via skill check
    }
}
