package arc.backend.minecraft;

import com.github.squi2rel.arc.runtime.RuntimeApplication;
import net.minecraft.client.Minecraft;

public class MinecraftApplication extends RuntimeApplication {
    private final Minecraft minecraft;

    public MinecraftApplication(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.desktop;
    }

    @Override
    public String getClipboardText() {
        return minecraft.keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboardText(String text) {
        minecraft.keyboardHandler.setClipboard(text == null ? "" : text);
    }

    @Override
    public void post(Runnable runnable) {
        minecraft.execute(runnable);
    }

    @Override
    public void exit() {
        requestExit();
        minecraft.execute(minecraft::stop);
    }
}
