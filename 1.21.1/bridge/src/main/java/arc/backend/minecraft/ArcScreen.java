package arc.backend.minecraft;

import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A placeholder screen whose table is rendered and handled by the shared Arc scene.
 */
public class ArcScreen extends Screen {
    private final Table root;

    public ArcScreen(Table root) {
        super(Component.empty());
        this.root = root;
        root.setFillParent(true);
    }

    public Scene scene() {
        return ArcRuntime.scene();
    }

    public Table root() {
        return root;
    }

    @Override
    public void added() {
        super.added();
        ArcRuntime.init(Minecraft.getInstance());
        scene().add(root);
    }

    @Override
    public void removed() {
        root.remove();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBackground(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBlurredBackground(tickDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
