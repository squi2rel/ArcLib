package arc.backend.minecraft;

import arc.graphics.GL20;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.Texture.TextureWrap;
import arc.util.ArcRuntimeException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns Arc-created GPU textures without registering them as Minecraft resources.
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
        long sourceAddress = MemoryUtil.memAddress(bytes);
        long targetAddress = target.getPointer() + ((long) targetY * target.getWidth() + targetX) * 4L;
        long sourceStride = (long) width * 4L;
        long targetStride = (long) target.getWidth() * 4L;
        for (int row = 0; row < height; row++) {
            MemoryUtil.memCopy(sourceAddress + row * sourceStride, targetAddress + row * targetStride, sourceStride);
        }
    }

    private static GpuSampler sampler(Texture texture) {
        AddressMode u = address(texture.getUWrap());
        AddressMode v = address(texture.getVWrap());
        FilterMode min = filter(texture.getMinFilter());
        FilterMode mag = filter(texture.getMagFilter());
        return RenderSystem.getSamplerCache().getSampler(u, v, min, mag, false);
    }

    private static AddressMode address(TextureWrap wrap) {
        return wrap == TextureWrap.clampToEdge ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
    }

    private static FilterMode filter(TextureFilter filter) {
        return filter == TextureFilter.nearest ? FilterMode.NEAREST : FilterMode.LINEAR;
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
            Entry replacement = new Entry(new DynamicTexture(() -> "Arc texture " + boundHandle, image), width, height);
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
            if (minecraftTexture.isWhiteTexture()) {
                return new TextureBinding(TextureSetup.singleTexture(whiteTexture().getTextureView(), sampler(texture)));
            }
            if (minecraftTexture.identifier() == null) return null;
            AbstractTexture resource = minecraft.getTextureManager().getTexture(minecraftTexture.identifier());
            return new TextureBinding(TextureSetup.singleTexture(resource.getTextureView(), sampler(texture)));
        }
        Entry entry = entries.get(texture.getTextureObjectHandle());
        if (entry == null) return null;
        return new TextureBinding(TextureSetup.singleTexture(entry.texture.getTextureView(), sampler(texture)));
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
            try {
                whiteTexture = new DynamicTexture(() -> "Arc white texture", image);
            } catch (RuntimeException error) {
                image.close();
                throw error;
            }
        }
        return whiteTexture;
    }

    record TextureBinding(TextureSetup setup) {
    }

    private record Entry(DynamicTexture texture, int width, int height) {
        void close() {
            texture.close();
        }
    }
}
