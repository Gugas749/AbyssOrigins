package com.gugas749.abyssorigins.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

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
            Codec.INT.optionalFieldOf("spec_ability_cooldown", 0)
                .forGetter(d -> d.abilityCooldown)
        ).apply(instance, (id, lvl, pts, skills, cd) -> {
            PlayerSpecializationData data = new PlayerSpecializationData();
            data.specId        = id.orElse(null);
            data.specLevel     = lvl;
            data.skillPoints   = pts;
            data.unlockedSkills.addAll(skills);
            data.abilityCooldown = cd;
            return data;
        })
    );

    private ResourceLocation specId      = null;
    private int              specLevel   = 0;
    private int              skillPoints = 0;
    private final List<String> unlockedSkills = new ArrayList<>();
    private int              abilityCooldown  = 0;

    public PlayerSpecializationData() {}

    // ---- Spec identity ---------------------------------------------------

    public ResourceLocation getSpecId()            { return specId; }
    public void setSpecId(ResourceLocation id)     { this.specId = id; }
    public boolean hasSpec()                       { return specId != null; }

    // ---- Level -----------------------------------------------------------

    public int  getSpecLevel()                     { return specLevel; }
    public void setSpecLevel(int lvl)              { this.specLevel = Math.max(0, lvl); }
    public void addSpecLevel(int n)                { this.specLevel = Math.max(0, specLevel + n); }

    // ---- Skill points (granted via command / FTB Quests) ----------------

    public int  getSkillPoints()                   { return skillPoints; }
    public void addSkillPoints(int n)              { this.skillPoints = Math.max(0, skillPoints + n); }
    public void spendSkillPoints(int n)            { this.skillPoints = Math.max(0, skillPoints - n); }

    // ---- Unlocked skills -------------------------------------------------

    public boolean     hasSkill(String id)         { return unlockedSkills.contains(id); }
    public void        unlockSkill(String id)      { if (!hasSkill(id)) unlockedSkills.add(id); }
    public List<String> getUnlockedSkills()        { return Collections.unmodifiableList(unlockedSkills); }
    public Set<String>  getUnlockedSkillsSet()     { return new HashSet<>(unlockedSkills); }

    // ---- Ability cooldown ------------------------------------------------

    public int  getAbilityCooldown()               { return abilityCooldown; }
    public void setAbilityCooldown(int ticks)      { this.abilityCooldown = Math.max(0, ticks); }
    public void tickAbilityCooldown()              { if (abilityCooldown > 0) abilityCooldown--; }

    // ---- Copy (used on death/clone) --------------------------------------

    public void copyFrom(PlayerSpecializationData other) {
        this.specId        = other.specId;
        this.specLevel     = other.specLevel;
        this.skillPoints   = other.skillPoints;
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(other.unlockedSkills);
        this.abilityCooldown = other.abilityCooldown;
    }
}
