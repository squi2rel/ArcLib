package arc.backend.minecraft;

import arc.graphics.Texture;
import net.minecraft.resources.ResourceLocation;

public class MinecraftTexture extends Texture {
    private static final MinecraftTexture white = new MinecraftTexture(null, 1, 1, true);

    private final ResourceLocation identifier;
    private final boolean whiteTexture;

    public MinecraftTexture(ResourceLocation identifier, int width, int height) {
        this(identifier, width, height, false);
    }

    private MinecraftTexture(ResourceLocation identifier, int width, int height, boolean whiteTexture) {
        super();
        this.identifier = identifier;
        this.whiteTexture = whiteTexture;
        this.width = width;
        this.height = height;
    }

    public static MinecraftTexture white() {
        return white;
    }

    public ResourceLocation identifier() {
        return identifier;
    }

    public boolean isWhiteTexture() {
        return whiteTexture;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void dispose() {
    }
}
