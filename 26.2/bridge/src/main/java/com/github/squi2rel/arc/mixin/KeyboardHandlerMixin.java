package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void arclib$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().keyPressed(action, event)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void arclib$charTyped(long handle, CharacterEvent event, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().charTyped(event)) {
            ci.cancel();
        }
    }

}
