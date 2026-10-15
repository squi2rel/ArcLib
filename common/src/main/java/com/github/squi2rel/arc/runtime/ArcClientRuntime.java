package com.github.squi2rel.arc.runtime;

import arc.Core;
import arc.scene.Scene;

/**
 * Minecraft-independent state shared by ArcLib's client backend and client-side consumers.
 */
public final class ArcClientRuntime {
    private static Scene scene;

    private ArcClientRuntime() {
    }

    public static void attach(Scene initialScene) {
        if (initialScene == null) throw new IllegalArgumentException("initialScene cannot be null");
        scene = initialScene;
        setActiveScene(initialScene);
    }

    public static void detach() {
        if (Core.scene == scene) Core.scene = null;
        scene = null;
    }

    public static Scene scene() {
        return Core.scene == null ? scene : Core.scene;
    }

    public static void setActiveScene(Scene value) {
        if (value == null) return;
        Core.scene = value;
    }
}
