package arc.backend.minecraft;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Batch;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public class MinecraftBatch extends Batch{
    private static final Color tmpColor = new Color();

    private final MinecraftTextureStore textures;
    private GuiGraphicsExtractor graphics;

    public MinecraftBatch(){
        this(null);
    }

    MinecraftBatch(MinecraftTextureStore textures){
        this.textures = textures;
    }

    public void begin(GuiGraphicsExtractor graphics){
        this.graphics = graphics;
    }

    public void end(){
        this.graphics = null;
    }

    public GuiGraphicsExtractor graphics(){
        return graphics;
    }

    public void fillRect(float x, float y, float width, float height, Color color){
        fillRect(x, y, width, height, color, true);
    }

    private void fillRect(float x, float y, float width, float height, Color color, boolean applyBatchTransform){
        if(graphics == null || color.a <= 0f || width <= 0f || height <= 0f) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        if(applyBatchTransform) applyTransform(pose, transformMatrix);
        pose.translate(x, y + height);
        pose.scale(1f, -1f);
        graphics.fill(0, 0, Math.round(width), Math.round(height), toArgb(color));
        pose.popMatrix();
    }

    public void drawTexture(Identifier id, int textureWidth, int textureHeight, float x, float y, float width, float height,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight, Color color){
        if(graphics == null || id == null || color.a <= 0f || width <= 0f || height <= 0f || sourceWidth <= 0 || sourceHeight <= 0) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y + height);
        pose.scale(1f, -1f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, sourceX, sourceY, Math.round(width), Math.round(height),
        sourceWidth, sourceHeight, textureWidth, textureHeight, toArgb(color));
        pose.popMatrix();
    }

    public void drawText(String text, float x, float y, Color color, float scaleX, float scaleY){
        if(graphics == null || text == null || text.isEmpty() || color.a <= 0f) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y);
        float fontScale = MinecraftFont.scaled();
        pose.scale(scaleX * fontScale, -scaleY * fontScale);
        graphics.text(ArcRuntime.minecraft().font, text, 0, 0, toArgb(color), false);
        pose.popMatrix();
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        if(graphics == null || count < 24) return;

        int usable = count - count % 24;
        submit(texture, spriteVertices, offset, usable);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        if(graphics == null || region == null || width <= 0f || height <= 0f) return;

        tmpColor.abgr8888(colorPacked);
        if(tmpColor.a <= 0f) return;

        if(submitRegion(region, x, y, originX, originY, width, height, rotation)) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y);
        if(rotation != 0f){
            pose.translate(originX, originY);
            pose.rotate((float)Math.toRadians(rotation));
            pose.translate(-originX, -originY);
        }

        Identifier id = null;
        if(region.texture instanceof MinecraftTexture mcTexture && !mcTexture.isWhiteTexture()){
            id = mcTexture.identifier();
        }

        if(id == null && rotation == 0f){
            pose.popMatrix();
            fillRect(x, y, width, height, tmpColor);
            return;
        }else if(id == null){
            pose.translate(0f, height);
            pose.scale(1f, -1f);
            graphics.fill(0, 0, Math.round(width), Math.round(height), toArgb(tmpColor));
        }else if(rotation == 0f){
            pose.popMatrix();
            drawTexture(id, region.texture.width, region.texture.height, x, y, width, height, region.getX(), region.getY(),
            Math.round(region.width), Math.round(region.height), tmpColor);
            return;
        }else{
            pose.translate(0f, height);
            pose.scale(1f, -1f);
            graphics.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, region.getX(), region.getY(), Math.round(width), Math.round(height),
            Math.round(region.width), Math.round(region.height), region.texture.width, region.texture.height, toArgb(tmpColor));
        }

        pose.popMatrix();
    }

    private boolean submit(Texture texture, float[] vertices, int offset, int count){
        MinecraftTextureStore store = textureStore();
        if(store == null) return false;
        MinecraftTextureStore.TextureBinding binding = store.binding(texture);
        if(binding == null) return false;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        Matrix3x2f snapshot = new Matrix3x2f(pose);
        pose.popMatrix();

        for(int index = offset; index + 23 < offset + count; index += 24){
            ((GuiGraphicsExtractorAccess)(Object)graphics).arclib$renderState().addGuiElement(
                new QuadRenderState(ArcRenderPipelines.GUI_TEXTURED_NO_CULL, binding.setup(), snapshot,
                    vertices, index, ArcRuntime.graphics().currentScissor()));
        }
        return true;
    }

    private boolean submitRegion(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        MinecraftTextureStore store = textureStore();
        if(store == null) return false;
        MinecraftTextureStore.TextureBinding binding = store.binding(region.texture);
        if(binding == null) return false;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y);
        if(rotation != 0f){
            pose.translate(originX, originY);
            pose.rotate((float)Math.toRadians(rotation));
            pose.translate(-originX, -originY);
        }
        pose.translate(0f, height);
        pose.scale(1f, -1f);
        Matrix3x2f snapshot = new Matrix3x2f(pose);
        pose.popMatrix();
        ((GuiGraphicsExtractorAccess)(Object)graphics).arclib$renderState().addGuiElement(new BlitRenderState(
        RenderPipelines.GUI_TEXTURED,
        binding.setup(),
        snapshot,
        0, 0, Math.max(1, Math.round(width)), Math.max(1, Math.round(height)),
        region.u, region.u2, region.v, region.v2,
        packedToArgb(colorPacked),
        ArcRuntime.graphics().currentScissor()
        ));
        return true;
    }

    void constructVertices(float[] vertices, TextureRegion region, float x, float y, float originX, float originY,
                           float width, float height, float rotation){
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float x1 = x, y1 = y;
        float x2 = x, y2 = y + height;
        float x3 = x + width, y3 = y + height;
        float x4 = x + width, y4 = y;

        if(rotation != 0f){
            float worldOriginX = x + originX;
            float worldOriginY = y + originY;
            float cos = (float)Math.cos(Math.toRadians(rotation));
            float sin = (float)Math.sin(Math.toRadians(rotation));
            x1 = rotateX(x - worldOriginX, y - worldOriginY, cos, sin) + worldOriginX;
            y1 = rotateY(x - worldOriginX, y - worldOriginY, cos, sin) + worldOriginY;
            x2 = rotateX(x - worldOriginX, y + height - worldOriginY, cos, sin) + worldOriginX;
            y2 = rotateY(x - worldOriginX, y + height - worldOriginY, cos, sin) + worldOriginY;
            x3 = rotateX(x + width - worldOriginX, y + height - worldOriginY, cos, sin) + worldOriginX;
            y3 = rotateY(x + width - worldOriginX, y + height - worldOriginY, cos, sin) + worldOriginY;
            x4 = rotateX(x + width - worldOriginX, y - worldOriginY, cos, sin) + worldOriginX;
            y4 = rotateY(x + width - worldOriginX, y - worldOriginY, cos, sin) + worldOriginY;
        }

        vertex(vertices, 0, x1, y1, u, v);
        vertex(vertices, 6, x2, y2, u, v2);
        vertex(vertices, 12, x3, y3, u2, v2);
        vertex(vertices, 18, x4, y4, u2, v);
    }

    private void vertex(float[] vertices, int index, float x, float y, float u, float v){
        vertices[index] = x;
        vertices[index + 1] = y;
        vertices[index + 2] = colorPacked;
        vertices[index + 3] = u;
        vertices[index + 4] = v;
        vertices[index + 5] = mixColorPacked;
    }

    private static float rotateX(float x, float y, float cos, float sin){
        return cos * x - sin * y;
    }

    private static float rotateY(float x, float y, float cos, float sin){
        return sin * x + cos * y;
    }

    static int packedToArgb(float packed){
        int abgr = Float.floatToRawIntBits(packed);
        int alpha = abgr >>> 24;
        if(alpha == 254) alpha = 255;
        return alpha << 24 | (abgr & 0xff) << 16 | abgr & 0xff00 | abgr >>> 16 & 0xff;
    }

    private MinecraftTextureStore textureStore(){
        if(textures != null) return textures;
        MinecraftGraphics graphics = ArcRuntime.graphics();
        return graphics == null ? null : graphics.textureStore();
    }

    @Override
    protected void flush(){
    }

    private static void applyTransform(Matrix3x2fStack pose, Mat mat){
        if(mat == null) return;
        pose.mul(new Matrix3x2f(
        mat.val[Mat.M00], mat.val[Mat.M10],
        mat.val[Mat.M01], mat.val[Mat.M11],
        mat.val[Mat.M02], mat.val[Mat.M12]
        ));
    }

    public static int toArgb(Color color){
        int a = Math.round(color.a * 255f) & 0xff;
        int r = Math.round(color.r * 255f) & 0xff;
        int g = Math.round(color.g * 255f) & 0xff;
        int b = Math.round(color.b * 255f) & 0xff;
        return a << 24 | r << 16 | g << 8 | b;
    }
}
