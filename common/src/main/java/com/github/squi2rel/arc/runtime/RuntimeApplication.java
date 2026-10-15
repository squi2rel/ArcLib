package com.github.squi2rel.arc.runtime;

import arc.Application;
import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import com.github.squi2rel.arc.Trigger;
import com.github.squi2rel.arc.event.FileDroppedEvent;
import com.github.squi2rel.arc.event.ResizeEvent;

import java.util.IdentityHashMap;
import java.util.Objects;

/**
 * Shared lifecycle dispatch for the client and headless backends. Backend dispatch methods run on
 * the application thread. Registering and removing listeners is safe from other threads.
 * Pause and resume are not synthesized for desktop focus changes or shutdown.
 */
public abstract class RuntimeApplication implements Application {
    private final Seq<ApplicationListener> listeners = new Seq<>(ApplicationListener.class);
    private final IdentityHashMap<ApplicationListener, ListenerState> states = new IdentityHashMap<>();
    private volatile Thread mainThread;
    private volatile boolean initialized, ready, disposed, exitRequested;
    private boolean hasSize;
    private int width, height;

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            Log.err("Application shutdown callback failed", failure);
        }
    }

    @Override
    public final Seq<ApplicationListener> getListeners() {
        return listeners;
    }

    @Override
    public final void addListener(ApplicationListener listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (listeners) {
            if (disposed) throw new IllegalStateException("Application is disposed");
            if (!listeners.contains(listener, true)) listeners.add(listener);
        }
    }

    @Override
    public final void removeListener(ApplicationListener listener) {
        synchronized (listeners) {
            listeners.remove(listener, true);
            states.remove(listener);
        }
    }

    @Override
    public final Thread getMainThread() {
        return mainThread;
    }

    public final boolean initialized() {
        return initialized && !disposed;
    }

    public final boolean ready() {
        return ready && !disposed;
    }

    public final boolean disposed() {
        return disposed;
    }

    /**
     * Called once by the backend after its Core services are available.
     */
    public final void initialize() {
        if (initialized || disposed) return;
        mainThread = Thread.currentThread();
        initialized = true;
        prepare(snapshot());
        Events.fire(Trigger.init);
    }

    /**
     * Loading completion is separate from initialization and is not replayed.
     */
    public final void markReady() {
        if (!initialized() || ready) return;
        prepare(snapshot());
        ready = true;
        Events.fire(Trigger.ready);
    }

    /**
     * Listener updates run per frame on clients and per tick on headless servers.
     */
    public final void update() {
        if (initialized()) dispatch(ApplicationListener::update);
    }

    /**
     * Tick notification; deliberately separate from the rendering-driven listener update.
     */
    public final void fireUpdate() {
        if (!initialized()) return;
        prepare(snapshot());
        Events.fire(Trigger.update);
    }

    public final void fireDraw() {
        if (initialized()) Events.fire(Trigger.draw);
    }

    public final void resize(int width, int height) {
        if (!initialized() || width <= 0 || height <= 0) return;
        if (hasSize && this.width == width && this.height == height) return;
        this.width = width;
        this.height = height;
        hasSize = true;
        // Preparation delivers the new size once, including to newly registered listeners.
        prepare(snapshot());
        Events.fire(new ResizeEvent(width, height));
    }

    public final void fileDropped(Seq<Fi> files) {
        Objects.requireNonNull(files, "files");
        if (!initialized()) return;
        dispatch(listener -> listener.fileDropped(files));
        Events.fire(new FileDroppedEvent(files));
    }

    /**
     * Marks a graceful stop request. Notifications are deferred to backend disposal.
     */
    public final void requestExit() {
        if (!disposed) exitRequested = true;
    }

    @Override
    public final synchronized void dispose() {
        if (disposed) return;
        disposed = true;
        ApplicationListener[] closing = snapshot();
        if (initialized) {
            for (ApplicationListener listener : closing) safely(() -> prepare(listener));
            if (exitRequested) {
                for (ApplicationListener listener : closing) safely(listener::exit);
                safely(() -> Events.fire(Trigger.exit));
            }
            for (ApplicationListener listener : closing) safely(listener::dispose);
            safely(() -> Events.fire(Trigger.dispose));
        }
        if (Core.settings != null) safely(Core.settings::autosave);
        if (Core.audio != null) safely(Core.audio::dispose);
        synchronized (listeners) {
            listeners.clear();
            states.clear();
        }
        ready = false;
    }

    private void dispatch(Cons<ApplicationListener> callback) {
        ApplicationListener[] current = snapshot();
        prepare(current);
        for (ApplicationListener listener : current) callback.get(listener);
    }

    private ApplicationListener[] snapshot() {
        synchronized (listeners) {
            return listeners.toArray(ApplicationListener.class);
        }
    }

    private void prepare(ApplicationListener[] current) {
        for (ApplicationListener listener : current) prepare(listener);
    }

    private void prepare(ApplicationListener listener) {
        ListenerState state;
        synchronized (listeners) {
            state = states.computeIfAbsent(listener, ignored -> new ListenerState());
        }
        if (!state.initialized) {
            state.initialized = true;
            listener.init();
        }
        if (hasSize && (!state.hasSize || state.width != width || state.height != height)) {
            state.hasSize = true;
            state.width = width;
            state.height = height;
            listener.resize(width, height);
        }
    }

    private static final class ListenerState {
        boolean initialized, hasSize;
        int width, height;
    }
}
