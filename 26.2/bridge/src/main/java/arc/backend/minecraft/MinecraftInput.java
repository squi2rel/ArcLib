package arc.backend.minecraft;

import arc.Input;
import arc.Core;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.ui.TextField;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;

public class MinecraftInput extends Input{
    private static final char backspaceChar = 8;
    private static final char deleteChar = 127;
    private static final Vec2 tmp = new Vec2();

    private int mouseX, mouseY;
    private int deltaX, deltaY;
    private boolean touched;
    private boolean justTouched;
    private long currentEventTime;

    @Override
    public int mouseX(){
        return mouseGrabbed() ? Core.graphics.getWidth() / 2 : mouseX;
    }

    @Override
    public int mouseX(int pointer){
        return mouseX();
    }

    @Override
    public int deltaX(){
        return mouseGrabbed() ? 0 : deltaX;
    }

    @Override
    public int deltaX(int pointer){
        return deltaX();
    }

    @Override
    public int mouseY(){
        return mouseGrabbed() ? Core.graphics.getHeight() / 2 : mouseY;
    }

    @Override
    public int mouseY(int pointer){
        return mouseY();
    }

    @Override
    public int deltaY(){
        return mouseGrabbed() ? 0 : deltaY;
    }

    @Override
    public int deltaY(int pointer){
        return deltaY();
    }

    @Override
    public boolean isTouched(){
        return touched;
    }

    @Override
    public boolean justTouched(){
        return justTouched;
    }

    @Override
    public boolean isTouched(int pointer){
        return pointer == 0 && touched;
    }

    @Override
    public long getCurrentEventTime(){
        return currentEventTime;
    }

    public void postUpdate(){
        keyboard.postUpdate();
        justTouched = false;
        deltaX = 0;
        deltaY = 0;
    }

    public boolean mouseMoved(Scene scene, double x, double yTop, int height){
        updateMouse(x, yTop, height);
        return scene.mouseMoved(mouseX, mouseY);
    }

    public boolean mouseButton(Scene scene, double x, double yTop, int height, MouseButtonInfo info, int action){
        updateMouse(x, yTop, height);
        KeyCode button = KeyMapper.mouse(info);
        if(action == InputConstants.PRESS){
            touched = true;
            justTouched = true;
            keyboard.touchDown(mouseX, mouseY, 0, button);
            clearTextFocusIfOutside(scene, button);
            boolean handled = scene.touchDown(mouseX, mouseY, 0, button);
            MouseHandler mouse = mouseHandler();
            if(handled && mouse != null) ArcRuntime.releaseMouse(mouse);
            return handled;
        }else if(action == InputConstants.RELEASE){
            touched = false;
            keyboard.touchUp(mouseX, mouseY, 0, button);
            return scene.touchUp(mouseX, mouseY, 0, button);
        }
        return false;
    }

    public boolean mouseDragged(Scene scene, double x, double yTop, int height){
        updateMouse(x, yTop, height);
        return scene.touchDragged(mouseX, mouseY, 0);
    }

    public boolean scrolled(Scene scene, double x, double yTop, int height, double horizontal, double vertical){
        updateMouse(x, yTop, height);
        float amountX = (float)-horizontal;
        float amountY = (float)-vertical;
        keyboard.scrolled(amountX, amountY);
        return scene.scrolled(amountX, amountY);
    }

    public boolean keyPressed(Scene scene, KeyEvent event){
        currentEventTime = System.nanoTime();
        KeyCode key = KeyMapper.key(event);
        keyboard.keyDown(key);
        boolean handled = scene.keyDown(key);
        char typed = typedControlCharacter(key);
        return typed == 0 ? handled : handled | scene.keyTyped(typed);
    }

    public boolean keyReleased(Scene scene, KeyEvent event){
        currentEventTime = System.nanoTime();
        KeyCode key = KeyMapper.key(event);
        keyboard.keyUp(key);
        // Scene may consume the release, preventing vanilla from clearing its held binding.
        KeyMapping.set(InputConstants.getKey(event), false);
        return scene.keyUp(key);
    }

    public boolean charTyped(Scene scene, CharacterEvent event){
        currentEventTime = System.nanoTime();
        String value = event.codepointAsString();
        boolean handled = false;
        for(int i = 0; i < value.length(); i++){
            handled |= scene.keyTyped(value.charAt(i));
        }
        return handled;
    }

    protected MouseHandler mouseHandler(){
        Minecraft minecraft = ArcRuntime.minecraft();
        return minecraft == null ? null : minecraft.mouseHandler;
    }

    protected boolean mouseGrabbed(){
        MouseHandler mouse = mouseHandler();
        return mouse != null && mouse.isMouseGrabbed();
    }

    private void updateMouse(double x, double yTop, int height){
        currentEventTime = System.nanoTime();
        boolean grabbed = mouseGrabbed();
        int newX = grabbed ? Core.graphics.getWidth() / 2 : (int)Math.floor(x);
        int newY = grabbed ? height / 2 : Math.max(0, height - (int)Math.floor(yTop) - 1);
        deltaX = grabbed ? 0 : newX - mouseX;
        deltaY = grabbed ? 0 : newY - mouseY;
        mouseX = newX;
        mouseY = newY;
    }

    private void clearTextFocusIfOutside(Scene scene, KeyCode button){
        if(button != KeyCode.mouseLeft || !(scene.getKeyboardFocus() instanceof TextField focus)) return;
        scene.screenToStageCoordinates(tmp.set(mouseX, mouseY));
        Element target = scene.hit(tmp.x, tmp.y, true);
        if(target == focus || (target != null && target.isDescendantOf(focus))) return;
        scene.setKeyboardFocus(null);
    }

    private char typedControlCharacter(KeyCode key){
        // GLFW does not emit character callbacks for control keys.
        return switch(key){
            case enter -> '\n';
            case backspace -> backspaceChar;
            case forwardDel, del -> deleteChar;
            default -> 0;
        };
    }
}
