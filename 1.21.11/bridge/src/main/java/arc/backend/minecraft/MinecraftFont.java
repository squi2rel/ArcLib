package arc.backend.minecraft;

import arc.graphics.g2d.Font;
import arc.graphics.g2d.FontCache;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Scl;

public class MinecraftFont extends Font{
    public static final float scale = 2f;

    public static float scaled(){
        return Scl.scl(scale);
    }

    public MinecraftFont(){
        super(new MinecraftFontData(), new TextureRegion(MinecraftTexture.white()), true);
    }

    public static Font create(){
        return new MinecraftFont();
    }

    @Override
    public FontCache newFontCache(){
        return new MinecraftFontCache(this, usesIntegerPositions());
    }
}
