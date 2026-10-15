package arc.backend.minecraft;

import net.minecraft.client.renderer.state.gui.GuiRenderState;

/**
 * Duck interface implemented by the version-specific GuiGraphicsExtractor mixin.
 */
public interface GuiGraphicsExtractorAccess {
    GuiRenderState arclib$renderState();
}
