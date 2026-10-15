package arc.backend.minecraft;

import arc.scene.Scene;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;

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

    public boolean mouseButton(MouseButtonInfo info, int action){
        Minecraft mc = ArcRuntime.minecraft();
        double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double y = mc.mouseHandler.getScaledYPos(mc.getWindow());
        return ArcRuntime.mouseButton(scene(), x, y, width(), height(), info, action);
    }

    public boolean scrolled(double horizontal, double vertical){
        Minecraft mc = ArcRuntime.minecraft();
        double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double y = mc.mouseHandler.getScaledYPos(mc.getWindow());
        return ArcRuntime.scrolled(scene(), x, y, width(), height(), horizontal, vertical);
    }

    public boolean keyPressed(int action, KeyEvent event){
        return switch(action){
            case 1, 2 -> ArcRuntime.keyPressed(scene(), event, width(), height());
            case 0 -> ArcRuntime.keyReleased(scene(), event, width(), height());
            default -> false;
        };
    }

    public boolean charTyped(CharacterEvent event){
        return ArcRuntime.charTyped(scene(), event, width(), height());
    }

    private int width(){
        return ArcRuntime.minecraft().getWindow().getGuiScaledWidth();
    }

    private int height(){
        return ArcRuntime.minecraft().getWindow().getGuiScaledHeight();
    }
}
