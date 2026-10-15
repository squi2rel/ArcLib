package arc.backend.minecraft;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinecraftBatchTest{
    private final List<String> events = new ArrayList<>();
    private final List<Submission> submissions = new ArrayList<>();
    private final PoseStack pose = new PoseStack();
    private final GuiGraphics graphics = mock(GuiGraphics.class);
    private final MinecraftTextureStore textures = mock(MinecraftTextureStore.class);
    private final Texture first = new TestTexture(), second = new TestTexture();
    private final RecordingBatch batch = new RecordingBatch();

    private class RecordingBatch extends MinecraftBatch{
        RecordingBatch(){ super(textures); }
        @Override protected void setTransform(Mat value){ super.setTransform(value); }
        @Override protected void setBlending(Blending value){ super.setBlending(value); }
        @Override protected void setPackedColor(float value){ super.setPackedColor(value); }
        @Override protected void discard(){ super.discard(); }
        @Override
        void drawRaw(int textureId, float[] vertices, int offset, int count, Matrix4f matrix){
            events.add("arc:" + textureId);
            submissions.add(new Submission(textureId, Arrays.copyOfRange(vertices, offset, offset + count), new Matrix4f(matrix)));
        }
    }

    @BeforeEach
    void setup(){
        when(graphics.pose()).thenReturn(pose);
        when(textures.binding(first)).thenReturn(new MinecraftTextureStore.TextureBinding(11));
        when(textures.binding(second)).thenReturn(new MinecraftTextureStore.TextureBinding(22));
        doAnswer(call -> { events.add("vanilla"); return null; }).when(graphics).flush();
        batch.begin(graphics);
        events.clear();
    }

    @Test
    void coalescesQuadsAndCopiesReusedInput(){
        float[] quad = quad(1f);
        batch.draw(first, quad, 0, quad.length);
        quad[0] = 2f;
        batch.draw(first, quad, 0, quad.length);
        Arrays.fill(quad, -1f);
        assertTrue(submissions.isEmpty());

        batch.end();
        assertEquals(1, submissions.size());
        assertEquals(48, submissions.get(0).vertices.length);
        assertEquals(1f, submissions.get(0).vertices[0]);
        assertEquals(2f, submissions.get(0).vertices[24]);
        assertEquals(List.of("vanilla", "arc:11"), events);
    }

    @Test
    void preservesTextureOrderInsteadOfSorting(){
        draw(first);
        draw(second);
        draw(first);
        batch.end();
        assertEquals(List.of(11, 22, 11), submissions.stream().map(Submission::texture).toList());
    }

    @Test
    void splitsOversizedInputAtQuadBoundariesWithoutLosingVertices(){
        float[] source = new float[(1024 * 2 + 7) * 24 + 24];
        for(int i = 0; i < source.length; i++) source[i] = i;
        batch.draw(first, source, 24, source.length - 24);
        batch.end();
        assertEquals(List.of(1024 * 24, 1024 * 24, 7 * 24), submissions.stream().map(s -> s.vertices.length).toList());
        int cursor = 24;
        for(Submission submission : submissions){
            for(float vertex : submission.vertices) assertEquals(source[cursor++], vertex);
        }
        assertEquals(source.length, cursor);
    }

    @Test
    void snapshotsGuiPoseBeforeItIsChangedOrPopped(){
        pose.pushPose();
        pose.translate(10f, 20f, 0f);
        draw(first);
        pose.translate(30f, 0f, 0f);
        draw(first);
        pose.popPose();
        batch.end();
        assertEquals(2, submissions.size());
        assertEquals(10f, submissions.get(0).matrix.m30());
        assertEquals(40f, submissions.get(1).matrix.m30());
        assertEquals(20f, submissions.get(1).matrix.m31());
    }

    @Test
    void flushesTransformAndBlendChanges(){
        draw(first);
        batch.setTransform(new Mat().setToTranslation(15f, 25f));
        draw(first);
        batch.setBlending(Blending.additive);
        draw(first);
        batch.end();
        assertEquals(3, submissions.size());
        assertEquals(0f, submissions.get(0).matrix.m30());
        assertEquals(15f, submissions.get(1).matrix.m30());
        assertEquals(25f, submissions.get(2).matrix.m31());
    }

    @Test
    void regionDrawsShareABatchAndRetainTheirVertexColors(){
        TextureRegion region = new TextureRegion(first);
        batch.setPackedColor(Color.red.toFloatBits());
        batch.draw(region, 1f, 2f, 0f, 0f, 10f, 20f, 0f);
        batch.setPackedColor(Color.blue.toFloatBits());
        batch.draw(region, 3f, 4f, 0f, 0f, 10f, 20f, 0f);
        batch.end();
        assertEquals(1, submissions.size());
        float[] vertices = submissions.get(0).vertices;
        assertEquals(48, vertices.length);
        assertEquals(1f, vertices[0]);
        assertEquals(3f, vertices[24]);
        assertEquals(Color.red.toFloatBits(), vertices[2]);
        assertEquals(Color.blue.toFloatBits(), vertices[26]);
    }

    @Test
    void vanillaAccessAndCallbacksFlushInPainterOrder(){
        draw(first);
        assertSame(graphics, batch.graphics());
        events.add("widget");
        draw(first);
        batch.draw(() -> {
            events.add("callback");
            draw(second);
        });
        draw(first);
        batch.end();
        assertEquals(List.of("vanilla", "arc:11", "widget", "vanilla", "arc:11", "callback",
        "vanilla", "arc:22", "vanilla", "arc:11"), events);
    }

    @Test
    void discardDoesNotLeakPendingVerticesIntoNextFrame(){
        draw(first);
        batch.discard();
        batch.end();
        assertTrue(submissions.isEmpty());
        batch.begin(graphics);
        draw(second);
        batch.end();
        assertEquals(1, submissions.size());
        assertEquals(22, submissions.get(0).texture);
        assertEquals(24, submissions.get(0).vertices.length);
    }

    @Test
    void textureMutationAndScissorChangesFlushPendingGeometry(){
        MinecraftGraphics backend = mock(MinecraftGraphics.class);
        MinecraftGL20 gl = new MinecraftGL20(backend, textures);
        try(var runtime = mockStatic(ArcRuntime.class)){
            runtime.when(ArcRuntime::batch).thenReturn(batch);
            draw(first);
            gl.glScissor(0, 0, 10, 10);
            assertEquals(1, submissions.size());
            draw(first);
            gl.glDisable(arc.graphics.GL20.GL_SCISSOR_TEST);
            assertEquals(2, submissions.size());
            draw(first);
            gl.glTexSubImage2D(0, 0, 0, 0, 1, 1, 0, 0, null);
            assertEquals(3, submissions.size());
            draw(first);
            gl.glDeleteTexture(1);
            assertEquals(4, submissions.size());
        }
    }

    private void draw(Texture texture){
        float[] vertices = quad(0f);
        batch.draw(texture, vertices, 0, vertices.length);
    }

    private static float[] quad(float x){
        float[] vertices = new float[24];
        vertices[0] = x;
        for(int i = 2; i < vertices.length; i += 6) vertices[i] = Color.whiteFloatBits;
        return vertices;
    }

    private record Submission(int texture, float[] vertices, Matrix4f matrix){}

    private static class TestTexture extends Texture{
        TestTexture(){ width = height = 16; }
    }
}
