package com.gugas749.abyssorigins.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Persistent per-player origin data stored as a NeoForge attachment.
 *
 * Fields:
 *  originId      — ResourceLocation of the assigned origin (null = none)
 *  resource      — Current resource value (0-100), e.g. blood for vampires
 *  cooldown1     — Ticks remaining on ability 1 cooldown
 *  cooldown2     — Ticks remaining on ability 2 cooldown
 *  originLevel   — XP level within the origin (1-50)
 *  originXp      — Raw XP points
 */
public class PlayerOriginData {

    public static final Codec<PlayerOriginData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.optionalFieldOf("origin_id")
                .forGetter(d -> Optional.ofNullable(d.originId)),
            Codec.FLOAT.optionalFieldOf("resource", 100f)
                .forGetter(d -> d.resource),
            Codec.INT.optionalFieldOf("cooldown1", 0)
                .forGetter(d -> d.cooldown1),
            Codec.INT.optionalFieldOf("cooldown2", 0)
                .forGetter(d -> d.cooldown2),
            Codec.INT.optionalFieldOf("origin_level", 1)
                .forGetter(d -> d.originLevel),
            Codec.INT.optionalFieldOf("origin_xp", 0)
                .forGetter(d -> d.originXp)
        ).apply(instance, (id, res, cd1, cd2, lvl, xp) -> {
            PlayerOriginData data = new PlayerOriginData();
            data.originId    = id.orElse(null);
            data.resource    = res;
            data.cooldown1   = cd1;
            data.cooldown2   = cd2;
            data.originLevel = lvl;
            data.originXp    = xp;
            return data;
        })
    );

    private ResourceLocation originId   = null;
    private float resource              = 100f;
    private int   cooldown1             = 0;
    private int   cooldown2             = 0;
    private int   originLevel           = 1;
    private int   originXp              = 0;

    public PlayerOriginData() {}

    // ---- Accessors / mutators -------------------------------------------

    public ResourceLocation getOriginId()          { return originId; }
    public void setOriginId(ResourceLocation id)   { this.originId = id; }

    public float getResource()                     { return resource; }
    public void setResource(float v)               { this.resource = Math.max(0, Math.min(100, v)); }
    public void addResource(float v)               { setResource(resource + v); }
    public void drainResource(float v)             { setResource(resource - v); }

    public int getCooldown1()                      { return cooldown1; }
    public void setCooldown1(int ticks)            { this.cooldown1 = Math.max(0, ticks); }
    public void tickCooldown1()                    { if (cooldown1 > 0) cooldown1--; }

    public int getCooldown2()                      { return cooldown2; }
    public void setCooldown2(int ticks)            { this.cooldown2 = Math.max(0, ticks); }
    public void tickCooldown2()                    { if (cooldown2 > 0) cooldown2--; }

    public int getOriginLevel()                    { return originLevel; }
    public void setOriginLevel(int lvl)            { this.originLevel = Math.max(1, Math.min(50, lvl)); }

    public int getOriginXp()                       { return originXp; }
    public void setOriginXp(int xp)               { this.originXp = Math.max(0, xp); }

    /** Copy all fields from another instance (used in PlayerEvent.Clone). */
    public void copyFrom(PlayerOriginData other) {
        this.originId    = other.originId;
        this.resource    = other.resource;
        this.cooldown1   = other.cooldown1;
        this.cooldown2   = other.cooldown2;
        this.originLevel = other.originLevel;
        this.originXp    = other.originXp;
    }
}
