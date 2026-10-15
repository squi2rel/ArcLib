package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void arclib$keyPress(long l, int i, int j, int k, int m, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().keyPressed(i, j, k)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void arclib$charTyped(long l, int i, int j, CallbackInfo ci) {
        if (ArcRuntime.initialized() && Character.isBmpCodePoint(i) && ArcRuntime.sceneHost().charTyped((char) i)) {
            ci.cancel();
        }
    }

}
