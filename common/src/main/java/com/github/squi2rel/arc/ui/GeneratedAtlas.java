package com.github.squi2rel.arc.ui;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.PixmapPacker;
import arc.graphics.g2d.TextureAtlas;

/**
 * Runtime UI atlas backed by PixmapPacker pages.
 * Creation and packing require an initialized GL context on the render thread.
 */
public class GeneratedAtlas extends TextureAtlas {
    private static final int pageSize = 2048, padding = 2;
    private final PixmapPacker packer = new PixmapPacker(pageSize, pageSize, padding, true);
    private boolean disposed;

    public GeneratedAtlas() {
        Pixmap whitePixels = new Pixmap(1, 1);
        whitePixels.fill(Color.white);
        white = pack("white", whitePixels);
        whitePixels.dispose();

        Pixmap errorPixels = new Pixmap(16, 16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                errorPixels.setRaw(x, y, ((x / 8 + y / 8) & 1) == 0 ? 0x000000ff : 0xff00ffff);
            }
        }
        error = pack("error", errorPixels);
        errorPixels.dispose();

        Pixmap circle = new Pixmap(128, 128);
        for (int y = 0; y < circle.height; y++) {
            for (int x = 0; x < circle.width; x++) {
                double dx = x + 0.5 - 64, dy = y + 0.5 - 64;
                int alpha = (int) Math.round(255 * Math.max(0, Math.min(1, 64 - Math.sqrt(dx * dx + dy * dy) + 0.5)));
                circle.setRaw(x, y, 0xffffff00 | alpha);
            }
        }
        pack("circle", circle);
        circle.dispose();
    }

    /**
     * Packs a region with a one-pixel extruded border, adding pages as needed.
     * The caller retains its pixmap; existing regions keep their textures and UVs.
     */
    public AtlasRegion pack(String name, Pixmap source) {
        if (disposed) throw new IllegalStateException("Atlas is disposed");
        if (has(name)) return find(name);
        // Guillotine packing reserves padding around the page and after each region.
        if (source.width + padding * 3 > pageSize || source.height + padding * 3 > pageSize)
            throw new IllegalArgumentException("Region exceeds UI atlas page size: " + name);

        var rect = packer.pack(source);
        // The default GuillotineStrategy always inserts into the last page.
        var page = packer.getPages().peek();
        page.updateTexture(TextureFilter.nearest, TextureFilter.nearest, false);
        return addRegion(name, page.texture, (int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        super.dispose();
        packer.dispose();
    }
}
