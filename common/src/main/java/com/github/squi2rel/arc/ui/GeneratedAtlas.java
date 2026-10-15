package com.github.squi2rel.arc.ui;

import arc.graphics.Color;
import arc.graphics.GL20;
import arc.graphics.Gl;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.TextureData;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.gl.PixmapTextureData;

/** CPU-built UI atlas, uploaded on first use after the render context is available. */
public class GeneratedAtlas extends TextureAtlas{
    private static final int size = 256;
    private final Pixmap pixels = new Pixmap(size, size);
    private final AtlasTexture texture = new AtlasTexture(pixels);
    private int nextX, nextY, rowHeight;

    public GeneratedAtlas(){
        pixels.fill(0);
        Pixmap whitePixels = new Pixmap(1, 1);
        whitePixels.fill(Color.white);
        white = pack("white", whitePixels);
        whitePixels.dispose();

        Pixmap errorPixels = new Pixmap(16, 16);
        for(int y = 0; y < 16; y++){
            for(int x = 0; x < 16; x++){
                errorPixels.setRaw(x, y, ((x / 8 + y / 8) & 1) == 0 ? 0x000000ff : 0xff00ffff);
            }
        }
        error = pack("error", errorPixels);
        errorPixels.dispose();

        Pixmap circle = new Pixmap(128, 128);
        for(int y = 0; y < circle.height; y++){
            for(int x = 0; x < circle.width; x++){
                double dx = x + 0.5 - 64, dy = y + 0.5 - 64;
                int alpha = (int)Math.round(255 * Math.max(0, Math.min(1, 64 - Math.sqrt(dx * dx + dy * dy) + 0.5)));
                circle.setRaw(x, y, 0xffffff00 | alpha);
            }
        }
        pack("circle", circle);
        circle.dispose();
    }

    /** Packs a region with a one-pixel extruded border. The caller retains its pixmap. */
    public AtlasRegion pack(String name, Pixmap source){
        if(texture.isDisposed()) throw new IllegalStateException("Atlas is disposed");
        if(has(name)) return find(name);
        int width = source.width + 2, height = source.height + 2;
        if(width > size || height > size) throw new IllegalArgumentException("Region exceeds UI atlas size: " + name);
        int x = nextX, y = nextY, row = rowHeight;
        if(x + width > size){
            x = 0;
            y += row;
            row = 0;
        }
        if(y + height > size) throw new IllegalStateException("UI atlas is full: " + name);
        for(int dy = -1; dy <= source.height; dy++){
            for(int dx = -1; dx <= source.width; dx++){
                pixels.setRaw(x + dx + 1, y + dy + 1, source.getClamp(dx, dy));
            }
        }
        nextX = x + width;
        nextY = y;
        rowHeight = Math.max(row, height);
        texture.dirty = true;
        return addRegion(name, texture, x + 1, y + 1, source.width, source.height);
    }

    /** Transfers ownership of a pixmap to a texture that uploads on first use. */
    static Texture deferredTexture(Pixmap pixels){
        return new AtlasTexture(pixels);
    }

    private static final class AtlasTexture extends Texture{
        private final Pixmap pixels;
        private TextureData source;
        private boolean dirty, disposed;

        AtlasTexture(Pixmap pixels){
            // Texture's constructor calls our load(), which keeps initialization on the CPU.
            super(GL20.GL_TEXTURE_2D, 0, new PixmapTextureData(pixels, false, false));
            this.pixels = pixels;
        }

        @Override
        public void load(TextureData data){
            source = data;
            width = data.getWidth();
            height = data.getHeight();
            dirty = true;
        }

        private void upload(){
            if(disposed) throw new IllegalStateException("Atlas is disposed");
            if(!dirty) return;
            if(glHandle == 0) glHandle = Gl.genTexture();
            dirty = false;
            try{
                super.load(source);
            }catch(RuntimeException error){
                dirty = true;
                throw error;
            }
        }

        @Override public int getTextureObjectHandle(){ upload(); return super.getTextureObjectHandle(); }
        @Override public void bind(){ upload(); super.bind(); }
        @Override public void bind(int unit){ upload(); super.bind(unit); }
        @Override public TextureData getTextureData(){ return source; }
        @Override public boolean isDisposed(){ return disposed; }

        @Override
        public void dispose(){
            if(disposed) return;
            disposed = true;
            super.dispose();
            pixels.dispose();
        }
    }
}
