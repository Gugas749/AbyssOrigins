package com.gugas749.abyssorigins.api;

import net.minecraft.network.chat.Component;

/**
 * Origin stages ("sub-origins"). Every origin has three, reached by origin level:
 *
 *  - stage 0, the origin itself (level 1), e.g. Vampire;
 *  - stage 1, Noble (level {@link #NOBLE_LEVEL}), e.g. Noble Vampire;
 *  - stage 2, Lord (level {@link #LORD_LEVEL}), e.g. Lord Vampire.
 *
 * The stage is derived from the level, so it never needs saving. Each stage opens a new row of the origin's
 * skill tree (SkillTier TIER_7 / TIER_8), adds the stats the origin lists for it ({@link Origin#addStageStat})
 * and gives bonus skill points. Reaching a stage plays the ascension ceremony (AscensionCeremony on the
 * server, AscensionCinematic on AbyssCore's cinematic engine) in the origin's own {@link Motif}.
 *
 * Stage names and lines are lang keys: {@code abyssorigins.origin.<path>.stage.<n>} and {@code ...stage.<n>.line}.
 */
public final class Ascension {

    public static final int BASE = 0, NOBLE = 1, LORD = 2;
    public static final int NOBLE_LEVEL = 25, LORD_LEVEL = 50;

    private static final int[] BONUS_POINTS = { 0, 3, 5 };

    /** How the ceremony looks and sounds: each origin's camera path, effects and sounds. */
    public enum Motif { RADIANT, BLOOD, HELLFIRE, DIVINE, VOID, SHADOW, SOVEREIGN }

    /**
     * Ceremony timeline in ticks: the player rises until {@code rise}, the energy gathers until {@code burst}
     * (the explosion), the player is released at {@code release} and the cinematic ends at {@code total}.
     */
    public record Timeline(int rise, int burst, int release, int total) {}

    private static final Timeline NOBLE_TIMELINE = new Timeline(30, 100, 140, 165);
    private static final Timeline LORD_TIMELINE  = new Timeline(36, 130, 175, 205);

    private Ascension() {}

    public static int stageAt(int level) {
        return level >= LORD_LEVEL ? LORD : level >= NOBLE_LEVEL ? NOBLE : BASE;
    }

    public static int levelFor(int stage) {
        return stage >= LORD ? LORD_LEVEL : stage == NOBLE ? NOBLE_LEVEL : 1;
    }

    /** Skill points given on reaching {@code stage}. */
    public static int bonusPoints(int stage) {
        return BONUS_POINTS[Math.max(0, Math.min(LORD, stage))];
    }

    public static Timeline timeline(int stage) {
        return stage >= LORD ? LORD_TIMELINE : NOBLE_TIMELINE;
    }

    /** The origin's name at {@code stage}: "Vampire", "Noble Vampire", "Lord Vampire". */
    public static Component title(Origin origin, int stage) {
        return stage <= BASE ? origin.getDisplayName()
            : Component.translatable("abyssorigins.origin." + origin.getPath() + ".stage." + Math.min(LORD, stage));
    }

    /** The origin's name at {@code level}. */
    public static Component titleAtLevel(Origin origin, int level) {
        return title(origin, stageAt(level));
    }

    /** The line shown while the ceremony for {@code stage} gathers. */
    public static Component line(Origin origin, int stage) {
        return Component.translatable(lineKey(origin, stage));
    }

    /** The lang key of {@link #line}. */
    public static String lineKey(Origin origin, int stage) {
        return "abyssorigins.origin." + origin.getPath() + ".stage." + Math.min(LORD, Math.max(NOBLE, stage)) + ".line";
    }
}
