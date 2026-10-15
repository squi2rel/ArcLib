package arc.backend.minecraft;

import arc.graphics.GL20;
import arc.graphics.Texture;
import arc.util.ArcRuntimeException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns Arc-created GPU textures and exposes their OpenGL ids to the 1.20.1 GUI renderer.
 */
final class MinecraftTextureStore {
    private final Minecraft minecraft;
    private final Map<Integer, Entry> entries = new HashMap<>();
    private final ArrayList<Entry> pendingClose = new ArrayList<>();
    private DynamicTexture whiteTexture;
    private int nextHandle = 1;
    private int boundHandle;

    MinecraftTextureStore(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    static void copyRgba(Buffer source, int width, int height, NativeImage target, int targetX, int targetY) {
        if (!(source instanceof ByteBuffer bytes) || !bytes.isDirect()) {
            throw new ArcRuntimeException("Arc texture pixels must be a direct ByteBuffer");
        }
        long required = (long) width * height * 4L;
        if (bytes.remaining() < required) throw new ArcRuntimeException("Not enough RGBA texture data");
        int sourceOffset = bytes.position();
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int offset = sourceOffset + (row * width + column) * 4;
                int r = bytes.get(offset) & 0xff;
                int g = bytes.get(offset + 1) & 0xff;
                int b = bytes.get(offset + 2) & 0xff;
                int a = bytes.get(offset + 3) & 0xff;
                target.setPixelRGBA(targetX + column, targetY + row, a << 24 | b << 16 | g << 8 | r);
            }
        }
    }

    private static void requireTexture2d(int target) {
        if (target != GL20.GL_TEXTURE_2D)
            throw new ArcRuntimeException("Only GL_TEXTURE_2D is supported by the Minecraft UI backend");
    }

    private static void requireRgba(int format, int type) {
        if (format != GL20.GL_RGBA || type != GL20.GL_UNSIGNED_BYTE) {
            throw new ArcRuntimeException("Only RGBA8888 textures are supported by the Minecraft UI backend");
        }
    }

    int generateHandle() {
        return nextHandle++;
    }

    void bind(int target, int handle) {
        requireTexture2d(target);
        boundHandle = handle;
    }

    void upload(int target, int level, int width, int height, int format, int type, Buffer pixels) {
        requireTexture2d(target);
        requireRgba(format, type);
        if (level != 0) return;
        if (boundHandle == 0) throw new ArcRuntimeException("No Arc texture is bound");
        if (width <= 0 || height <= 0) throw new ArcRuntimeException("Invalid texture size: " + width + "x" + height);
        RenderSystem.assertOnRenderThread();

        NativeImage image = new NativeImage(width, height, false);
        boolean retained = false;
        try {
            if (pixels == null) {
                image.fillRect(0, 0, width, height, 0);
            } else {
                copyRgba(pixels, width, height, image, 0, 0);
            }
            DynamicTexture texture = new DynamicTexture(image);
            texture.upload();
            Entry replacement = new Entry(texture, width, height);
            retained = true;
            retire(entries.put(boundHandle, replacement));
        } finally {
            if (!retained) image.close();
        }
    }

    void update(int target, int level, int x, int y, int width, int height, int format, int type, Buffer pixels) {
        requireTexture2d(target);
        requireRgba(format, type);
        if (level != 0) return;
        Entry entry = entries.get(boundHandle);
        if (entry == null) throw new ArcRuntimeException("No uploaded Arc texture is bound");
        if (pixels == null) return;
        if (x < 0 || y < 0 || width < 0 || height < 0 || x + width > entry.width || y + height > entry.height) {
            throw new ArcRuntimeException("Texture update is outside " + entry.width + "x" + entry.height);
        }
        RenderSystem.assertOnRenderThread();
        copyRgba(pixels, width, height, entry.texture.getPixels(), x, y);
        entry.texture.upload();
    }

    void delete(int handle) {
        if (handle == 0) return;
        retire(entries.remove(handle));
        if (boundHandle == handle) boundHandle = 0;
    }

    TextureBinding binding(Texture texture) {
        if (texture instanceof MinecraftTexture minecraftTexture) {
            if (minecraftTexture.isWhiteTexture()) return new TextureBinding(whiteTexture().getId());
            if (minecraftTexture.identifier() == null) return null;
            return new TextureBinding(minecraft.getTextureManager().getTexture(minecraftTexture.identifier()).getId());
        }
        Entry entry = entries.get(texture.getTextureObjectHandle());
        if (entry == null) return null;
        RenderSystem.bindTextureForSetup(entry.texture.getId());
        RenderSystem.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, texture.getMinFilter().glEnum);
        RenderSystem.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, texture.getMagFilter().glEnum);
        RenderSystem.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, texture.getUWrap().getGLEnum());
        RenderSystem.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, texture.getVWrap().getGLEnum());
        return new TextureBinding(entry.texture.getId());
    }

    void beginFrame() {
        for (Entry entry : pendingClose) entry.close();
        pendingClose.clear();
    }

    void dispose() {
        if (whiteTexture != null) {
            whiteTexture.close();
            whiteTexture = null;
        }
        for (Entry entry : entries.values()) entry.close();
        entries.clear();
        for (Entry entry : pendingClose) entry.close();
        pendingClose.clear();
        boundHandle = 0;
    }

    private void retire(Entry entry) {
        if (entry != null) pendingClose.add(entry);
    }

    private DynamicTexture whiteTexture() {
        if (whiteTexture == null) {
            RenderSystem.assertOnRenderThread();
            NativeImage image = new NativeImage(1, 1, false);
            image.fillRect(0, 0, 1, 1, 0xffffffff);
            DynamicTexture created = null;
            try {
                created = new DynamicTexture(image);
                created.upload();
                whiteTexture = created;
            } catch (RuntimeException error) {
                if (created == null) image.close();
                else created.close();
                throw error;
            }
        }
        return whiteTexture;
    }

    record TextureBinding(int id) {
    }

    private record Entry(DynamicTexture texture, int width, int height) {
        void close() {
            texture.close();
        }
    }
}
