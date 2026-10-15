package arc.backend.minecraft;

import arc.scene.Scene;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ArcSceneHost{
    private final Scene initialScene;

    public ArcSceneHost(Scene scene){
        this.initialScene = scene;
    }

    public Scene scene(){
        Scene scene = ArcRuntime.scene();
        return scene == null ? initialScene : scene;
    }

    public void draw(GuiGraphics graphics, DeltaTracker tickCounter){
        float delta = tickCounter == null ? 0f : tickCounter.getRealtimeDeltaTicks() / 20f;
        ArcRuntime.renderFrame(scene(), graphics, graphics.guiWidth(), graphics.guiHeight(), delta);
    }

    public boolean mouseMoved(double x, double y){
        return ArcRuntime.input().isTouched() ?
        ArcRuntime.mouseDragged(scene(), x, y, width(), height()) :
        ArcRuntime.mouseMoved(scene(), x, y, width(), height());
    }

    public boolean mouseButton(int button, int action){
        Minecraft mc = ArcRuntime.minecraft();
        double x = scaledX(mc);
        double y = scaledY(mc);
        return ArcRuntime.mouseButton(scene(), x, y, width(), height(), button, action);
    }

    public boolean scrolled(double horizontal, double vertical){
        Minecraft mc = ArcRuntime.minecraft();
        double x = scaledX(mc);
        double y = scaledY(mc);
        return ArcRuntime.scrolled(scene(), x, y, width(), height(), horizontal, vertical);
    }

    public boolean keyPressed(int key, int scanCode, int action, int modifiers){
        return switch(action){
            case 1, 2 -> ArcRuntime.keyPressed(scene(), key, scanCode, modifiers, width(), height());
            case 0 -> ArcRuntime.keyReleased(scene(), key, scanCode, modifiers, width(), height());
            default -> false;
        };
    }

    public boolean charTyped(char character, int modifiers){
        return ArcRuntime.charTyped(scene(), character, modifiers, width(), height());
    }

    private int width(){
        return ArcRuntime.minecraft().getWindow().getGuiScaledWidth();
    }

    private int height(){
        return ArcRuntime.minecraft().getWindow().getGuiScaledHeight();
    }

    private static double scaledX(Minecraft minecraft){
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
    }

    private static double scaledY(Minecraft minecraft){
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
    }
}
