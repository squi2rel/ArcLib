package com.github.squi2rel.arc;

import arc.backend.minecraft.ArcRuntime;
import net.minecraft.client.Minecraft;

public final class ArcLibClient {
    private static boolean windowCreated;

    private ArcLibClient() {
    }

    public static void afterWindowCreated() {
        windowCreated = true;
        init();
    }

    public static void init() {
        if (!windowCreated) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (ArcRuntime.initialized() || ArcRuntime.canInitialize(minecraft)) return;
        ArcRuntime.init(minecraft);
        ArcLib.LOGGER.info("ArcLib Minecraft backend initialized");
    }
}
