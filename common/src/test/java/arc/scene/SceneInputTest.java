package arc.scene;

import arc.Core;
import arc.Graphics;
import arc.graphics.GL20;
import arc.input.KeyCode;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneInputTest{
    private final Graphics previousGraphics = Core.graphics;
    private final GL20 previousGl = Core.gl, previousGl20 = Core.gl20;
    private Scene scene;

    @BeforeEach
    void setup(){
        Core.gl = Core.gl20 = new MockGL20();
        Core.graphics = new MockGraphics(){
            @Override public int getWidth(){ return 800; }
            @Override public int getHeight(){ return 600; }
        };
        scene = new Scene();
    }

    @AfterEach
    void restore(){
        Core.graphics = previousGraphics;
        Core.gl = previousGl;
        Core.gl20 = previousGl20;
    }

    @Test
    void emptySpaceClicksReachCaptureListenersWithoutClaimingMouseInput(){
        int[] clicks = {0};
        scene.addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                assertSame(scene.root, event.targetActor);
                assertEquals(5f, x, 0.001f);
                assertEquals(5f, y, 0.001f);
                assertEquals(KeyCode.mouseLeft, button);
                clicks[0]++;
                return false;
            }
        });

        assertFalse(scene.hasMouse(5f, 5f));
        assertFalse(scene.touchDown(5, 5, 0, KeyCode.mouseLeft));
        assertEquals(1, clicks[0]);
        assertFalse(scene.touchUp(5, 5, 0, KeyCode.mouseLeft));
    }

    @Test
    void disabledRootDoesNotReceiveEmptySpaceClicks(){
        scene.root.touchable = Touchable.disabled;
        scene.addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fail("Disabled scene received a click");
                return true;
            }
        });

        assertFalse(scene.touchDown(5, 5, 0, KeyCode.mouseLeft));
    }

}
