package com.github.squi2rel.arc.ui;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mat;
import arc.scene.style.BaseDrawable;
import arc.scene.style.TransformDrawable;
import arc.util.Align;

public class TextDrawable extends BaseDrawable implements TransformDrawable{
    private final String text;
    private final Mat previousTransform = new Mat();
    private final Mat transform = new Mat();
    private final GlyphLayout layout = new GlyphLayout();

    public TextDrawable(String text, float size){
        this.text = text;
        setMinWidth(size);
        setMinHeight(size);
    }

    @Override
    public void draw(float x, float y, float width, float height){
        if(width <= 0f || height <= 0f || ArcStyles.label == null) return;
        var font = ArcStyles.label.font;
        layout.setText(font, text);
        float lineHeight = Math.max(1f, font.getLineHeight());
        float scale = Math.min(1f, Math.min(width / Math.max(1f, layout.width), height / lineHeight));
        font.draw(text, x + width / 2f, y + (height + lineHeight * scale) / 2f,
        Draw.getColor(), scale, false, Align.center);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
        originX = width / 2f;
        originY = height / 2f;
        previousTransform.set(Draw.trans());
        transform.set(previousTransform).translate(x + originX, y + originY).rotate(rotation)
        .scale(scaleX, scaleY).translate(-originX, -originY);
        Draw.trans(transform);
        try{
            draw(0f, 0f, width, height);
        }finally{
            Draw.trans(previousTransform);
        }
    }
}
