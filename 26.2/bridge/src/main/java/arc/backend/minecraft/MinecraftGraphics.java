package arc.backend.minecraft;

import arc.Application.ApplicationType;
import arc.Graphics;
import arc.graphics.GL20;
import arc.graphics.GL30;
import arc.graphics.Pixmap;
import arc.graphics.gl.GLVersion;
import com.github.squi2rel.arc.mixin.CursorTypeInvoker;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

public class MinecraftGraphics extends Graphics {
    private final Minecraft minecraft;
    private final MinecraftTextureStore textureStore;
    private final List<MinecraftCursor> cursors = new ArrayList<>();
    private final BufferFormat bufferFormat = new BufferFormat(8, 8, 8, 8, 24, 8, 0, false);
    private CursorType requestedCursor;
    private GL20 gl20;
    private int widthOverride = -1;
    private int heightOverride = -1;
    private GuiGraphicsExtractor frameGraphics;
    private boolean scissorEnabled;
    private boolean scissorApplied;
    private ScreenRectangle currentScissor;
    private long frameId;
    private long lastFrameNs = System.nanoTime();
    private float delta = 1f / 60f;
    private boolean continuous = true;

    public MinecraftGraphics(Minecraft minecraft) {
        this.minecraft = minecraft;
        textureStore = new MinecraftTextureStore(minecraft);
        gl20 = new MinecraftGL20(this, textureStore);
    }

    public void beginFrame(int width, int height, float delta, GuiGraphicsExtractor graphics) {
        textureStore.beginFrame();
        widthOverride = width;
        heightOverride = height;
        this.delta = delta > 0f ? delta : computeDelta();
        frameGraphics = graphics;
        scissorEnabled = false;
        scissorApplied = false;
        currentScissor = null;
        frameId++;
    }

    MinecraftTextureStore textureStore() {
        return textureStore;
    }

    void disposeResources() {
        try {
            textureStore.dispose();
        } finally {
            dispose();
        }
    }

    public void endFrame() {
        disableScissor();
        frameGraphics = null;
        widthOverride = -1;
        heightOverride = -1;
    }

    public void withSize(int width, int height, Runnable runnable) {
        int oldWidth = widthOverride;
        int oldHeight = heightOverride;
        widthOverride = width;
        heightOverride = height;
        try {
            runnable.run();
        } finally {
            widthOverride = oldWidth;
            heightOverride = oldHeight;
        }
    }

    private float computeDelta() {
        long now = System.nanoTime();
        float out = Math.min((now - lastFrameNs) / 1_000_000_000f, 0.25f);
        lastFrameNs = now;
        return out <= 0f ? 1f / 60f : out;
    }

    @Override
    public GL20 getGL20() {
        return gl20;
    }

    @Override
    public void setGL20(GL20 gl20) {
        this.gl20 = gl20;
    }

    @Override
    public GL30 getGL30() {
        return null;
    }

    @Override
    public void setGL30(GL30 gl30) {
    }

    @Override
    public int getWidth() {
        if (widthOverride > 0) return widthOverride;
        return minecraft.getWindow().getWidth();
    }

    @Override
    public int getHeight() {
        if (heightOverride > 0) return heightOverride;
        return minecraft.getWindow().getHeight();
    }

    @Override
    public int getBackBufferWidth() {
        if (widthOverride > 0) return widthOverride;
        return minecraft.getWindow().getWidth();
    }

    @Override
    public int getBackBufferHeight() {
        if (heightOverride > 0) return heightOverride;
        return minecraft.getWindow().getHeight();
    }

    void enableScissor() {
        scissorEnabled = true;
    }

    void disableScissor() {
        if (scissorApplied && frameGraphics != null) frameGraphics.disableScissor();
        scissorApplied = false;
        scissorEnabled = false;
        currentScissor = null;
    }

    void scissor(int x, int y, int width, int height) {
        if (!scissorEnabled || frameGraphics == null || width <= 0 || height <= 0) return;

        if (scissorApplied) frameGraphics.disableScissor();

        int left = Math.clamp(x, 0, widthOverride);
        int right = Math.clamp(x + width, 0, widthOverride);
        int bottom = Math.clamp(y, 0, heightOverride);
        int top = Math.clamp(y + height, 0, heightOverride);

        // GuiGraphicsExtractor applies the current pose to the scissor rectangle. Arc is drawn under
        // a y-flipped pose, so the local y coordinates must be supplied in reverse order.
        frameGraphics.enableScissor(left, top, Math.max(left, right), Math.min(top, bottom));
        currentScissor = new ScreenRectangle(left, top, Math.max(left, right) - left, Math.min(top, bottom) - top)
                .transformAxisAligned(frameGraphics.pose());
        scissorApplied = true;
    }

    ScreenRectangle currentScissor() {
        return currentScissor;
    }

    @Override
    public long getFrameId() {
        return frameId;
    }

    @Override
    public float getDeltaTime() {
        return delta;
    }

    @Override
    public int getFramesPerSecond() {
        return minecraft.getFps();
    }

    @Override
    public GLVersion getGLVersion() {
        return new GLVersion(ApplicationType.desktop, "Minecraft GUI", "unknown", "minecraft");
    }

    @Override
    public float getPpiX() {
        return 96f;
    }

    @Override
    public float getPpiY() {
        return 96f;
    }

    @Override
    public float getPpcX() {
        return getPpiX() / 2.54f;
    }

    @Override
    public float getPpcY() {
        return getPpiY() / 2.54f;
    }

    @Override
    public float getDensity() {
        return 1f;
    }

    @Override
    public void setTitle(String title) {
        minecraft.getWindow().setTitle(title);
    }

    @Override
    public void setVSync(boolean vsync) {
        minecraft.options.enableVsync().set(vsync);
    }

    @Override
    public BufferFormat getBufferFormat() {
        return bufferFormat;
    }

    @Override
    public boolean supportsExtension(String extension) {
        return false;
    }

    @Override
    public boolean isContinuousRendering() {
        return continuous;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
        continuous = isContinuous;
    }

    @Override
    public void requestRendering() {
    }

    @Override
    public boolean isFullscreen() {
        return minecraft.getWindow().isFullscreen();
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
        if (pixmap == null || pixmap.isDisposed())
            throw new IllegalArgumentException("Cursor pixmap must not be null or disposed.");
        if (xHotspot < 0 || xHotspot >= pixmap.width || yHotspot < 0 || yHotspot >= pixmap.height) {
            throw new IllegalArgumentException("Cursor hotspot must be inside the pixmap.");
        }

        long handle;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage image = GLFWImage.malloc(stack)
                    .set(pixmap.width, pixmap.height, pixmap.getPixels().duplicate().clear());
            handle = GLFW.glfwCreateCursor(image, xHotspot, yHotspot);
        }
        if (handle == 0L) throw new IllegalStateException("Unable to create mouse cursor.");
        MinecraftCursor cursor = new MinecraftCursor(handle);
        cursors.add(cursor);
        return cursor;
    }

    @Override
    protected void setCursor(Cursor cursor) {
        if (!(cursor instanceof MinecraftCursor nativeCursor) || nativeCursor.handle == 0L) {
            throw new IllegalArgumentException("Cursor must be a live Minecraft cursor.");
        }
        requestedCursor = nativeCursor.type;
        minecraft.getWindow().selectCursor(requestedCursor);
    }

    @Override
    protected void setSystemCursor(Cursor.SystemCursor systemCursor) {
        requestedCursor = switch (systemCursor) {
            case arrow -> CursorTypes.ARROW;
            case ibeam -> CursorTypes.IBEAM;
            case crosshair -> CursorTypes.CROSSHAIR;
            case hand -> CursorTypes.POINTING_HAND;
            case horizontalResize -> CursorTypes.RESIZE_EW;
            case verticalResize -> CursorTypes.RESIZE_NS;
        };
        minecraft.getWindow().selectCursor(requestedCursor);
    }

    /**
     * Adds Arc's active cursor to Minecraft's per-frame cursor selection.
     */
    public void requestCursor(GuiGraphicsExtractor graphics) {
        // Releasing the Arc cursor lets vanilla widgets choose their own shape.
        if (requestedCursor != null && requestedCursor != CursorTypes.ARROW) {
            graphics.requestCursor(requestedCursor);
        }
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
        } finally {
            if (requestedCursor != null) {
                minecraft.getWindow().selectCursor(CursorType.DEFAULT);
                requestedCursor = null;
            }
            while (!cursors.isEmpty()) cursors.getLast().dispose();
        }
    }

    private class MinecraftCursor implements Cursor {
        private final CursorType type;
        private long handle;

        MinecraftCursor(long handle) {
            this.handle = handle;
            type = CursorTypeInvoker.arclib$create("arclib", handle);
        }

        @Override
        public void dispose() {
            if (handle == 0L) return;
            if (requestedCursor == type) {
                minecraft.getWindow().selectCursor(CursorType.DEFAULT);
                requestedCursor = null;
            }
            GLFW.glfwDestroyCursor(handle);
            handle = 0L;
            cursors.remove(this);
        }
    }
}
