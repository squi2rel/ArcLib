package arc.backend.minecraft;

import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

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
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        extractBlurredBackground(graphics);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
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
    public boolean keyPressed(@NonNull KeyEvent event) {
        return false;
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        return false;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return false;
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
