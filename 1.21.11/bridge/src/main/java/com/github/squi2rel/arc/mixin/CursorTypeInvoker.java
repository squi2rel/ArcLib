package com.github.squi2rel.arc.mixin;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CursorType.class)
public interface CursorTypeInvoker {
    @Invoker("<init>")
    static CursorType arclib$create(String name, long handle) {
        throw new AssertionError();
    }
}
