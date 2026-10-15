package arc.backend.minecraft;

import arc.func.Cons;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ArcComponent extends AbstractWidget{
    private final Table root;

    public ArcComponent(int x, int y, int width, int height, Cons<Scene> builder){
        super(x, y, width, height, Component.empty());
        ArcRuntime.init(net.minecraft.client.Minecraft.getInstance());
        root = ArcRuntime.createContainer();
        ArcRuntime.buildInContainer(root, builder);
    }

    public ArcComponent(int x, int y, int width, int height, Element element){
        this(x, y, width, height, scene -> scene.add(element));
    }

    public ArcComponent(int x, int y, int width, int height, Table root){
        super(x, y, width, height, Component.empty());
        this.root = root;
    }

    public ArcComponent(int x, int y, int width, int height, Scene scene){
        super(x, y, width, height, Component.empty());
        this.root = ArcRuntime.createContainer(scene);
    }

    public Scene scene(){
        return ArcRuntime.scene();
    }

    public Table root(){
        return root;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta){
        ArcRuntime.layoutElement(root, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick){
        return ArcRuntime.mouseButton(ArcRuntime.scene(), event.x(), event.y(), screenWidth(), screenHeight(), event.buttonInfo(), 1);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event){
        return ArcRuntime.mouseButton(ArcRuntime.scene(), event.x(), event.y(), screenWidth(), screenHeight(), event.buttonInfo(), 0);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY){
        return ArcRuntime.mouseDragged(ArcRuntime.scene(), event.x(), event.y(), screenWidth(), screenHeight());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY){
        ArcRuntime.mouseMoved(ArcRuntime.scene(), mouseX, mouseY, screenWidth(), screenHeight());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical){
        return ArcRuntime.scrolled(ArcRuntime.scene(), mouseX, mouseY, screenWidth(), screenHeight(), horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event){
        return ArcRuntime.keyPressed(ArcRuntime.scene(), event, screenWidth(), screenHeight());
    }

    @Override
    public boolean keyReleased(KeyEvent event){
        return ArcRuntime.keyReleased(ArcRuntime.scene(), event, screenWidth(), screenHeight());
    }

    @Override
    public boolean charTyped(CharacterEvent event){
        return ArcRuntime.charTyped(ArcRuntime.scene(), event, screenWidth(), screenHeight());
    }

    private int screenWidth(){
        return ArcRuntime.minecraft().getWindow().getGuiScaledWidth();
    }

    private int screenHeight(){
        return ArcRuntime.minecraft().getWindow().getGuiScaledHeight();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput){
    }
}
