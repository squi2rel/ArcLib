package com.github.squi2rel.arc.mixin;

import arc.Core;
import com.github.squi2rel.arc.runtime.HeadlessApplication;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void arclib$start(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (server.isDedicatedServer() && Core.app instanceof HeadlessApplication app) {
            app.start(() -> server.halt(false));
        }
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void arclib$endTick(CallbackInfo ci) {
        if (Core.app instanceof HeadlessApplication app) app.tick();
    }

    @Inject(method = "halt", at = @At("HEAD"))
    private void arclib$requestExit(boolean bl, CallbackInfo ci) {
        if (Core.app instanceof HeadlessApplication app) app.requestExit();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void arclib$close(CallbackInfo ci) {
        if (Core.app instanceof HeadlessApplication app) app.requestExit();
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void arclib$stop(CallbackInfo ci) {
        if (Core.app instanceof HeadlessApplication app) app.dispose();
    }
}
