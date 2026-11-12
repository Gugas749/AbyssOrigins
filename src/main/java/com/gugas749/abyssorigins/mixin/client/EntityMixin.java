package com.gugas749.abyssorigins.mixin.client;

import com.gugas749.abyssorigins.client.reveal.RevealClient;
import com.gugas749.abyssorigins.hiddenpower.realm.CrossPin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Outline colour for privately revealed entities (origin colour under Occult Vision), and the
 * crucified player's frozen view.
 * Client-side mixin only; guarded to client-level entities because in singleplayer the
 * integrated server shares this class.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    /** A crucified player can't look around: the mouse doesn't turn them (CrossPin holds their view). */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void abyssorigins$crucifiedLook(double yaw, double pitch, CallbackInfo ci) {
        if ((Object) this instanceof Player p && p.level().isClientSide() && CrossPin.crucified(p)) ci.cancel();
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void abyssorigins$revealColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide()) return;
        int rgb = RevealClient.colorOf(self);
        if (rgb != -1) cir.setReturnValue(rgb);
    }
}
