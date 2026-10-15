package arc.backend.minecraft;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Batch;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public class MinecraftBatch extends Batch {
    private static final Color tmpColor = new Color();

    private final MinecraftTextureStore textures;
    private GuiGraphics graphics;

    MinecraftBatch(MinecraftTextureStore textures) {
        this.textures = textures;
    }

    static int packedToArgb(float packed) {
        int abgr = Float.floatToRawIntBits(packed);
        int alpha = abgr >>> 24;
        if (alpha == 254) alpha = 255;
        return alpha << 24 | (abgr & 0xff) << 16 | abgr & 0xff00 | abgr >>> 16 & 0xff;
    }

    private static void applyTransform(Matrix3x2fStack pose, Mat mat) {
        if (mat == null) return;
        pose.mul(new Matrix3x2f(
                mat.val[Mat.M00], mat.val[Mat.M10],
                mat.val[Mat.M01], mat.val[Mat.M11],
                mat.val[Mat.M02], mat.val[Mat.M12]
        ));
    }

    public static int toArgb(Color color) {
        int a = Math.round(color.a * 255f) & 0xff;
        int r = Math.round(color.r * 255f) & 0xff;
        int g = Math.round(color.g * 255f) & 0xff;
        int b = Math.round(color.b * 255f) & 0xff;
        return a << 24 | r << 16 | g << 8 | b;
    }

    public void begin(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public void end() {
        this.graphics = null;
    }

    public void fillRect(float x, float y, float width, float height, Color color) {
        if (graphics == null || color.a <= 0f || width == 0f || height == 0f) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y + height);
        pose.scale(1f, -1f);
        graphics.fill(0, 0, Math.round(width), Math.round(height), toArgb(color));
        pose.popMatrix();
    }

    public void drawTexture(Identifier id, int textureWidth, int textureHeight, float x, float y, float width, float height,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight, Color color) {
        if (graphics == null || id == null || color.a <= 0f || width == 0f || height == 0f || sourceWidth <= 0 || sourceHeight <= 0)
            return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y + height);
        pose.scale(1f, -1f);
        graphics.blit(ArcRenderPipelines.GUI_TEXTURED_NO_CULL, id, 0, 0, sourceX, sourceY, Math.round(width), Math.round(height),
                sourceWidth, sourceHeight, textureWidth, textureHeight, toArgb(color));
        pose.popMatrix();
    }

    public void drawText(String text, float x, float y, Color color, float scaleX, float scaleY) {
        if (graphics == null || text == null || text.isEmpty() || color.a <= 0f) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y);
        float fontScale = MinecraftFont.scaled();
        pose.scale(scaleX * fontScale, -scaleY * fontScale);
        graphics.drawString(ArcRuntime.minecraft().font, text, 0, 0, toArgb(color), false);
        pose.popMatrix();
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        if (graphics == null || count < 24) return;

        int usable = count - count % 24;
        submit(texture, spriteVertices, offset, usable);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation) {
        if (graphics == null || region == null || width == 0f || height == 0f) return;

        tmpColor.abgr8888(colorPacked);
        if (tmpColor.a <= 0f) return;

        if (submitRegion(region, x, y, originX, originY, width, height, rotation)) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y);
        if (rotation != 0f) {
            pose.translate(originX, originY);
            pose.rotate((float) Math.toRadians(rotation));
            pose.translate(-originX, -originY);
        }

        Identifier id = null;
        if (region.texture instanceof MinecraftTexture mcTexture && !mcTexture.isWhiteTexture()) {
            id = mcTexture.identifier();
        }

        if (id == null && rotation == 0f) {
            pose.popMatrix();
            fillRect(x, y, width, height, tmpColor);
            return;
        } else if (id == null) {
            pose.translate(0f, height);
            pose.scale(1f, -1f);
            graphics.fill(0, 0, Math.round(width), Math.round(height), toArgb(tmpColor));
        } else if (rotation == 0f) {
            pose.popMatrix();
            drawTexture(id, region.texture.width, region.texture.height, x, y, width, height, region.getX(), region.getY(),
                    region.width, region.height, tmpColor);
            return;
        } else {
            pose.translate(0f, height);
            pose.scale(1f, -1f);
            graphics.blit(ArcRenderPipelines.GUI_TEXTURED_NO_CULL, id, 0, 0, region.getX(), region.getY(), Math.round(width), Math.round(height),
                    region.width, region.height, region.texture.width, region.texture.height, toArgb(tmpColor));
        }

        pose.popMatrix();
    }

    private void submit(Texture texture, float[] vertices, int offset, int count) {
        MinecraftTextureStore store = textureStore();
        if (store == null) return;
        MinecraftTextureStore.TextureBinding binding = store.binding(texture);
        if (binding == null) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        Matrix3x2f snapshot = new Matrix3x2f(pose);
        pose.popMatrix();

        for (int index = offset; index + 23 < offset + count; index += 24) {
            ((GuiGraphicsAccess) graphics).arclib$renderState().submitGuiElement(
                    new QuadRenderState(binding.setup(), snapshot,
                            vertices, index, ArcRuntime.graphics().currentScissor()));
        }
    }

    private boolean submitRegion(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation) {
        MinecraftTextureStore store = textureStore();
        if (store == null) return false;
        MinecraftTextureStore.TextureBinding binding = store.binding(region.texture);
        if (binding == null) return false;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y);
        if (rotation != 0f) {
            pose.translate(originX, originY);
            pose.rotate((float) Math.toRadians(rotation));
            pose.translate(-originX, -originY);
        }
        pose.translate(0f, height);
        pose.scale(1f, -1f);
        Matrix3x2f snapshot = new Matrix3x2f(pose);
        pose.popMatrix();
        ((GuiGraphicsAccess) graphics).arclib$renderState().submitGuiElement(new BlitRenderState(
                ArcRenderPipelines.GUI_TEXTURED_NO_CULL,
                binding.setup(),
                snapshot,
                0, 0,
                width < 0f ? -Math.max(1, Math.round(-width)) : Math.max(1, Math.round(width)),
                height < 0f ? -Math.max(1, Math.round(-height)) : Math.max(1, Math.round(height)),
                region.u, region.u2, region.v, region.v2,
                packedToArgb(colorPacked),
                ArcRuntime.graphics().currentScissor()
        ));
        return true;
    }

    private MinecraftTextureStore textureStore() {
        if (textures != null) return textures;
        MinecraftGraphics graphics = ArcRuntime.graphics();
        return graphics == null ? null : graphics.textureStore();
    }

    @Override
    protected void flush() {
    }
}
