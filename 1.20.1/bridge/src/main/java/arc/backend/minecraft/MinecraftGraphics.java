package arc.backend.minecraft;

import arc.Application.ApplicationType;
import arc.Graphics;
import arc.graphics.GL20;
import arc.graphics.GL30;
import arc.graphics.Pixmap;
import arc.graphics.gl.GLVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class MinecraftGraphics extends Graphics {
    private final Minecraft minecraft;
    private final MinecraftTextureStore textureStore;
    private final List<MinecraftCursor> cursors = new ArrayList<>();
    private final EnumMap<Cursor.SystemCursor, Long> systemCursors = new EnumMap<>(Cursor.SystemCursor.class);
    private final BufferFormat bufferFormat = new BufferFormat(8, 8, 8, 8, 24, 8, 0, false);
    private MinecraftCursor activeCursor;
    private GL20 gl20;
    private int widthOverride = -1;
    private int heightOverride = -1;
    private GuiGraphics frameGraphics;
    private int frameGuiX, frameGuiY;
    private float frameScaleX = 1f, frameScaleY = 1f;
    private boolean scissorEnabled;
    private boolean scissorApplied;
    private long frameId;
    private long lastFrameNs = System.nanoTime();
    private float delta = 1f / 60f;
    private boolean continuous = true;

    public MinecraftGraphics(Minecraft minecraft) {
        this.minecraft = minecraft;
        textureStore = new MinecraftTextureStore(minecraft);
        gl20 = new MinecraftGL20(this, textureStore);
    }

    public void beginFrame(int width, int height, float delta, GuiGraphics graphics, int guiX, int guiY, float scaleX, float scaleY) {
        textureStore.beginFrame();
        widthOverride = width;
        heightOverride = height;
        this.delta = delta > 0f ? delta : computeDelta();
        frameGraphics = graphics;
        frameGuiX = guiX;
        frameGuiY = guiY;
        frameScaleX = Math.max(0.001f, scaleX);
        frameScaleY = Math.max(0.001f, scaleY);
        scissorEnabled = false;
        scissorApplied = false;
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
    }

    void scissor(int x, int y, int width, int height) {
        if (!scissorEnabled || frameGraphics == null || width <= 0 || height <= 0) return;

        if (scissorApplied) frameGraphics.disableScissor();

        int localLeft = Math.max(0, Math.min(widthOverride, x));
        int localRight = Math.max(0, Math.min(widthOverride, x + width));
        int localBottom = Math.max(0, Math.min(heightOverride, y));
        int localTop = Math.max(0, Math.min(heightOverride, y + height));
        int left = frameGuiX + (int) Math.floor(localLeft / frameScaleX);
        int right = frameGuiX + (int) Math.ceil(localRight / frameScaleX);
        int top = frameGuiY + (int) Math.floor((heightOverride - localTop) / frameScaleY);
        int bottom = frameGuiY + (int) Math.ceil((heightOverride - localBottom) / frameScaleY);
        frameGraphics.enableScissor(left, top, Math.max(left, right), Math.max(top, bottom));
        scissorApplied = true;
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
        minecraft.getWindow().updateVsync(vsync);
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
        activeCursor = nativeCursor;
        GLFW.glfwSetCursor(minecraft.getWindow().getWindow(), nativeCursor.handle);
    }

    @Override
    protected void setSystemCursor(Cursor.SystemCursor systemCursor) {
        activeCursor = null;
        long handle = systemCursors.computeIfAbsent(systemCursor, cursor -> GLFW.glfwCreateStandardCursor(switch (cursor) {
            case arrow -> GLFW.GLFW_ARROW_CURSOR;
            case ibeam -> GLFW.GLFW_IBEAM_CURSOR;
            case crosshair -> GLFW.GLFW_CROSSHAIR_CURSOR;
            case hand -> GLFW.GLFW_HAND_CURSOR;
            case horizontalResize -> GLFW.GLFW_HRESIZE_CURSOR;
            case verticalResize -> GLFW.GLFW_VRESIZE_CURSOR;
        }));
        GLFW.glfwSetCursor(minecraft.getWindow().getWindow(), handle);
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
        } finally {
            if (activeCursor != null || !systemCursors.isEmpty()) {
                GLFW.glfwSetCursor(minecraft.getWindow().getWindow(), 0L);
                activeCursor = null;
            }
            while (!cursors.isEmpty()) cursors.get(cursors.size() - 1).dispose();
            for (long handle : systemCursors.values()) {
                if (handle != 0L) GLFW.glfwDestroyCursor(handle);
            }
            systemCursors.clear();
        }
    }

    private class MinecraftCursor implements Cursor {
        private long handle;

        MinecraftCursor(long handle) {
            this.handle = handle;
        }

        @Override
        public void dispose() {
            if (handle == 0L) return;
            if (activeCursor == this) {
                GLFW.glfwSetCursor(minecraft.getWindow().getWindow(), 0L);
                activeCursor = null;
            }
            GLFW.glfwDestroyCursor(handle);
            handle = 0L;
            cursors.remove(this);
        }
    }
}
