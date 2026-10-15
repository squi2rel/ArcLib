package arc.backend.minecraft;

import arc.scene.Element;
import arc.scene.Scene;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ArcRuntimeFocusTest{
    private final Scene scene = mock(Scene.class);
    private final MouseHandler mouse = mock(MouseHandler.class);

    private final KeyMapping movement = new KeyMapping("arclib.test.focus.forward", InputConstants.KEY_W, KeyMapping.CATEGORY_MOVEMENT);

    @BeforeEach
    void holdMovementKey(){
        movement.setDown(true);
    }

    @AfterEach
    void releaseGameKeys(){
        KeyMapping.releaseAll();
    }

    @Test
    void keyboardFocusReleasesMinecraftMouseGrabAndHeldGameKeys(){
        when(scene.getKeyboardFocus()).thenReturn(new Element());
        when(mouse.isMouseGrabbed()).thenReturn(true);

        ArcRuntime.releaseMouseIfFocused(scene, mouse);

        verify(mouse).releaseMouse();
        assertFalse(movement.isDown());
    }

    @Test
    void anAlreadyReleasedMouseIsLeftAlone(){
        when(scene.getKeyboardFocus()).thenReturn(new Element());
        when(mouse.isMouseGrabbed()).thenReturn(false);

        ArcRuntime.releaseMouseIfFocused(scene, mouse);

        verify(mouse, never()).releaseMouse();
        assertTrue(movement.isDown());
    }

    @Test
    void scrollFocusAloneDoesNotUnlockTheMouseWhilePlaying(){
        when(scene.getScrollFocus()).thenReturn(new Element());
        when(mouse.isMouseGrabbed()).thenReturn(true);

        ArcRuntime.releaseMouseIfFocused(scene, mouse);

        verify(mouse, never()).releaseMouse();
        assertTrue(movement.isDown());
    }
}
