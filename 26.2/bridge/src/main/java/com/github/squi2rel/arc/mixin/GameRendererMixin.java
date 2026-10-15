package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin{
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Inject(method = "extract", at = @At("TAIL"))
    private void arclib$renderArcScene(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci){
        if(!ArcRuntime.initialized() || !minecraft.isGameLoadFinished()) return;

        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, gameRenderState.guiRenderState, mouseX(), mouseY());
        graphics.nextStratum();
        ArcRuntime.sceneHost().draw(graphics, deltaTracker);
    }

    private int mouseX(){
        return (int)minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
    }

    private int mouseY(){
        return (int)minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
    }
}
