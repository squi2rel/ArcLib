package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    private double xpos;
    @Shadow
    private double ypos;
    @Shadow
    private boolean ignoreFirstMove;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void arclib$onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().mouseButton(rawButtonInfo, action)) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrop", at = @At("TAIL"))
    private void arclib$onDrop(long handle, List<Path> files, int failedCount, CallbackInfo ci) {
        ArcRuntime.fileDropped(files);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void arclib$onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().scrolled(xoffset, yoffset)) {
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void arclib$onMove(long handle, double xpos, double ypos, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (handle != minecraft.getWindow().handle()) return;
        double scaledX = MouseHandler.getScaledXPos(minecraft.getWindow(), xpos);
        double scaledY = MouseHandler.getScaledYPos(minecraft.getWindow(), ypos);
        boolean grabbed = minecraft.mouseHandler.isMouseGrabbed();
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().mouseMoved(scaledX, scaledY) && !grabbed) {
            this.xpos = xpos;
            this.ypos = ypos;
            ignoreFirstMove = false;
            ci.cancel();
        }
    }
}
