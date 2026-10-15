package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(FontManager.class)
public class FontManagerMixin {
    @Inject(method = "reload", at = @At("RETURN"), cancellable = true)
    private void arclib$afterFontReload(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        ci.setReturnValue(ci.getReturnValue().thenRunAsync(ArcRuntime::fontsChanged, Minecraft.getInstance()));
    }

    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void arclib$refreshFontMetrics(CallbackInfo ci) {
        ArcRuntime.fontsChanged();
    }
}
