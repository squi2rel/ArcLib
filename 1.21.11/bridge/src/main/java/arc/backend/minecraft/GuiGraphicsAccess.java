package arc.backend.minecraft;

import net.minecraft.client.gui.render.state.GuiRenderState;

/**
 * Duck interface implemented by the version-specific GuiGraphics mixin.
 */
public interface GuiGraphicsAccess {
    GuiRenderState arclib$renderState();
}
