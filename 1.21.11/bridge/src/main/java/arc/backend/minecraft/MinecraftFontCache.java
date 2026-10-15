package arc.backend.minecraft;

import arc.flabel.FGlyph;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.FontCache;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.GlyphLayout.GlyphRun;
import arc.struct.Seq;

/**
 * Font cache that delegates final text rendering to Minecraft while leaving Arc's FontCache unchanged.
 */
public class MinecraftFontCache extends FontCache {
    private static final Color tempColor = new Color();

    private final Seq<MinecraftTextRun> runs = new Seq<>();
    private final Color tint = new Color(Color.white);
    private int glyphCount;

    public MinecraftFontCache(Font font, boolean integer) {
        super(font, integer);
    }

    @Override
    public void addText(GlyphLayout layout, float x, float y) {
        super.addText(layout, x, y);
        float baselineY = y + getFont().getData().ascent;
        for (int i = 0; i < layout.runs.size; i++) {
            GlyphRun run = layout.runs.get(i);
            if (run.glyphs.isEmpty()) continue;

            boolean perGlyph = false;
            for (int j = 0; j < run.glyphs.size; j++) {
                if (run.glyphs.get(j) instanceof FGlyph) {
                    perGlyph = true;
                    break;
                }
            }

            if (perGlyph) {
                float glyphX = x + run.x;
                for (int j = 0; j < run.glyphs.size; j++) {
                    Font.Glyph glyph = run.glyphs.get(j);
                    glyphX += run.xAdvances.get(j);
                    // The layout already reserves the tab width; never send the control character to Minecraft.
                    if (glyph.id == '\t') {
                        glyphCount++;
                        continue;
                    }
                    MinecraftTextRun textRun = new MinecraftTextRun();
                    textRun.text = Character.toString((char) glyph.id);
                    textRun.x = glyphX + glyph.xoffset * getFont().getData().scaleX;
                    textRun.y = baselineY + run.y + glyph.yoffset * getFont().getData().scaleY;
                    // FLabel restores the shared font scale before rebuilding its cache.
                    textRun.scaleX = getFont().getData().scaleX;
                    textRun.scaleY = getFont().getData().scaleY;
                    Color glyphColor = glyph instanceof FGlyph fancy && fancy.color != null ? fancy.color : run.color;
                    textRun.color.set(glyphColor);
                    textRun.glyphStart = glyphCount;
                    textRun.glyphEnd = ++glyphCount;
                    addRun(textRun);
                }
            } else {
                StringBuilder builder = new StringBuilder(run.glyphs.size);
                float glyphX = x + run.x;
                float textX = 0f;
                int textStart = glyphCount;
                for (int j = 0; j < run.glyphs.size; j++) {
                    Font.Glyph glyph = run.glyphs.get(j);
                    glyphX += run.xAdvances.get(j);
                    if (glyph.id == '\t') {
                        if (!builder.isEmpty()) {
                            addTextRun(builder, run, textX, baselineY, textStart, glyphCount);
                            builder.setLength(0);
                        }
                    } else {
                        if (builder.isEmpty()) {
                            textX = glyphX;
                            textStart = glyphCount;
                        }
                        builder.append((char) glyph.id);
                    }
                    glyphCount++;
                }
                if (!builder.isEmpty()) addTextRun(builder, run, textX, baselineY, textStart, glyphCount);
            }
        }
        tint.set(Color.white);
    }

    private void addTextRun(StringBuilder builder, GlyphRun run, float x, float baselineY, int start, int end) {
        MinecraftTextRun textRun = new MinecraftTextRun();
        textRun.text = builder.toString();
        textRun.x = x;
        textRun.y = baselineY + run.y;
        textRun.scaleX = getFont().getData().scaleX;
        textRun.scaleY = getFont().getData().scaleY;
        textRun.color.set(run.color);
        textRun.glyphStart = start;
        textRun.glyphEnd = end;
        addRun(textRun);
    }

    private void addRun(MinecraftTextRun run) {
        if (usesIntegerPositions()) {
            run.x = Math.round(run.x);
            run.y = Math.round(run.y);
        }
        runs.add(run);
    }

    @Override
    public void translate(float xAmount, float yAmount) {
        if (xAmount == 0f && yAmount == 0f) return;
        if (usesIntegerPositions()) {
            xAmount = Math.round(xAmount);
            yAmount = Math.round(yAmount);
        }
        super.translate(xAmount, yAmount);
        for (MinecraftTextRun run : runs) {
            run.x += xAmount;
            run.y += yAmount;
        }
    }

    @Override
    public void tint(Color value) {
        super.tint(value);
        tint.set(value);
    }

    @Override
    public void setAlphas(float alpha) {
        super.setAlphas(alpha);
        for (MinecraftTextRun run : runs) run.color.a = alpha;
    }

    @Override
    public void setColors(float color) {
        super.setColors(color);
        tempColor.abgr8888(color);
        for (MinecraftTextRun run : runs) run.color.set(tempColor);
    }

    @Override
    public void setColors(float color, int start, int end) {
        super.setColors(color, start, end);
        tempColor.abgr8888(color);
        for (MinecraftTextRun run : runs) {
            if (run.glyphEnd > start && run.glyphStart < end) run.color.set(tempColor);
        }
    }

    @Override
    public void draw() {
        for (MinecraftTextRun run : runs) {
            tempColor.set(run.color).mul(tint);
            ArcRuntime.drawText(run.text, run.x, run.y, tempColor, run.scaleX, run.scaleY);
        }
    }

    @Override
    public void draw(int start, int end) {
        draw();
    }

    @Override
    public void clear() {
        super.clear();
        runs.clear();
        tint.set(Color.white);
        glyphCount = 0;
    }

    private static class MinecraftTextRun {
        final Color color = new Color(Color.white);
        String text;
        float x, y, scaleX, scaleY;
        int glyphStart, glyphEnd;
    }
}
