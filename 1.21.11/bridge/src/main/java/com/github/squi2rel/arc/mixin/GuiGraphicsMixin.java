package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import arc.backend.minecraft.GuiGraphicsAccess;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin implements GuiGraphicsAccess {
    @Shadow
    @Final
    GuiRenderState guiRenderState;

    @Inject(method = "applyCursor", at = @At("HEAD"))
    private void arclib$requestCursor(Window window, CallbackInfo ci) {
        if (ArcRuntime.initialized()) ArcRuntime.graphics().requestCursor((GuiGraphics) (Object) this);
    }

    @Override
    public GuiRenderState arclib$renderState() {
        return guiRenderState;
    }
}
