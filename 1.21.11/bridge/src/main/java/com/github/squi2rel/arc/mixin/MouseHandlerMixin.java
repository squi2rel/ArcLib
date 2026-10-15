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
    private void arclib$onButton(long l, MouseButtonInfo mouseButtonInfo, int i, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().mouseButton(mouseButtonInfo, i)) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrop", at = @At("TAIL"))
    private void arclib$onDrop(long l, List<Path> list, int i, CallbackInfo ci) {
        ArcRuntime.fileDropped(list);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void arclib$onScroll(long l, double d, double e, CallbackInfo ci) {
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().scrolled(d, e)) {
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void arclib$onMove(long l, double d, double e, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (l != minecraft.getWindow().handle()) return;
        double scaledX = MouseHandler.getScaledXPos(minecraft.getWindow(), d);
        double scaledY = MouseHandler.getScaledYPos(minecraft.getWindow(), e);
        boolean grabbed = minecraft.mouseHandler.isMouseGrabbed();
        if (ArcRuntime.initialized() && ArcRuntime.sceneHost().mouseMoved(scaledX, scaledY) && !grabbed) {
            xpos = d;
            ypos = e;
            ignoreFirstMove = false;
            ci.cancel();
        }
    }
}
