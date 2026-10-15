package com.github.squi2rel.arc.runtime;

import arc.Application;
import arc.ApplicationCore;
import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.Graphics;
import arc.Settings;
import arc.audio.Audio;
import arc.files.Fi;
import arc.mock.MockAudio;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import com.github.squi2rel.arc.Trigger;
import com.github.squi2rel.arc.event.FileDroppedEvent;
import com.github.squi2rel.arc.event.GameEvent;
import com.github.squi2rel.arc.event.ResizeEvent;
import com.github.squi2rel.arc.script.ScriptEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeApplicationTest{
    private Application previousApp;
    private Graphics previousGraphics;
    private Settings previousSettings;
    private Audio previousAudio;
    private final List<String> calls = new ArrayList<>();
    private TestApplication app;

    @BeforeEach
    void setup(){
        previousApp = Core.app;
        previousGraphics = Core.graphics;
        previousSettings = Core.settings;
        previousAudio = Core.audio;
        Core.app = app = new TestApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        Core.audio = new MockAudio();
        Events.clear();
    }

    @AfterEach
    void restore(){
        Events.clear();
        Core.app = previousApp;
        Core.graphics = previousGraphics;
        Core.settings = previousSettings;
        Core.audio = previousAudio;
    }

    @Test
    void startupAndResizeDispatchListenersBeforeTypedEvents(){
        app.addListener(listener("a"));
        Events.run(Trigger.init, () -> calls.add("init event"));
        Events.on(ResizeEvent.class, event -> calls.add("resize event " + event.width + "x" + event.height));
        Events.on(GameEvent.class, event -> fail("Events must keep their existing concrete-class dispatch"));

        app.initialize();
        app.initialize();
        app.resize(800, 600);
        app.resize(800, 600);
        app.resize(0, 0);
        app.resize(1600, 900);

        assertTrue(app.initialized());
        assertSame(Thread.currentThread(), app.getMainThread());
        assertEquals(List.of("a init", "init event", "a resize 800x600", "resize event 800x600",
            "a resize 1600x900", "resize event 1600x900"), calls);
    }

    @Test
    void lateListenersReceiveInitAndCurrentSizeWithoutReplayingGlobalEvents(){
        AtomicInteger global = new AtomicInteger();
        Events.run(Trigger.init, global::incrementAndGet);
        Events.on(ResizeEvent.class, event -> global.incrementAndGet());
        app.initialize();
        app.resize(800, 600);
        ApplicationListener late = listener("late");
        app.addListener(late);
        app.addListener(late);

        assertTrue(calls.isEmpty());
        app.fireUpdate();
        app.update();
        app.update();

        assertEquals(List.of("late init", "late resize 800x600", "late update", "late update"), calls);
        assertEquals(2, global.get());
    }

    @Test
    void renderingAndTickNotificationsHaveIndependentCadences(){
        app.addListener(listener("a"));
        Events.run(Trigger.ready, () -> calls.add("ready"));
        Events.run(Trigger.update, () -> calls.add("tick"));
        Events.run(Trigger.draw, () -> calls.add("draw"));
        app.initialize();
        calls.clear();

        for(int frame = 0; frame < 3; frame++){
            app.defaultUpdate();
            app.update();
            app.fireDraw();
        }
        app.markReady();
        app.markReady();
        app.fireUpdate();

        assertTrue(app.ready());
        assertEquals(List.of("a update", "draw", "a update", "draw", "a update", "draw", "ready", "tick"), calls);
    }

    @Test
    void listenerChangesApplyToTheNextDispatchSnapshot(){
        ApplicationListener added = listener("added");
        ApplicationListener removed = listener("removed");
        app.addListener(new ApplicationListener(){
            @Override public void update(){
                calls.add("first update");
                app.addListener(added);
                app.removeListener(removed);
            }
        });
        app.addListener(removed);
        app.initialize();
        calls.clear();

        app.update();
        assertEquals(List.of("first update", "removed update"), calls);
        calls.clear();
        app.update();
        assertEquals(List.of("added init", "first update", "added update"), calls);
    }

    @Test
    void fileDropPreservesTheFileAndOrdersTheCallbackBeforeTheEvent(){
        Fi file = new Fi("example.txt");
        app.addListener(new ApplicationListener(){
            @Override public void fileDropped(Fi value){
                assertSame(file, value);
                calls.add("listener");
            }
        });
        Events.on(FileDroppedEvent.class, event -> {
            assertSame(file, event.file);
            calls.add("event");
        });
        app.initialize();
        app.fileDropped(file);
        assertEquals(List.of("listener", "event"), calls);
    }

    @Test
    void gracefulShutdownIsOrderedIdempotentAndNeverPauses(){
        app.addListener(listener("a"));
        Events.run(Trigger.exit, () -> calls.add("exit event"));
        Events.run(Trigger.dispose, () -> calls.add("dispose event"));
        Core.settings = new MockSettings(){
            @Override public synchronized void autosave(){ calls.add("settings"); }
        };
        Core.audio = new MockAudio(){
            @Override public void dispose(){ calls.add("audio"); }
        };
        app.initialize();
        app.markReady();
        calls.clear();

        app.requestExit();
        assertTrue(calls.isEmpty());
        app.dispose();
        app.dispose();
        app.update();
        app.fireUpdate();
        app.fireDraw();
        app.resize(400, 300);

        assertEquals(List.of("a exit", "exit event", "a dispose", "dispose event", "settings", "audio"), calls);
        assertTrue(app.disposed());
        assertFalse(app.initialized());
        assertFalse(app.ready());
    }

    @Test
    void exceptionalShutdownSkipsExitButContinuesPastFailingCallbacks(){
        app.addListener(new ApplicationListener(){
            @Override public void dispose(){ throw new IllegalStateException("expected listener failure"); }
        });
        app.addListener(listener("survivor"));
        Events.run(Trigger.exit, () -> fail("Exceptional disposal must not announce a graceful exit"));
        Events.run(Trigger.dispose, () -> {
            calls.add("dispose event");
            throw new IllegalStateException("expected event failure");
        });
        Core.settings = new MockSettings(){
            @Override public synchronized void autosave(){ calls.add("settings"); }
        };
        Core.audio = new MockAudio(){
            @Override public void dispose(){ calls.add("audio"); }
        };
        app.initialize();
        calls.clear();

        assertDoesNotThrow(app::dispose);
        assertEquals(List.of("survivor dispose", "dispose event", "settings", "audio"), calls);
    }

    @Test
    void headlessRuntimePersistsListenersAndRunsPostedWorkOnTheServerThread() throws Exception{
        HeadlessApplication server = new HeadlessApplication();
        Core.app = server;
        server.addListener(listener("server"));
        Events.run(Trigger.ready, () -> calls.add("ready"));
        Events.run(Trigger.update, () -> calls.add("tick"));
        Events.run(Trigger.draw, () -> fail("Headless ticks must not draw"));
        Thread owner = Thread.currentThread();
        AtomicReference<Thread> executionThread = new AtomicReference<>();
        Thread worker = new Thread(() -> server.post(() -> {
            executionThread.set(Thread.currentThread());
            calls.add("posted");
        }));
        worker.start();
        worker.join();
        assertNull(executionThread.get());

        server.start(() -> calls.add("stop"));
        server.tick();
        server.tick();
        assertSame(owner, executionThread.get());
        assertSame(owner, server.getMainThread());
        assertEquals(List.of("server init", "posted", "ready", "server update", "tick", "server update", "tick"), calls);

        calls.clear();
        server.exit();
        assertTrue(calls.isEmpty());
        server.tick();
        server.dispose();
        assertEquals(List.of("stop", "server update", "tick", "server exit", "server dispose"), calls);
    }

    @Test
    void postsDuringQueueDrainingWaitForTheNextServerTick(){
        HeadlessApplication server = new HeadlessApplication();
        Core.app = server;
        server.start(() -> {});
        server.post(() -> {
            calls.add("first");
            server.post(() -> calls.add("second"));
        });
        server.tick();
        assertEquals(List.of("first"), calls);
        server.tick();
        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void applicationCoreForwardsGracefulExitToItsModules(){
        ApplicationCore core = new ApplicationCore(){
            @Override public void setup(){ add(listener("module")); }
        };
        core.init();
        core.exit();
        assertEquals(List.of("module init", "module exit"), calls);
    }

    @Test
    void scriptEnvironmentExposesTriggersAndTypedEvents(){
        ScriptEnvironment scripts = new ScriptEnvironment();
        scripts.evaluate("""
            var ticks = 0, dimensions = '';
            Events.run(Trigger.update, function(){ ticks++; });
            Events.on(ResizeEvent, function(event){ dimensions = event.width + 'x' + event.height; });
            """, "events.js");
        Events.fire(Trigger.update);
        Events.fire(new ResizeEvent(1280, 720));
        assertEquals("1:1280x720", scripts.evaluate("ticks + ':' + dimensions", "result.js").toString());
        assertEquals(Boolean.TRUE, scripts.evaluate("new ResizeEvent(1, 2) instanceof GameEvent", "base.js"));
    }

    private ApplicationListener listener(String name){
        return new ApplicationListener(){
            @Override public void init(){ calls.add(name + " init"); }
            @Override public void resize(int width, int height){ calls.add(name + " resize " + width + "x" + height); }
            @Override public void update(){ calls.add(name + " update"); }
            @Override public void pause(){ fail("Desktop and headless runtimes must not pause"); }
            @Override public void resume(){ fail("Desktop and headless runtimes must not resume"); }
            @Override public void exit(){ calls.add(name + " exit"); }
            @Override public void dispose(){ calls.add(name + " dispose"); }
        };
    }

    private static class TestApplication extends RuntimeApplication{
        @Override public ApplicationType getType(){ return ApplicationType.desktop; }
        @Override public String getClipboardText(){ return null; }
        @Override public void setClipboardText(String text){ }
        @Override public void post(Runnable task){ task.run(); }
        @Override public void exit(){ requestExit(); }
    }
}
