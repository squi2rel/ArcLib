package com.github.squi2rel.arc.mixin;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontManager.class)
public class FontManagerMixin{
    @Inject(method = "apply", at = @At("TAIL"))
    private void arclib$refreshFontMetrics(CallbackInfo ci){
        ArcRuntime.fontsChanged();
    }
}
