package arc.backend.minecraft;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.Input;
import arc.graphics.Color;
import arc.graphics.GL20;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Scene;
import arc.scene.Element;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.math.geom.Vec2;
import arc.scene.ui.TextArea;
import arc.scene.ui.Button;
import arc.scene.ui.TextField;
import arc.scene.ui.TextField.TextFieldStyle;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinecraftInputTest{
    private final Application previousApp = Core.app;
    private final Graphics previousGraphics = Core.graphics;
    private final Input previousInput = Core.input;
    private final GL20 previousGl = Core.gl;
    private final Scene previousScene = Core.scene;
    private final MouseHandler mouse = mock(MouseHandler.class);
    private boolean mouseGrabbed;
    private final MinecraftInput input = new MinecraftInput(){
        @Override protected MouseHandler mouseHandler(){ return mouse; }
    };
    private Scene scene;
    private TextFieldStyle style;

    @BeforeEach
    void setup(){
        when(mouse.isMouseGrabbed()).thenAnswer(call -> mouseGrabbed);
        doAnswer(call -> { mouseGrabbed = false; return null; }).when(mouse).releaseMouse();
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics(){
            @Override public int getWidth(){ return 801; }
            @Override public int getHeight(){ return 603; }
        };
        Core.input = input;
        Core.gl = new MockGL20();
        Core.scene = scene = new Scene();
        Font.FontData data = new Font.FontData();
        data.lineHeight = 16;
        data.capHeight = 12;
        Font font = new Font(data, new TextureRegion(), true);
        style = new TextFieldStyle();
        style.font = font;
        style.fontColor = Color.white;
    }

    @AfterEach
    void restore(){
        Core.app = previousApp;
        Core.graphics = previousGraphics;
        Core.input = previousInput;
        Core.gl = previousGl;
        Core.scene = previousScene;
    }

    @ParameterizedTest
    @ValueSource(ints = {InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER})
    void enterInsertsOneNewlinePerPressInTextArea(int keyCode){
        TextArea area = new TextArea("", style);
        area.setSize(200, 100);
        scene.add(area);
        scene.setKeyboardFocus(area);
        List<Character> typed = new ArrayList<>();
        area.typed(typed::add);

        assertTrue(press(keyCode));
        assertEquals("\n", area.getText());
        assertEquals(1, area.getCursorPosition());
        assertTrue(press(keyCode)); // Repeated key presses follow the same input path.
        assertEquals("\n\n", area.getText());
        assertEquals(2, area.getCursorPosition());
        assertEquals(List.of('\n', '\n'), typed);
        assertSame(area, scene.getKeyboardFocus());
    }

    @ParameterizedTest
    @ValueSource(ints = {InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER})
    void enterNotifiesSingleLineFieldWithoutInsertingNewline(int keyCode){
        TextField field = new TextField("", style);
        scene.add(field);
        scene.setKeyboardFocus(field);
        List<Character> typed = new ArrayList<>();
        field.typed(typed::add);

        assertTrue(press(keyCode));
        assertEquals("", field.getText());
        assertEquals(List.of('\n'), typed);
    }

    @ParameterizedTest
    @ValueSource(ints = {InputConstants.KEY_W, InputConstants.KEY_LCONTROL, InputConstants.KEY_LSHIFT})
    void aReleaseConsumedByTheFieldStillReleasesTheGameBinding(int keyCode){
        KeyMapping binding = new KeyMapping("arclib.test.release." + keyCode, keyCode, KeyMapping.Category.MOVEMENT);
        try{
            // The game receives the press before a field takes keyboard focus.
            assertFalse(press(keyCode));
            binding.setDown(true);
            assertTrue(input.keyDown(KeyMapper.key(keyCode)));
            TextField field = new TextField("", style);
            scene.add(field);
            scene.setKeyboardFocus(field);

            assertTrue(input.keyReleased(scene, new KeyEvent(keyCode, 0, 0)));

            assertFalse(binding.isDown());
            assertFalse(input.keyDown(KeyMapper.key(keyCode)));
            assertSame(field, scene.getKeyboardFocus());
        }finally{
            binding.setDown(false);
        }
    }

    @Test
    void capturedMouseStaysAtTheCenterAndDoesNotReportCameraMovement(){
        input.mouseMoved(scene, 17.5, 23.5, 603);
        List<Vec2> positions = new ArrayList<>();
        scene.addCaptureListener(new InputListener(){
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                positions.add(new Vec2(event.stageX, event.stageY));
                return false;
            }
        });

        mouseGrabbed = true;
        assertEquals(400, input.mouseX());
        assertEquals(301, input.mouseY());
        assertEquals(400, input.mouseX(0));
        assertEquals(301, input.mouseY(0));
        assertEquals(0, input.deltaX());
        assertEquals(0, input.deltaY());
        assertEquals(0, input.deltaX(0));
        assertEquals(0, input.deltaY(0));

        input.mouseMoved(scene, 18000, -12000, 603);
        input.mouseMoved(scene, -14000, 16000, 603);

        assertEquals(2, positions.size());
        for(Vec2 position : positions) assertCenter(position);
        assertEquals(400, input.mouseX());
        assertEquals(301, input.mouseY());
        assertEquals(0, input.deltaX());
        assertEquals(0, input.deltaY());
    }

    @Test
    void releasingTheMouseRestoresRealCursorCoordinatesAndDeltas(){
        mouseGrabbed = true;
        input.mouseMoved(scene, 18000, -12000, 603);
        List<Vec2> positions = new ArrayList<>();
        scene.addCaptureListener(new InputListener(){
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                positions.add(new Vec2(event.stageX, event.stageY));
                return false;
            }
        });

        mouseGrabbed = false;
        input.mouseMoved(scene, 123.9, 456.8, 603);

        assertEquals(123, input.mouseX());
        assertEquals(146, input.mouseY());
        assertEquals(-277, input.deltaX());
        assertEquals(-155, input.deltaY());
        assertEquals(1, positions.size());
        assertEquals(123f, positions.get(0).x, 0.001f);
        assertEquals(146f, positions.get(0).y, 0.001f);
    }

    @Test
    void capturedClicksDragsAndScrollsUseTheSameCenterPosition(){
        mouseGrabbed = true;
        input.mouseMoved(scene, 18000, -12000, 603);
        List<Vec2> positions = new ArrayList<>();
        scene.addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, arc.input.KeyCode button){
                positions.add(new Vec2(event.stageX, event.stageY));
                return true;
            }
            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                positions.add(new Vec2(event.stageX, event.stageY));
            }
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                positions.add(new Vec2(event.stageX, event.stageY));
                return true;
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, arc.input.KeyCode button){
                positions.add(new Vec2(event.stageX, event.stageY));
            }
        });

        assertTrue(input.mouseButton(scene, -7000, 8000, 603, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.PRESS));
        assertFalse(mouseGrabbed);
        // Re-grabbing while a press is held must also center subsequent drag events.
        mouseGrabbed = true;
        assertTrue(input.mouseDragged(scene, 9000, -10000, 603));
        assertTrue(input.scrolled(scene, -11000, 12000, 603, 0, 1));
        assertTrue(input.mouseButton(scene, 13000, -14000, 603, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.RELEASE));

        assertEquals(4, positions.size());
        for(Vec2 position : positions) assertCenter(position);
        assertFalse(input.isTouched());
    }

    @Test
    void clickingAButtonWithoutKeyboardFocusUnlocksAndReleasesHeldGameKeys(){
        Button button = new Button(new Button.ButtonStyle());
        button.setBounds(350f, 250f, 100f, 100f);
        Element child = new Element();
        button.add(child).size(60f, 60f);
        button.validate();
        int[] clicks = {0};
        button.clicked(() -> clicks[0]++);
        scene.add(button);
        assertSame(child, scene.hit(400f, 301f, true));
        KeyMapping binding = new KeyMapping("arclib.test.click.forward", InputConstants.KEY_W, KeyMapping.Category.MOVEMENT);
        try{
            binding.setDown(true);
            mouseGrabbed = true;

            assertTrue(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.PRESS));

            assertFalse(mouseGrabbed);
            assertFalse(binding.isDown());
            assertNull(scene.getKeyboardFocus());
            assertTrue(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.RELEASE));
            assertEquals(1, clicks[0]);
        }finally{
            binding.setDown(false);
        }
    }

    @Test
    void emptySpaceAndAnElementWithoutClickHandlingDoNotUnlock(){
        mouseGrabbed = true;
        assertFalse(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.PRESS));
        assertFalse(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.RELEASE));
        Element decoration = new Element();
        decoration.setBounds(350f, 250f, 100f, 100f);
        scene.add(decoration);

        assertFalse(input.mouseMoved(scene, 18000, -12000, 603));
        assertFalse(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.PRESS));
        assertFalse(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.RELEASE));

        assertTrue(mouseGrabbed);
        assertNull(scene.getKeyboardFocus());
        verify(mouse, never()).releaseMouse();
    }

    @Test
    void aCustomRightClickHandlerUnlocksOnlyForTheConfiguredButton(){
        Element clickable = new Element();
        clickable.setBounds(350f, 250f, 100f, 100f);
        int[] clicks = {0};
        clickable.clicked(KeyCode.mouseRight, () -> clicks[0]++);
        scene.add(clickable);
        mouseGrabbed = true;

        assertFalse(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.PRESS));
        assertFalse(mouseButton(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.RELEASE));
        assertTrue(mouseGrabbed);
        assertTrue(mouseButton(InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.PRESS));
        assertFalse(mouseGrabbed);
        assertNull(scene.getKeyboardFocus());
        assertTrue(mouseButton(InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.RELEASE));
        assertEquals(1, clicks[0]);
    }

    private boolean mouseButton(int button, int action){
        return input.mouseButton(scene, 400, 301, 603, new MouseButtonInfo(button, 0), action);
    }

    private static void assertCenter(Vec2 position){
        assertEquals(400f, position.x, 0.001f);
        assertEquals(301f, position.y, 0.001f);
    }

    private boolean press(int keyCode){
        return input.keyPressed(scene, new KeyEvent(keyCode, 0, 0));
    }
}
