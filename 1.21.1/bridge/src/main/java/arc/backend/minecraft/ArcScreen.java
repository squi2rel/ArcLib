package arc.backend.minecraft;

import arc.func.Cons;
import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ArcScreen extends Screen{
    private final Table root;

    public ArcScreen(String title, Cons<Scene> builder){
        super(Component.literal(title));
        ArcRuntime.init(net.minecraft.client.Minecraft.getInstance());
        root = ArcRuntime.createContainer();
        root.setFillParent(true);
        ArcRuntime.buildInContainer(root, builder);
    }

    public ArcScreen(Component title, Table root){
        super(title);
        this.root = root;
    }

    public ArcScreen(Component title, Scene scene){
        super(title);
        this.root = ArcRuntime.createContainer(scene);
        this.root.setFillParent(true);
    }

    public Scene scene(){
        return ArcRuntime.scene();
    }

    public Table root(){
        return root;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta){
        renderTransparentBackground(graphics);
        super.render(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        return ArcRuntime.mouseButton(ArcRuntime.scene(), mouseX, mouseY, width, height, button, 1) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button){
        return ArcRuntime.mouseButton(ArcRuntime.scene(), mouseX, mouseY, width, height, button, 0) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY){
        return ArcRuntime.mouseDragged(ArcRuntime.scene(), mouseX, mouseY, width, height) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY){
        ArcRuntime.mouseMoved(ArcRuntime.scene(), mouseX, mouseY, width, height);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical){
        return ArcRuntime.scrolled(ArcRuntime.scene(), mouseX, mouseY, width, height, horizontal, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers){
        return ArcRuntime.keyPressed(ArcRuntime.scene(), key, scanCode, modifiers, width, height) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers){
        return ArcRuntime.keyReleased(ArcRuntime.scene(), key, scanCode, modifiers, width, height) || super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers){
        return ArcRuntime.charTyped(ArcRuntime.scene(), character, modifiers, width, height) || super.charTyped(character, modifiers);
    }

    @Override
    public void removed(){
        root.remove();
        super.removed();
    }

    @Override
    public boolean isPauseScreen(){
        return false;
    }
}
