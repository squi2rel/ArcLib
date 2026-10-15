package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import com.github.squi2rel.arc.ArcLibClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void arclib$initAfterWindow(CallbackInfo ci) {
        ArcLibClient.afterWindowCreated();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void arclib$endTick(CallbackInfo ci) {
        if (!ArcRuntime.initialized()) return;
        ArcRuntime.endTick();
    }

    @Inject(method = "resizeGui", at = @At("TAIL"))
    private void arclib$resize(CallbackInfo ci) {
        ArcRuntime.guiScaleChanged();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void arclib$requestExit(CallbackInfo ci) {
        ArcRuntime.requestExit();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void arclib$close(CallbackInfo ci) {
        if (ArcRuntime.initialized()) ArcRuntime.dispose();
    }
}
