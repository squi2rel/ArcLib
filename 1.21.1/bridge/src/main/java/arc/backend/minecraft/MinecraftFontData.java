package arc.backend.minecraft;

import arc.graphics.g2d.Font;
import arc.scene.ui.layout.Scl;
import arc.struct.IntMap;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MinecraftFontData extends Font.FontData {
    private static final float lineHeightValue = 18f;
    private static final Set<MinecraftFontData> instances = Collections.newSetFromMap(new WeakHashMap<>());
    private static long revision;
    private final IntMap<Font.Glyph> cachedGlyphs = new IntMap<>();
    private long cachedRevision = -1;
    private float uiScale;

    public MinecraftFontData() {
        instances.add(this);
        uiScale = Scl.scl();
        lineHeight = Scl.scl(lineHeightValue);
        capHeight = lineHeight;
        xHeight = Scl.scl(14f);
        ascent = 0f;
        descent = 0f;
        down = -lineHeight;
        spaceXadvance = Scl.scl(8f);
        flipped = false;
        markupEnabled = true;
    }

    public static void invalidateFonts() {
        revision++;
    }

    public static void rescaleFonts() {
        float nextScale = Scl.scl();
        for (MinecraftFontData data : instances) {
            float ratio = nextScale / data.uiScale;
            data.lineHeight *= ratio;
            data.capHeight *= ratio;
            data.xHeight *= ratio;
            data.ascent *= ratio;
            data.descent *= ratio;
            data.down *= ratio;
            data.spaceXadvance *= ratio;
            data.padLeft *= ratio;
            data.padRight *= ratio;
            data.padTop *= ratio;
            data.padBottom *= ratio;
            data.uiScale = nextScale;
        }
    }

    @Override
    public boolean hasGlyph(char ch) {
        return true;
    }

    @Override
    public Font.Glyph getGlyph(char ch) {
        if (cachedRevision != revision) {
            cachedGlyphs.clear();
            spaceXadvance = measureAdvance(' ') * scaleX;
            cachedRevision = revision;
        }
        return cachedGlyphs.get(ch, () -> {
            MinecraftGlyph glyph = new MinecraftGlyph();
            glyph.id = ch;
            glyph.advance = measureAdvance(ch);
            glyph.width = (int) Math.ceil(glyph.advance);
            glyph.height = Math.max(1, Math.round(lineHeight / Math.max(scaleY, 0.001f)));
            glyph.xadvance = Math.round(glyph.advance);
            // Minecraft positions whole text runs by advances, including the final character.
            glyph.fixedWidth = true;
            glyph.xoffset = 0;
            glyph.yoffset = 0;
            glyph.page = 0;
            glyph.u = 0f;
            glyph.v = 0f;
            glyph.u2 = 1f;
            glyph.v2 = 1f;
            return glyph;
        });
    }

    @Override
    public Font.Glyph getFirstGlyph() {
        return getGlyph('M');
    }

    @Override
    public float getGlyphAdvance(Font.Glyph glyph) {
        return glyph instanceof MinecraftGlyph nativeGlyph ? nativeGlyph.advance : super.getGlyphAdvance(glyph);
    }

    @Override
    public float getGlyphWidth(Font.Glyph glyph) {
        return getGlyphAdvance(glyph);
    }

    protected float measureAdvance(char ch) {
        // Minecraft renders tab as a control-character placeholder, so reserve four spaces instead.
        if (ch == '\t') return ArcRuntime.textAdvance("    ") * MinecraftFont.scaled();
        return ArcRuntime.textAdvance(Character.toString(ch)) * MinecraftFont.scaled();
    }

    private static class MinecraftGlyph extends Font.Glyph {
        float advance;
    }
}
