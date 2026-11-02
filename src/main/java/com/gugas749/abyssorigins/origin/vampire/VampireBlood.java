package com.gugas749.abyssorigins.origin.vampire;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Which creatures carry drinkable blood. Shared by Blood Gaze, Blood Drain and Predator Senses. */
public final class VampireBlood {

    private static final Set<String> BLOODLESS = Set.of(
        "iron_golem","snow_golem","shulker","armor_stand","blaze","magma_cube","ghast","slime",
        "enderman","endermite","ender_dragon","warden","allay","vex","breeze","creaking"
    );
    private static final Set<String> TOO_SMALL = Set.of(
        "bat","bee","rabbit","chicken","parrot","frog","tadpole",
        "cod","salmon","tropical_fish","pufferfish","silverfish","cave_spider"
    );

    private VampireBlood() {}

    public static boolean hasBlood(LivingEntity e) {
        if (!e.isAlive()) return false;
        if (e.getType().is(EntityTypeTags.UNDEAD)) return false;
        String id = EntityType.getKey(e.getType()).getPath();
        if (BLOODLESS.contains(id) || TOO_SMALL.contains(id)) return false;
        if (e instanceof Player || e instanceof Animal || e instanceof Squid || e instanceof AbstractVillager) return true;
        if (e instanceof Monster) return true;
        return e.getBbHeight() >= 0.5f && e.getBbWidth() >= 0.3f;
    }
}
