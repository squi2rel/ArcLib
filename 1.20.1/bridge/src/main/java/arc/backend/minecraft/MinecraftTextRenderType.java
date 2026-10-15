package arc.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Preserves the font renderer's shader while drawing text in Arc's painter order.
 */
final class MinecraftTextRenderType extends RenderType {
    private MinecraftTextRenderType(RenderType delegate) {
        super("arclib_text", delegate.format(), delegate.mode(), delegate.bufferSize(), false, false,
                () -> {
                    delegate.setupRenderState();
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                },
                () -> {
                    delegate.clearRenderState();
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                });
    }

    static MultiBufferSource painterOrder(MultiBufferSource source) {
        // Reuse each type within this text draw. Do not retain font atlas types across reloads.
        Map<RenderType, RenderType> types = new IdentityHashMap<>();
        return type -> source.getBuffer(types.computeIfAbsent(type, MinecraftTextRenderType::new));
    }
}
