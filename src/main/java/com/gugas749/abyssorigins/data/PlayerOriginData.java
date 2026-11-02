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
 *  originLevel   — XP level within the origin (1-50)
 *  originXp      — Raw XP points
 *
 * Ability cooldowns live in PlayerAbilityData (per ability, shared by origin and class).
 */
public class PlayerOriginData {

    public static final Codec<PlayerOriginData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.optionalFieldOf("origin_id")
                .forGetter(d -> Optional.ofNullable(d.originId)),
            Codec.FLOAT.optionalFieldOf("resource", 100f)
                .forGetter(d -> d.resource),
            Codec.INT.optionalFieldOf("origin_level", 1)
                .forGetter(d -> d.originLevel),
            Codec.INT.optionalFieldOf("origin_xp", 0)
                .forGetter(d -> d.originXp)
        ).apply(instance, (id, res, lvl, xp) -> {
            PlayerOriginData data = new PlayerOriginData();
            data.originId    = id.orElse(null);
            data.resource    = res;
            data.originLevel = lvl;
            data.originXp    = xp;
            return data;
        })
    );

    private ResourceLocation originId   = null;
    private float resource              = 100f;
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

    public int getOriginLevel()                    { return originLevel; }
    public void setOriginLevel(int lvl)            { this.originLevel = Math.max(1, Math.min(50, lvl)); }

    public int getOriginXp()                       { return originXp; }
    public void setOriginXp(int xp)               { this.originXp = Math.max(0, xp); }

    /** Copy all fields from another instance (used in PlayerEvent.Clone). */
    public void copyFrom(PlayerOriginData other) {
        this.originId    = other.originId;
        this.resource    = other.resource;
        this.originLevel = other.originLevel;
        this.originXp    = other.originXp;
    }
}
