package com.github.squi2rel.arc.ui;

import arc.Core;
import arc.Application;
import arc.Graphics;
import arc.graphics.Color;
import arc.graphics.GL20;
import arc.graphics.Gl;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Batch;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockGL20;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.style.NinePatchDrawable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.style.TransformDrawable;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField.TextFieldStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedAtlasTest{
    private final GL20 previousGl = Core.gl, previousGl20 = Core.gl20;
    private final Application previousApp = Core.app;
    private final Graphics previousGraphics = Core.graphics;
    private final TextureAtlas previousAtlas = Core.atlas;
    private final Batch previousBatch = Core.batch;
    private GeneratedAtlas atlas;

    @BeforeEach
    void setup(){
        Core.gl = Core.gl20 = null;
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Gl.reset();
        Core.atlas = atlas = new GeneratedAtlas();
    }

    @AfterEach
    void restore(){
        atlas.dispose();
        Core.gl = previousGl;
        Core.gl20 = previousGl20;
        Core.atlas = previousAtlas;
        Core.app = previousApp;
        Core.graphics = previousGraphics;
        Core.batch = previousBatch;
        Gl.reset();
    }

    @Test
    void builtinsAreGeneratedWithoutGlAndShareOnePixmap(){
        assertEquals(1, atlas.getTextures().size);
        assertSame(atlas.white().texture, atlas.find("circle").texture);
        assertSame(atlas.white().texture, atlas.find("error").texture);
        assertFalse(atlas.texture().isDisposed());
        assertSame(atlas.getPixmap("white").pixmap, atlas.getPixmap("circle").pixmap);
        assertEquals(Color.white.rgba(), atlas.getPixmap("white").get(0, 0));
        var circle = atlas.getPixmap("circle");
        assertEquals(255, circle.getA(64, 64));
        assertEquals(0, circle.getA(0, 0));
        boolean antialiased = false;
        for(int y = 0; y < circle.height; y++){
            for(int x = 0; x < circle.width; x++){
                assertEquals(0xffffff, circle.get(x, y) >>> 8);
                int alpha = circle.getA(x, y);
                antialiased |= alpha > 0 && alpha < 255;
            }
        }
        assertTrue(antialiased);
        assertNotEquals(atlas.getPixmap("error").get(0, 0), atlas.getPixmap("error").get(8, 0));
    }

    @Test
    void stylesUseTheAtlasAndPreserveSelectionColors(){
        Font font = new Font(new Font.FontData(), new TextureRegion(atlas.white()), true);
        ArcStyles.loadOnce(font);
        assertEquals(1, atlas.getTextures().size);
        for(var region : atlas.getRegions()) assertSame(atlas.texture(), region.texture);
        assertSame(atlas.texture(), ((NinePatchDrawable)ArcStyles.button.up).getPatch().getTexture());
        assertSame(atlas.texture(), ((NinePatchDrawable)ArcStyles.slider.knob).getPatch().getTexture());
        for(Drawable drawable : new Drawable[]{
            ArcStyles.selection, ArcStyles.overlay, ArcStyles.translucentPanel,
            ArcStyles.toolTextButton.up, ArcStyles.toolTextButton.over,
            ArcStyles.scrollPane.vScroll, ArcStyles.scrollPane.hScroll,
            ArcStyles.scrollPane.vScrollKnob, ArcStyles.scrollPane.hScrollKnob,
            ArcStyles.textField.cursor
        }){
            TextureRegion region = assertInstanceOf(TextureRegionDrawable.class, drawable).getRegion();
            assertSame(atlas.texture(), region.texture);
            assertNull(((TextureAtlas.AtlasRegion)region).splits);
        }
        for(Drawable drawable : new Drawable[]{
            ArcStyles.slider.background,
            ArcStyles.checkBox.checkboxOff, ArcStyles.checkBox.checkboxOver, ArcStyles.checkBox.checkboxOffDisabled,
            ArcStyles.checkBox.checkboxOn, ArcStyles.checkBox.checkboxOnOver, ArcStyles.checkBox.checkboxOnDisabled
        }) assertSame(atlas.texture(), assertInstanceOf(NinePatchDrawable.class, drawable).getPatch().getTexture());
        TextureRegion selection = ((TextureRegionDrawable)ArcStyles.selection).getRegion();
        assertEquals(Color.valueOf("426642").rgba(), atlas.getPixmap(selection).get(0, 0));
        TextureRegion overlay = ((TextureRegionDrawable)ArcStyles.overlay).getRegion();
        assertEquals(Color.valueOf("000000cc").rgba(), atlas.getPixmap(overlay).get(0, 0));
        assertCheckBoxPixels(ArcStyles.checkBox.checkboxOff, Color.valueOf("181818"), Color.gray, null);
        assertCheckBoxPixels(ArcStyles.checkBox.checkboxOver, Color.valueOf("303030"), Color.white, null);
        assertCheckBoxPixels(ArcStyles.checkBox.checkboxOffDisabled, Color.valueOf("181818"), Color.darkGray, null);
        assertCheckBoxPixels(ArcStyles.checkBox.checkboxOn, Color.valueOf("181818"), Color.gray, ArcStyles.accent);
        assertCheckBoxPixels(ArcStyles.checkBox.checkboxOnOver, Color.valueOf("181818"), Color.white, ArcStyles.accent);
        assertCheckBoxPixels(ArcStyles.checkBox.checkboxOnDisabled, Color.valueOf("181818"), Color.darkGray, Color.gray);
        assertEquals(8f, ArcStyles.button.up.getLeftWidth());
        assertEquals(12f, ArcStyles.slider.knob.getMinWidth());
        assertEquals(18f, ArcStyles.checkBox.checkboxOn.getMinWidth());
        assertSelectionColors();
    }

    private void assertCheckBoxPixels(Drawable drawable, Color fill, Color border, Color checked){
        assertInstanceOf(NinePatchDrawable.class, drawable);
        TextureRegion region = atlas.find("ui-check-" + fill + "-" + border + "-" + checked);
        assertNotNull(region);
        assertArrayEquals(new int[]{4, 4, 4, 4}, ((TextureAtlas.AtlasRegion)region).splits);
        assertEquals(18, region.width);
        assertEquals(18, region.height);
        var pixels = atlas.getPixmap(region);
        for(int y = 0; y < 18; y++){
            for(int x = 0; x < 18; x++){
                Color expected = x == 0 || y == 0 || x == 17 || y == 17 ? border
                    : checked != null && x >= 4 && y >= 4 && x < 14 && y < 14 ? checked : fill;
                assertEquals(expected.rgba(), pixels.get(x, y));
            }
        }
    }

    private void assertSelectionColors(){
        SelectionBatch batch = new SelectionBatch();
        Core.batch = batch;
        Font.FontData data = new Font.FontData();
        data.lineHeight = 16;
        data.down = -16;
        data.capHeight = 10;
        for(char character : new char[]{'a', ' '}){
            Font.Glyph glyph = new Font.Glyph();
            glyph.id = character;
            glyph.width = glyph.xadvance = 6;
            glyph.height = 10;
            glyph.fixedWidth = true;
            data.setGlyph(character, glyph);
        }
        TextFieldStyle style = new TextFieldStyle(ArcStyles.textField);
        style.font = new Font(data, atlas.white(), true);
        SelectionArea area = new SelectionArea("aa\naa\naa", style);
        area.setSize(200, 100);
        area.selectAll();
        for(String parentColor : new String[]{"ffffffff", "a0c0e080"}){
            batch.quads.clear();
            Draw.color(Color.valueOf(parentColor));
            float original = Draw.getColorPacked();
            // Selection color is in the atlas pixels; vertex colors only carry the caller's tint.
            float expected = original;

            area.drawSelectedLines();

            assertEquals(3, batch.quads.size());
            for(int line = 0; line < batch.quads.size(); line++){
                SelectionQuad quad = batch.quads.get(line);
                assertEquals(12f, quad.width);
                assertEquals(16f, quad.height);
                assertEquals(batch.quads.get(0).y - line * 16f, quad.y);
                assertEquals(expected, quad.color, "Selection tint changed on line " + (line + 1));
            }
            assertEquals(original, Draw.getColorPacked(), "Selection changed the color for subsequent draws");

            batch.quads.clear();
            for(int line = 0; line < 3; line++){
                ((TransformDrawable)ArcStyles.selection).draw(0, line * 16, 0, 0, 12, 16, 2, 2, 30);
            }
            assertEquals(3, batch.quads.size());
            for(SelectionQuad quad : batch.quads) assertEquals(expected, quad.color);
            assertEquals(original, Draw.getColorPacked());
        }
    }

    @Test
    void uploadsOnceAndRepacksWithoutChangingTextureOrUvs(){
        RecordingGl gl = new RecordingGl();
        Core.gl = Core.gl20 = gl;
        Texture texture = atlas.texture();
        var circle = atlas.find("circle");
        float u = circle.u, v = circle.v;
        int handle = texture.getTextureObjectHandle();
        assertEquals(1, gl.generated);
        assertEquals(1, gl.uploads);
        int center = ((circle.getY() + 64) * texture.width + circle.getX() + 64) * 4;
        assertEquals(255, gl.uploaded[center + 3] & 255);
        texture.bind();
        assertEquals(handle, texture.getTextureObjectHandle());
        assertEquals(1, gl.uploads);

        Pixmap extra = new Pixmap(3, 3);
        extra.fill(Color.red);
        var packed = atlas.pack("extra", extra);
        extra.dispose();
        assertSame(texture, packed.texture);
        assertEquals(handle, texture.getTextureObjectHandle());
        assertEquals(1, gl.generated);
        assertEquals(2, gl.uploads);
        assertEquals(u, circle.u);
        assertEquals(v, circle.v);
        int offset = (packed.getY() * texture.width + packed.getX()) * 4;
        assertEquals(255, gl.uploaded[offset] & 255);
        assertEquals(0, gl.uploaded[offset + 1] & 255);

        atlas.dispose();
        atlas.dispose();
        assertTrue(texture.isDisposed());
        assertEquals(0, atlas.getTextures().size);
        assertEquals(0, atlas.getPixmaps().size);
        assertEquals(1, gl.deleted);
        assertThrows(IllegalStateException.class, texture::getTextureObjectHandle);
    }

    @Test
    void unusedAtlasCanBeDisposedWithoutGl(){
        Texture texture = atlas.texture();
        atlas.dispose();
        assertTrue(texture.isDisposed());
        assertEquals(0, atlas.getTextures().size);
    }

    @Test
    void fallbackTextureExposesItsRegionBeforeUploading(){
        Pixmap pixels = new Pixmap(3, 3);
        pixels.fill(Color.red);
        Texture texture = GeneratedAtlas.deferredTexture(pixels);
        TextureRegion region = new TextureRegion(texture);
        assertEquals(3, region.width);
        assertEquals(3, region.height);
        assertSame(pixels, texture.getTextureData().getPixmap());

        RecordingGl gl = new RecordingGl();
        Core.gl = Core.gl20 = gl;
        texture.bind();
        texture.bind();
        assertEquals(1, gl.generated);
        assertEquals(1, gl.uploads);
        texture.dispose();
        texture.dispose();
        assertTrue(texture.isDisposed());
        assertEquals(1, gl.deleted);
    }

    private static class SelectionArea extends TextArea{
        SelectionArea(String text, TextFieldStyle style){ super(text, style); }

        void drawSelectedLines(){
            calculateOffsets();
            drawSelection(style.selection, style.font, 0, 100);
        }
    }

    private static class SelectionBatch extends Batch{
        final List<SelectionQuad> quads = new ArrayList<>();

        @Override
        protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
            quads.add(new SelectionQuad(y, width, height, colorPacked));
        }

        @Override
        protected void draw(Texture texture, float[] vertices, int offset, int count){
            for(int i = offset; i < offset + count; i += 24){
                float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
                float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
                for(int vertex = i; vertex < i + 24; vertex += 6){
                    minX = Math.min(minX, vertices[vertex]);
                    minY = Math.min(minY, vertices[vertex + 1]);
                    maxX = Math.max(maxX, vertices[vertex]);
                    maxY = Math.max(maxY, vertices[vertex + 1]);
                    assertEquals(vertices[i + 2], vertices[vertex + 2]);
                }
                quads.add(new SelectionQuad(minY, maxX - minX, maxY - minY, vertices[i + 2]));
            }
        }

        @Override protected void flush(){}
    }

    private record SelectionQuad(float y, float width, float height, float color){}

    private static final class RecordingGl extends MockGL20{
        int generated, uploads, deleted;
        byte[] uploaded;

        @Override public int glGenTexture(){ return ++generated; }
        @Override public void glDeleteTexture(int texture){ deleted++; }

        @Override
        public void glTexImage2D(int target, int level, int internalformat, int width, int height,
                                 int border, int format, int type, Buffer pixels){
            assertEquals(GL20.GL_TEXTURE_2D, target);
            assertEquals(GL20.GL_RGBA, format);
            assertEquals(GL20.GL_UNSIGNED_BYTE, type);
            assertTrue(((ByteBuffer)pixels).isDirect());
            uploaded = new byte[width * height * 4];
            ((ByteBuffer)pixels).duplicate().get(uploaded);
            uploads++;
        }
    }
}
