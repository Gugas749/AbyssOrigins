package com.gugas749.abyssorigins.mixin;

import com.gugas749.abyssorigins.effect.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shade's Phase Walk: {@code Player.tick} resets {@code noPhysics} to "is spectator" every tick; a Phasing
 * player keeps it on, so they move through blocks (client-side for the local player's own movement, and on
 * the server so movement validation, suffocation and the in-wall overlay all agree).
 */
@Mixin(Player.class)
public abstract class PlayerPhaseMixin {

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
        opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, ordinal = 0))
    private void abyssorigins$phase(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.hasEffect(ModEffects.PHASING)) return;
        self.noPhysics = true;
        // As for spectators: never "landed" (landing would end flight and drop the body through the floor),
        // and the player keeps flying even if they double-tap jump.
        self.setOnGround(false);
        if (self.getAbilities().mayfly) self.getAbilities().flying = true;
    }
}
