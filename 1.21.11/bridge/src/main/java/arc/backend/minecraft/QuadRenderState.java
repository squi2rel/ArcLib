package arc.backend.minecraft;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * Preserves Arc's four vertex positions, including skewed and degenerate quads.
 */
final class QuadRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final float[] vertices;
    private final ScreenRectangle scissorArea;
    private final ScreenRectangle bounds;

    QuadRenderState(TextureSetup textureSetup, Matrix3x2f pose,
                    float[] vertices, int offset, ScreenRectangle scissorArea) {
        this.pipeline = ArcRenderPipelines.GUI_TEXTURED_NO_CULL;
        this.textureSetup = textureSetup;
        this.pose = new Matrix3x2f(pose);
        this.vertices = Arrays.copyOfRange(vertices, offset, offset + 24);
        this.scissorArea = scissorArea;

        Vector2f point = new Vector2f();
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 24; i += 6) {
            this.pose.transformPosition(this.vertices[i], this.vertices[i + 1], point);
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        int left = (int) Math.floor(minX), top = (int) Math.floor(minY);
        ScreenRectangle area = new ScreenRectangle(left, top,
                (int) Math.ceil(maxX) - left, (int) Math.ceil(maxY) - top);
        bounds = scissorArea == null ? area : scissorArea.intersection(area);
    }

    @Override
    public void buildVertices(@NonNull VertexConsumer consumer) {
        for (int vertex = 0; vertex < 4; vertex++) {
            int i = vertex * 6;
            consumer.addVertexWith2DPose(pose, vertices[i], vertices[i + 1])
                    .setUv(vertices[i + 3], vertices[i + 4])
                    .setColor(MinecraftBatch.packedToArgb(vertices[i + 2]));
        }
    }

    @Override
    public @NonNull RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public @NonNull TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }
}
