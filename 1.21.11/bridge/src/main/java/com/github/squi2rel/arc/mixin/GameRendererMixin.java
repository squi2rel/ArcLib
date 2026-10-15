package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    GuiRenderState guiRenderState;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void arclib$renderArcScene(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        if (!ArcRuntime.initialized() || !minecraft.isGameLoadFinished()) return;

        GuiGraphics graphics = new GuiGraphics(minecraft, guiRenderState, arclib$mouseX(), arclib$mouseY());
        graphics.nextStratum();
        ArcRuntime.sceneHost().draw(graphics, deltaTracker);
    }

    @Unique
    private int arclib$mouseX() {
        return (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
    }

    @Unique
    private int arclib$mouseY() {
        return (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
    }
}
