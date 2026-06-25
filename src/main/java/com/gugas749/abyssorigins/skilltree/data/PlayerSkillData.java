package com.gugas749.abyssorigins.skilltree.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class PlayerSkillData {

    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.listOf().optionalFieldOf("unlocked_skills", new ArrayList<>())
                .forGetter(d -> new ArrayList<>(d.unlockedSkills)),
            Codec.INT.optionalFieldOf("skill_points", 0)
                .forGetter(d -> d.skillPoints)
        ).apply(instance, (skills, points) -> {
            PlayerSkillData data = new PlayerSkillData();
            data.unlockedSkills.addAll(skills);
            data.skillPoints = points;
            return data;
        })
    );

    private final List<String> unlockedSkills = new ArrayList<>();
    private int skillPoints = 0;

    public PlayerSkillData() {}

    // ---- Core methods ---------------------------------------------------

    public boolean hasSkill(String skillId)    { return unlockedSkills.contains(skillId); }
    public void    unlockSkill(String skillId) { if (!hasSkill(skillId)) unlockedSkills.add(skillId); }

    public List<String>      getUnlockedSkills()    { return Collections.unmodifiableList(unlockedSkills); }
    /** Returns as Set — used by SkillNode.canUnlock() */
    public Set<String>       getUnlockedSkillsSet() { return new HashSet<>(unlockedSkills); }

    public int  getSkillPoints()               { return skillPoints; }
    public void addSkillPoints(int n)          { this.skillPoints = Math.max(0, skillPoints + n); }
    public void spendSkillPoints(int n)        { this.skillPoints = Math.max(0, skillPoints - n); }

    public void copyFrom(PlayerSkillData other) {
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(other.unlockedSkills);
        this.skillPoints = other.skillPoints;
    }
}
