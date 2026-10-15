package arc.backend.minecraft;

import arc.Graphics.Cursor;
import arc.Graphics.Cursor.SystemCursor;
import arc.graphics.Pixmap;
import com.github.squi2rel.arc.mixin.CursorTypeInvoker;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinecraftCursorTest{
    private final Window window = mock(Window.class);
    private final Minecraft minecraft = mock(Minecraft.class);
    private final MinecraftGraphics graphics;

    MinecraftCursorTest(){
        when(minecraft.getWindow()).thenReturn(window);
        graphics = new MinecraftGraphics(minecraft);
    }

    @BeforeAll
    static void initializeMinecraftCursors(){
        // Give vanilla's standard cursor objects distinct handles without opening a window.
        try(var glfw = mockStatic(GLFW.class)){
            glfw.when(() -> GLFW.glfwCreateStandardCursor(anyInt())).thenAnswer(call -> (long)(int)call.getArgument(0));
            assertNotSame(CursorTypes.ARROW, CursorTypes.POINTING_HAND);
        }
    }

    @Test
    void hoverSurvivesMinecraftRebuildingGuiGraphicsEachFrame(){
        graphics.cursor(SystemCursor.hand);
        for(int frame = 0; frame < 3; frame++){
            GuiGraphicsExtractor gui = mock(GuiGraphicsExtractor.class, CALLS_REAL_METHODS);
            gui.requestCursor(CursorType.DEFAULT);
            graphics.cursor(SystemCursor.hand); // Arc caches this repeated call.
            graphics.requestCursor(gui);
            gui.applyCursor(window);
        }
        verify(window, times(4)).selectCursor(CursorTypes.POINTING_HAND);
    }

    @Test
    void restoringCursorLetsVanillaChooseTheNextShape(){
        graphics.cursor(SystemCursor.hand);
        graphics.restoreCursor();
        GuiGraphicsExtractor gui = mock(GuiGraphicsExtractor.class, CALLS_REAL_METHODS);
        gui.requestCursor(CursorTypes.IBEAM);
        graphics.requestCursor(gui);
        gui.applyCursor(window);
        verify(window).selectCursor(CursorTypes.IBEAM);
    }

    @Test
    void customCursorReadsTheWholePixmapAndCanBeDisposedTwice(){
        ByteBuffer pixels = ByteBuffer.allocateDirect(16);
        pixels.putInt(0, 0x12345678);
        Pixmap pixmap = new Pixmap(pixels, 2, 2);
        pixels.position(4).limit(8);
        try(var glfw = mockStatic(GLFW.class);
            var factory = mockStatic(CursorTypeInvoker.class)){
            CursorType type = mock(CursorType.class);
            factory.when(() -> CursorTypeInvoker.arclib$create("arclib", 7L)).thenReturn(type);
            glfw.when(() -> GLFW.glfwCreateCursor(any(GLFWImage.class), eq(1), eq(0))).thenAnswer(call -> {
                GLFWImage image = call.getArgument(0);
                assertEquals(2, image.width());
                assertEquals(2, image.height());
                assertEquals(0x12, image.pixels(16).get(0));
                return 7L;
            });

            Cursor cursor = graphics.newCursor(pixmap, 1, 0);
            assertEquals(4, pixels.position());
            assertEquals(8, pixels.limit());
            graphics.cursor(cursor);
            verify(window).selectCursor(type);
            cursor.dispose();
            cursor.dispose();
            graphics.dispose();
            verify(window).selectCursor(CursorType.DEFAULT);
            glfw.verify(() -> GLFW.glfwDestroyCursor(7L));
        }
    }
}
