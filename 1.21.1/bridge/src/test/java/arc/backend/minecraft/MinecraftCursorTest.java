package arc.backend.minecraft;

import arc.Graphics.Cursor;
import arc.Graphics.Cursor.SystemCursor;
import arc.graphics.Pixmap;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
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
        when(window.getWindow()).thenReturn(1L);
        graphics = new MinecraftGraphics(minecraft);
    }

    @Test
    void reusesStandardCursorsAndReleasesThemOnce(){
        try(var glfw = mockStatic(GLFW.class)){
            glfw.when(() -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)).thenReturn(2L);
            glfw.when(() -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)).thenReturn(3L);
            graphics.cursor(SystemCursor.hand);
            graphics.restoreCursor();
            graphics.cursor(SystemCursor.hand);
            graphics.dispose();
            graphics.dispose();

            glfw.verify(() -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
            glfw.verify(() -> GLFW.glfwSetCursor(1L, 2L), times(2));
            glfw.verify(() -> GLFW.glfwSetCursor(1L, 3L));
            glfw.verify(() -> GLFW.glfwDestroyCursor(2L));
            glfw.verify(() -> GLFW.glfwDestroyCursor(3L));
        }
    }

    @Test
    void customCursorReadsTheWholePixmapAndCanBeDisposedTwice(){
        ByteBuffer pixels = ByteBuffer.allocateDirect(16);
        pixels.putInt(0, 0x12345678);
        Pixmap pixmap = new Pixmap(pixels, 2, 2);
        pixels.position(4).limit(8);
        try(var glfw = mockStatic(GLFW.class)){
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
            glfw.verify(() -> GLFW.glfwSetCursor(1L, 7L));
            cursor.dispose();
            cursor.dispose();
            graphics.dispose();
            glfw.verify(() -> GLFW.glfwSetCursor(1L, 0L));
            glfw.verify(() -> GLFW.glfwDestroyCursor(7L));
        }
    }
}
