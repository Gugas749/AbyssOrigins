package com.gugas749.abyssorigins.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Persistent per-player class (specialization) data.
 *
 * Class level is raised either by class XP (earned alongside origin XP) or
 * directly by OP/quest commands (/abyssorigins specialization addlevel).
 * Class ability cooldowns live in PlayerAbilityData with the origin's.
 */
public class PlayerSpecializationData {

    public static final Codec<PlayerSpecializationData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.optionalFieldOf("spec_id")
                .forGetter(d -> Optional.ofNullable(d.specId)),
            Codec.INT.optionalFieldOf("spec_level", 0)
                .forGetter(d -> d.specLevel),
            Codec.INT.optionalFieldOf("spec_skill_points", 0)
                .forGetter(d -> d.skillPoints),
            Codec.STRING.listOf().optionalFieldOf("spec_unlocked_skills", new ArrayList<>())
                .forGetter(d -> new ArrayList<>(d.unlockedSkills)),
            Codec.INT.optionalFieldOf("spec_xp", 0)
                .forGetter(d -> d.specXp)
        ).apply(instance, (id, lvl, pts, skills, xp) -> {
            PlayerSpecializationData data = new PlayerSpecializationData();
            data.specId          = id.orElse(null);
            data.specLevel       = lvl;
            data.skillPoints     = pts;
            data.unlockedSkills.addAll(skills);
            data.specXp          = xp;
            return data;
        })
    );

    private ResourceLocation specId      = null;
    private int              specLevel   = 0;
    private int              specXp      = 0;
    private int              skillPoints = 0;
    private final List<String> unlockedSkills = new ArrayList<>();

    public PlayerSpecializationData() {}

    // ---- Spec identity ---------------------------------------------------

    public ResourceLocation getSpecId()            { return specId; }
    public void setSpecId(ResourceLocation id)     { this.specId = id; }
    public boolean hasSpec()                       { return specId != null; }

    // ---- Level / XP ------------------------------------------------------

    public int  getSpecLevel()                     { return specLevel; }
    public void setSpecLevel(int lvl)              { this.specLevel = Math.max(0, Math.min(50, lvl)); }
    public void addSpecLevel(int n)                { setSpecLevel(specLevel + n); }

    public int  getSpecXp()                        { return specXp; }
    public void setSpecXp(int xp)                  { this.specXp = Math.max(0, xp); }

    // ---- Skill points (granted via levels / command / FTB Quests) -------

    public int  getSkillPoints()                   { return skillPoints; }
    public void addSkillPoints(int n)              { this.skillPoints = Math.max(0, skillPoints + n); }
    public void spendSkillPoints(int n)            { this.skillPoints = Math.max(0, skillPoints - n); }
    public void setSkillPoints(int n)              { this.skillPoints = Math.max(0, n); }

    // ---- Unlocked skills -------------------------------------------------

    public boolean     hasSkill(String id)         { return unlockedSkills.contains(id); }
    public void        unlockSkill(String id)      { if (!hasSkill(id)) unlockedSkills.add(id); }
    public void        clearSkills()               { unlockedSkills.clear(); }
    public List<String> getUnlockedSkills()        { return Collections.unmodifiableList(unlockedSkills); }
    public Set<String>  getUnlockedSkillsSet()     { return new HashSet<>(unlockedSkills); }

    // ---- Copy (used on death/clone) --------------------------------------

    public void copyFrom(PlayerSpecializationData other) {
        this.specId          = other.specId;
        this.specLevel       = other.specLevel;
        this.specXp          = other.specXp;
        this.skillPoints     = other.skillPoints;
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(other.unlockedSkills);
    }
}
