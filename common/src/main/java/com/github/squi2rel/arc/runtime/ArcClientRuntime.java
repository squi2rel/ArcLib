package com.github.squi2rel.arc.runtime;

import arc.Core;
import arc.func.Cons;
import arc.scene.Scene;
import arc.struct.Seq;

/**
 * Minecraft-independent state shared by ArcLib's client backend and client-side consumers.
 */
public final class ArcClientRuntime{
    private static final Seq<Cons<Scene>> beforeDrawListeners = new Seq<>();
    private static Scene scene;
    private static Scene activeScene;
    private static boolean initialized;

    private ArcClientRuntime(){
    }

    public static void attach(Scene initialScene){
        if(initialScene == null) throw new IllegalArgumentException("initialScene cannot be null");
        scene = initialScene;
        setActiveScene(initialScene);
        initialized = true;
    }

    public static void detach(){
        initialized = false;
        scene = null;
        activeScene = null;
        beforeDrawListeners.clear();
    }

    public static boolean initialized(){
        return initialized;
    }

    public static Scene scene(){
        return Core.scene == null ? scene : Core.scene;
    }

    public static Scene activeScene(){
        Scene current = activeScene;
        if(current != null) return current;
        return scene();
    }

    public static void setActiveScene(Scene value){
        if(value == null) return;
        activeScene = value;
        Core.scene = value;
    }

    public static void addBeforeDrawListener(Cons<Scene> listener){
        if(listener != null && !beforeDrawListeners.contains(listener, true)) beforeDrawListeners.add(listener);
    }

    public static void removeBeforeDrawListener(Cons<Scene> listener){
        beforeDrawListeners.remove(listener, true);
    }

    public static void fireBeforeDraw(Scene value){
        for(Cons<Scene> listener : beforeDrawListeners) listener.get(value);
    }
}
