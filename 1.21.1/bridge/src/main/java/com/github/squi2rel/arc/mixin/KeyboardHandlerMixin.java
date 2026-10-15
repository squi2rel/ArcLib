package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin{
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void arclib$keyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci){
        if(ArcRuntime.initialized() && ArcRuntime.sceneHost().keyPressed(key, scanCode, action, modifiers)){
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void arclib$charTyped(long window, int codePoint, int modifiers, CallbackInfo ci){
        if(ArcRuntime.initialized() && Character.isBmpCodePoint(codePoint) && ArcRuntime.sceneHost().charTyped((char)codePoint, modifiers)){
            ci.cancel();
        }
    }

}
