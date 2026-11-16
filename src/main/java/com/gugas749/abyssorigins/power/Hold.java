package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.Abyssorigins;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Holds a player in place (in mid-air too): gravity, jump strength and movement speed go to 0 through
 * transient attribute modifiers. Transient modifiers are never saved, so a crash or logout can't leave a
 * player floating. Callers also zero the velocity each tick and snap the player back to an anchor.
 */
public final class Hold {

    private Hold() {}

    /** @param name modifier id path, one per kind of hold so they don't release each other. */
    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, name);
    }

    public static void set(ServerPlayer p, ResourceLocation id, boolean on) {
        set(p, Attributes.GRAVITY, id, on);
        set(p, Attributes.JUMP_STRENGTH, id, on);
        set(p, Attributes.MOVEMENT_SPEED, id, on);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, boolean on) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (on) inst.addTransientModifier(new AttributeModifier(id, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
