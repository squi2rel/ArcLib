package arc.backend.minecraft;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Batch;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Arrays;

public class MinecraftBatch extends Batch{
    private static final Color tmpColor = new Color();

    private final MinecraftTextureStore textures;
    private GuiGraphics graphics;
    // Keep complete quads together; Arc reuses its input arrays after every draw.
    private final float[] vertices = new float[1024 * 24];
    private final float[] regionVertices = new float[24];
    private final Matrix4f pendingPose = new Matrix4f();
    private final Mat pendingTransform = new Mat();
    private final Matrix4f pendingMatrix = new Matrix4f();
    private final Matrix4f transform = new Matrix4f();
    private int pendingTexture;

    public MinecraftBatch(){
        this(null);
    }

    MinecraftBatch(MinecraftTextureStore textures){
        this.textures = textures;
    }

    public void begin(GuiGraphics graphics){
        flush();
        this.graphics = graphics;
        flush();
    }

    public void end(){
        flush();
        this.graphics = null;
    }

    public GuiGraphics graphics(){
        // Handing the GUI to a vanilla widget is an ordering boundary.
        flush();
        return graphics;
    }

    public void fillRect(float x, float y, float width, float height, Color color){
        fillRect(x, y, width, height, color, true);
    }

    private void fillRect(float x, float y, float width, float height, Color color, boolean applyBatchTransform){
        if(graphics == null || color.a <= 0f || width <= 0f || height <= 0f) return;

        flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        if(applyBatchTransform) applyTransform(pose, transformMatrix);
        pose.translate(x, y + height, 0f);
        pose.scale(1f, -1f, 1f);
        graphics.fill(RenderType.guiOverlay(), 0, 0, Math.round(width), Math.round(height), toArgb(color));
        flush();
        pose.popPose();
    }

    public void drawTexture(ResourceLocation id, int textureWidth, int textureHeight, float x, float y, float width, float height,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight, Color color){
        if(graphics == null || id == null || color.a <= 0f || width <= 0f || height <= 0f || sourceWidth <= 0 || sourceHeight <= 0) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y + height, 0f);
        pose.scale(1f, -1f, 1f);
        beginImmediateDraw();
        try{
            graphics.setColor(color.r, color.g, color.b, color.a);
            graphics.blit(id, 0, 0, Math.round(width), Math.round(height), sourceX, sourceY,
            sourceWidth, sourceHeight, textureWidth, textureHeight);
        }finally{
            graphics.setColor(1f, 1f, 1f, 1f);
            endImmediateDraw();
        }
        pose.popPose();
    }

    public void drawText(String text, float x, float y, Color color, float scaleX, float scaleY){
        if(graphics == null || text == null || text.isEmpty() || color.a <= 0f) return;

        flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y, 0f);
        float fontScale = MinecraftFont.scaled();
        pose.scale(scaleX * fontScale, -scaleY * fontScale, 1f);
        Font font = ArcRuntime.minecraft().font;
        // Arc uses painter's order. Vanilla's NORMAL text writes depth and can
        // otherwise remain in front of panels drawn later in the scene.
        font.drawInBatch(text, 0f, 0f, toArgb(color), false, pose.last().pose(), graphics.bufferSource(),
        Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0, font.isBidirectional());
        flush();
        pose.popPose();
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

        flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        applyTransform(pose, transformMatrix);
        pose.translate(x, y, 0f);
        if(rotation != 0f){
            pose.translate(originX, originY, 0f);
            pose.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(rotation)));
            pose.translate(-originX, -originY, 0f);
        }

        ResourceLocation id = null;
        if(region.texture instanceof MinecraftTexture mcTexture && !mcTexture.isWhiteTexture()){
            id = mcTexture.identifier();
        }

        if(id == null && rotation == 0f){
            pose.popPose();
            fillRect(x, y, width, height, tmpColor);
            return;
        }else if(id == null){
            pose.translate(0f, height, 0f);
            pose.scale(1f, -1f, 1f);
            graphics.fill(RenderType.guiOverlay(), 0, 0, Math.round(width), Math.round(height), toArgb(tmpColor));
            flush();
        }else if(rotation == 0f){
            pose.popPose();
            drawTexture(id, region.texture.width, region.texture.height, x, y, width, height, region.getX(), region.getY(),
            Math.round(region.width), Math.round(region.height), tmpColor);
            return;
        }else{
            pose.translate(0f, height, 0f);
            pose.scale(1f, -1f, 1f);
            beginImmediateDraw();
            try{
                graphics.setColor(tmpColor.r, tmpColor.g, tmpColor.b, tmpColor.a);
                graphics.blit(id, 0, 0, Math.round(width), Math.round(height), region.getX(), region.getY(),
                Math.round(region.width), Math.round(region.height), region.texture.width, region.texture.height);
            }finally{
                graphics.setColor(1f, 1f, 1f, 1f);
                endImmediateDraw();
            }
        }

        pose.popPose();
    }

    private boolean submit(Texture texture, float[] vertices, int offset, int count){
        MinecraftTextureStore store = textureStore();
        if(store == null) return false;
        MinecraftTextureStore.TextureBinding binding = store.binding(texture);
        if(binding == null) return false;

        enqueue(binding.id(), vertices, offset, count);
        return true;
    }

    private boolean submitRegion(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        MinecraftTextureStore store = textureStore();
        if(store == null) return false;
        MinecraftTextureStore.TextureBinding binding = store.binding(region.texture);
        if(binding == null) return false;

        constructVertices(regionVertices, region, x, y, originX, originY, width, height, rotation);
        enqueue(binding.id(), regionVertices, 0, regionVertices.length);
        return true;
    }

    private void enqueue(int textureId, float[] source, int offset, int count){
        Matrix4f pose = graphics.pose().last().pose();
        if(idx > 0 && (pendingTexture != textureId || !pendingPose.equals(pose)
        || !Arrays.equals(pendingTransform.val, transformMatrix.val))){
            flush();
        }
        while(count > 0){
            if(idx == vertices.length) flush();
            if(idx == 0){
                pendingTexture = textureId;
                pendingPose.set(pose);
                pendingTransform.set(transformMatrix);
                // The GUI pose may be popped before end()/flush(), so retain the draw-time matrix.
                float[] m = pendingTransform.val;
                transform.set(
                m[Mat.M00], m[Mat.M10], 0f, 0f,
                m[Mat.M01], m[Mat.M11], 0f, 0f,
                0f, 0f, 1f, 0f,
                m[Mat.M02], m[Mat.M12], 0f, 1f);
                pendingMatrix.set(pendingPose).mul(transform);
            }
            int copied = Math.min(count, vertices.length - idx);
            System.arraycopy(source, offset, vertices, idx, copied);
            idx += copied;
            offset += copied;
            count -= copied;
        }
    }

    // Separate GPU submission from queuing so the ordering contract can be tested without a GL context.
    void drawRaw(int textureId, float[] vertices, int offset, int count, Matrix4f matrix){
        prepareImmediateDraw();
        try{
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, textureId);

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for(int index = offset; index + 5 < offset + count; index += 6){
                int argb = packedToArgb(vertices[index + 2]);
                buffer.addVertex(matrix, vertices[index], vertices[index + 1], 0f)
                .setUv(vertices[index + 3], vertices[index + 4])
                .setColor(argb);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }finally{
            endImmediateDraw();
        }
    }

    private void beginImmediateDraw(){
        flush();
        prepareImmediateDraw();
    }

    private void prepareImmediateDraw(){
        // GuiGraphics.flush() re-enables depth testing. Arc's Y-up quads also
        // have the opposite winding to vanilla GUI quads, so disable culling.
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void endImmediateDraw(){
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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
        if(graphics == null) return;
        graphics.flush();
        if(idx == 0) return;
        int count = idx;
        idx = 0;
        drawRaw(pendingTexture, vertices, 0, count, pendingMatrix);
    }

    @Override
    protected void draw(Runnable request){
        flush();
        try{
            request.run();
        }finally{
            flush();
        }
    }

    private static void applyTransform(PoseStack pose, Mat mat){
        if(mat == null) return;
        pose.mulPose(new Matrix4f(
        mat.val[Mat.M00], mat.val[Mat.M10], 0f, 0f,
        mat.val[Mat.M01], mat.val[Mat.M11], 0f, 0f,
        0f, 0f, 1f, 0f,
        mat.val[Mat.M02], mat.val[Mat.M12], 0f, 1f
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
