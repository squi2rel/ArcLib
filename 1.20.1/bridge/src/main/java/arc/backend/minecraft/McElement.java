package arc.backend.minecraft;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class McElement extends Element{
    private final Renderable renderable;
    private final GuiEventListener listener;

    public McElement(AbstractWidget widget){
        this(widget, widget);
        setSize(widget.getWidth(), widget.getHeight());
    }

    public McElement(Renderable renderable, GuiEventListener listener){
        this.renderable = renderable;
        this.listener = listener;

        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                return McElement.this.listener.mouseClicked(screenX(x), screenY(y), KeyMapper.minecraftMouse(button));
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                McElement.this.listener.mouseReleased(screenX(x), screenY(y), KeyMapper.minecraftMouse(button));
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                McElement.this.listener.mouseDragged(screenX(x), screenY(y), InputConstants.MOUSE_BUTTON_LEFT, 0, 0);
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                return McElement.this.listener.mouseScrolled(screenX(x), screenY(y), amountY);
            }

            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode){
                return McElement.this.listener.keyPressed(keyToMinecraft(keycode), 0, 0);
            }

            @Override
            public boolean keyUp(InputEvent event, KeyCode keycode){
                return McElement.this.listener.keyReleased(keyToMinecraft(keycode), 0, 0);
            }

            @Override
            public boolean keyTyped(InputEvent event, char character){
                return McElement.this.listener.charTyped(character, 0);
            }
        });
    }

    @Override
    public void draw(){
        super.draw();
        GuiGraphics graphics = ArcRuntime.batch().graphics();
        if(graphics == null) return;

        int sx = Math.round(x);
        int sy = Math.round(Core.graphics.getHeight() - y - height);
        int w = Math.round(width);
        int h = Math.round(height);
        if(renderable instanceof AbstractWidget widget){
            widget.setX(sx);
            widget.setY(sy);
            widget.setWidth(w);
            // 1.20.1 widgets expose width but no height setter.
        }
        renderable.render(graphics, Core.input.mouseX(), Core.graphics.getHeight() - Core.input.mouseY(), Core.graphics.getDeltaTime());
    }

    private double screenX(float localX){
        return x + localX;
    }

    private double screenY(float localY){
        return Core.graphics.getHeight() - (y + localY);
    }

    private int keyToMinecraft(KeyCode code){
        return switch(code){
            case enter -> InputConstants.KEY_RETURN;
            case escape -> InputConstants.KEY_ESCAPE;
            case tab -> InputConstants.KEY_TAB;
            case space -> InputConstants.KEY_SPACE;
            case backspace -> InputConstants.KEY_BACKSPACE;
            case forwardDel, del -> InputConstants.KEY_DELETE;
            case left -> InputConstants.KEY_LEFT;
            case right -> InputConstants.KEY_RIGHT;
            case up -> InputConstants.KEY_UP;
            case down -> InputConstants.KEY_DOWN;
            default -> InputConstants.UNKNOWN.getValue();
        };
    }
}
