package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin{
    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private boolean ignoreFirstMove;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void arclib$onPress(long window, int button, int action, int modifiers, CallbackInfo ci){
        if(ArcRuntime.initialized() && ArcRuntime.sceneHost().mouseButton(button, action)){
            ci.cancel();
        }
    }

    @Inject(method = "onDrop", at = @At("TAIL"))
    private void arclib$onDrop(long window, java.util.List<java.nio.file.Path> paths, int failures, CallbackInfo ci){
        for(java.nio.file.Path path : paths) ArcRuntime.fileDropped(path);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void arclib$onScroll(long window, double horizontal, double vertical, CallbackInfo ci){
        if(ArcRuntime.initialized() && ArcRuntime.sceneHost().scrolled(horizontal, vertical)){
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void arclib$onMove(long window, double x, double y, CallbackInfo ci){
        Minecraft minecraft = Minecraft.getInstance();
        if(window != minecraft.getWindow().getWindow()) return;
        double scaledX = x * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double scaledY = y * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        boolean grabbed = minecraft.mouseHandler.isMouseGrabbed();
        // Arc receives a stationary center position; Minecraft still needs raw movement for the camera.
        if(ArcRuntime.initialized() && ArcRuntime.sceneHost().mouseMoved(scaledX, scaledY) && !grabbed){
            // Cancelling onMove skips Minecraft's cursor update, which button events also use.
            xpos = x;
            ypos = y;
            ignoreFirstMove = false;
            ci.cancel();
        }
    }
}
