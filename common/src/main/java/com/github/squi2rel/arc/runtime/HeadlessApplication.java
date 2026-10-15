package com.github.squi2rel.arc.runtime;

import arc.Core;
import arc.mock.MockGraphics;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Dedicated-server application; posted work and callbacks run on the server thread.
 */
public final class HeadlessApplication extends RuntimeApplication {
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private Runnable stopServer;

    /**
     * Binds the server's stop operation and initializes on its main thread.
     */
    public void start(Runnable stopServer) {
        if (initialized() || disposed()) return;
        this.stopServer = Objects.requireNonNull(stopServer, "stopServer");
        initialize();
    }

    public void tick() {
        if (!initialized()) return;
        int count = tasks.size();
        for (int i = 0; i < count; i++) {
            Runnable task = tasks.poll();
            if (task != null) task.run();
        }
        if (disposed()) return;
        if (Core.graphics instanceof MockGraphics graphics) {
            graphics.updateTime();
            graphics.incrementFrameId();
        }
        markReady();
        defaultUpdate();
        update();
        fireUpdate();
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.headless;
    }

    @Override
    public String getClipboardText() {
        return null;
    }

    @Override
    public void setClipboardText(String text) {
    }

    @Override
    public void post(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (!disposed()) tasks.add(runnable);
    }

    @Override
    public void exit() {
        post(() -> {
            requestExit();
            stopServer.run();
        });
    }
}
