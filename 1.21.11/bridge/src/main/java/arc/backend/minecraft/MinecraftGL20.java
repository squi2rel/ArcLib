package arc.backend.minecraft;

import arc.graphics.GL20;
import arc.mock.MockGL20;

import java.nio.Buffer;

final class MinecraftGL20 extends MockGL20 {
    private final MinecraftGraphics graphics;
    private final MinecraftTextureStore textures;

    MinecraftGL20(MinecraftGraphics graphics, MinecraftTextureStore textures) {
        this.graphics = graphics;
        this.textures = textures;
    }

    @Override
    public int glGenTexture() {
        return textures.generateHandle();
    }

    @Override
    public void glBindTexture(int target, int texture) {
        textures.bind(target, texture);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
        textures.upload(target, level, width, height, format, type, pixels);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
        textures.update(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glDeleteTexture(int texture) {
        textures.delete(texture);
    }

    @Override
    public void glEnable(int cap) {
        if (cap == GL20.GL_SCISSOR_TEST) {
            graphics.enableScissor();
            return;
        }
        super.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        if (cap == GL20.GL_SCISSOR_TEST) {
            graphics.disableScissor();
            return;
        }
        super.glDisable(cap);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        graphics.scissor(x, y, width, height);
    }
}
