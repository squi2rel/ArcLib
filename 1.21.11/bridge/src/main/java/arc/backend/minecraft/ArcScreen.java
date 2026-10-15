package arc.backend.minecraft;

import arc.func.Cons;
import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick){
        return ArcRuntime.mouseButton(ArcRuntime.scene(), event.x(), event.y(), width, height, event.buttonInfo(), 1) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event){
        return ArcRuntime.mouseButton(ArcRuntime.scene(), event.x(), event.y(), width, height, event.buttonInfo(), 0) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY){
        return ArcRuntime.mouseDragged(ArcRuntime.scene(), event.x(), event.y(), width, height) || super.mouseDragged(event, deltaX, deltaY);
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
    public boolean keyPressed(KeyEvent event){
        return ArcRuntime.keyPressed(ArcRuntime.scene(), event, width, height) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event){
        return ArcRuntime.keyReleased(ArcRuntime.scene(), event, width, height) || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event){
        return ArcRuntime.charTyped(ArcRuntime.scene(), event, width, height) || super.charTyped(event);
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
