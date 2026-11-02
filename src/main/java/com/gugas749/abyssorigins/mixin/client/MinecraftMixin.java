package com.gugas749.abyssorigins.mixin.client;

import com.gugas749.abyssorigins.client.reveal.RevealClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets RevealClient make an entity render its outline on THIS client only.
 * Vanilla only consults the shared Glowing flag here; we add our private reveal set.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void abyssorigins$privateReveal(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RevealClient.shouldReveal(entity)) cir.setReturnValue(true);
    }
}
