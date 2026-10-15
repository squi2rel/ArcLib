package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void arclib$renderArcScene(float f, long l, boolean bl, CallbackInfo ci) {
        if (!ArcRuntime.initialized() || minecraft.getOverlay() != null) return;
        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        ArcRuntime.sceneHost().draw(graphics, minecraft.getDeltaFrameTime() / 20f);
        graphics.flush();
    }
}
