# ArcLib

ArcLib ports the [Arc](https://github.com/Anuken/Arc) framework to Minecraft.

## Source layout

- `common`: plain Java Arc sources and Minecraft-independent ArcLib runtime/UI APIs.
- `bukkit`: Java 17 plugin adapter compiled against Spigot 1.20.1.
- `<minecraft>/bridge`: Minecraft-version-specific backend and mixins shared by both loaders.
- `<minecraft>/fabric` and `forge`/`neoforge`: loader entrypoints, metadata, and final packaging.

Supported versions are Minecraft 1.20.1 on Fabric and Forge (including the compatible NeoForge 47.1 fork), and Minecraft 1.21.1, 1.21.11, and 26.2 on Fabric and NeoForge. Minecraft 1.20.1 requires Java 17, Minecraft 1.21.1 and 1.21.11 require Java 21, and Minecraft 26.2 requires Java 25. The standalone common module targets Java 17.

When a scene control accepts a mouse press, Arc releases Minecraft's mouse grab, including buttons and drag controls that do not take keyboard focus. Clicking empty space or merely hovering does not release the mouse. Keyboard focus from a global shortcut or script also releases the mouse. Both paths clear held Minecraft key bindings. Key releases clear the corresponding game binding even when a focused Arc control consumes the event, preventing movement keys from remaining pressed.

While Minecraft has grabbed the mouse, Arc reports the center of its current viewport with zero mouse movement deltas. Scene pointer events use the same center coordinates, while Minecraft continues to receive raw movement for camera control. Releasing the mouse restores normal cursor coordinates.

## Build

Minecraft version builds share `gradle/java-conventions.gradle` for artifact names, repositories, and explicit Java 17/21/25 toolchains. Loader modules use `gradle/loader-packaging.gradle`: `commonCode` resolves only this project's common jar, and `shadowBundle` contains only the common/bridge code owned by this mod. External libraries and loader dependencies are declared separately by each loader. Shared bridge dependencies are compile-only and do not propagate a loader into another platform's runtime.

```bash
./gradlew buildAll
```

`buildAll` also collects the eight mod jars and the Bukkit plugin jar into the project root's `build/libs/` directory for uploading. This directory is synchronized with the current build, removing stale artifacts; sources, development, thin, common, and bridge jars are excluded.

Build one version with `./gradlew -p <minecraft> build`, where `<minecraft>` is `1.20.1`, `1.21.1`, `1.21.11`, or `26.2`. Build the standalone Java module with `./gradlew -p common build`.

Final loader jars are written below `<minecraft>/<loader>/build/libs/` and include the Minecraft version in their name. Each loader jar provides Rhino together with ArcLib.

## IntelliJ IDEA

Open this directory as a Gradle project. The root build includes `common`, the Bukkit plugin, and all four Minecraft versions as composite builds, so IDEA imports their Java sources, resources, tests, dependencies, and loader run configurations. After updating an existing checkout, use **Reload All Gradle Projects** in the Gradle tool window.

Use Java 25 as IDEA's Gradle JVM when importing all versions, and install JDK 17 and 21 for the older versions' toolchains. Each version can also be opened separately through its own `build.gradle`.

## JavaScript environment

`com.github.squi2rel.arc.script.ScriptEnvironment` owns a persistent, thread-safe Rhino 1.9.1 scope using Rhino's current ECMAScript language mode. It loads the Arc-compatible helpers from `base.js` and imports every Arc package present in this port through `global.js`.

`com.github.squi2rel.arc.ArcLib.scripts()` returns the lazily initialized default environment. ArcTools Tester uses this same instance, so variables and functions are shared with Java callers:

```java
ScriptEnvironment scripts = ArcLib.scripts();
scripts.put("value", 40); // Tester can now read value.
Object result = scripts.evaluate("value + 2", "example.js");
```

Each executing thread enters its own Rhino context. Java-to-JavaScript callbacks, including UI providers and HTTP response handlers, wait for the shared scope to become available and execute synchronously on their calling thread. Explicit environment operations fail immediately if another thread is executing; `tryWithContext` instead returns its supplied fallback when busy.

Use a new instance when an independent environment is needed:

```java
ScriptEnvironment scripts = new ScriptEnvironment();
scripts.put("value", 40);
Object result = scripts.evaluate("value + 2", "example.js");
```

Scripts can also be compiled with `compile` or `compileStrict` and reused with `execute`. Rhino is an API dependency of `arclib-common` and is embedded in each loader artifact, so consumers do not need to package it themselves.

## Application lifecycle and events

`Core.app.addListener(ApplicationListener)` registers lifecycle callbacks on the client rendering thread or dedicated-server thread. Listeners registered after startup receive `init()` before their first callback, followed by the current full framebuffer size on clients. Registering the same instance twice has no effect. Dispatch uses a snapshot, so listener changes take effect on the next dispatch. Callbacks run without a graphics context on dedicated servers.

`ApplicationListener.update()` runs once per main client rendering frame and once per dedicated-server tick. The parameterless `com.github.squi2rel.arc.Trigger.update` event runs once per **tick** on either backend, independently of rendering. The integrated server does not dispatch a second set of events or dispose the client application.

```java
import arc.Events;
import com.github.squi2rel.arc.Trigger;
import com.github.squi2rel.arc.event.ResizeEvent;
import com.github.squi2rel.arc.event.FileDroppedEvent;

Events.run(Trigger.update, () -> { /* client or dedicated-server tick */ });
Events.run(Trigger.draw, () -> { /* before drawing the main client scene */ });
Events.on(ResizeEvent.class, event -> resize(event.width, event.height));
        Events.

on(FileDroppedEvent .class, event ->open(event.files));
```

`Trigger.init` fires after Arc's services initialize. `Trigger.ready` fires once after initial client loading completes
or on the first dedicated-server tick, before that tick's update event. These events are not replayed;
`RuntimeApplication.initialized()` and `ready()` expose the current state. Other parameterless events are `exit` and
`dispose`. `ResizeEvent` and `FileDroppedEvent` extend `com.github.squi2rel.arc.event.GameEvent`; subscribe to their
concrete classes with `Events.on`. Resize dimensions are full framebuffer pixels, independent of temporary
rendering sizes. Each file-drop operation produces one `fileDropped(Seq<Fi> files)` callback and one `FileDroppedEvent`,
with all files in drop order in `event.files`, without replacing the game's own file-drop handling. Headless servers
emit neither resize nor draw events.

Listeners run before the corresponding global event. A graceful shutdown calls listener `exit()`, fires `Trigger.exit`,
calls listener `dispose()`, and fires `Trigger.dispose` before releasing backend resources. Exceptional cleanup skips
`exit`. Disposal is idempotent; errors in shutdown callbacks are logged and do not prevent the remaining cleanup.
Desktop and headless backends never call `pause()` or `resume()`, including on focus changes and shutdown.

On dedicated servers `Core.app.post()` queues work for the server thread before the next tick update; work posted while draining the queue waits until the following tick. `Core.app.exit()` requests normal server shutdown. On clients it requests normal client shutdown. `Trigger`, `GameEvent`, `ResizeEvent`, and `FileDroppedEvent` are also available directly in the JavaScript environment.

## Bukkit / Spigot / Paper

Install `arclib-bukkit-1.0.0.jar` from `build/libs/` in the server's `plugins/` directory. This is one Java 17 artifact compiled against Spigot 1.20.1, also used on newer server versions. Minecraft clients continue to install the matching ArcLib mod when they need Arc rendering.

The plugin supplies the same `Core`, `ArcLib.scripts()`, headless lifecycle, tick events and main-thread `Core.app.post()` APIs. Settings are stored in `plugins/ArcLib/`; local file paths still resolve against the server working directory. `Core.app.exit()` requests normal server shutdown. Disabling ArcLib cancels its scheduled tick and disposes the runtime. It does not provide a graphics backend on the server.

Other Bukkit plugins should declare `depend: [ArcLib]` in `plugin.yml` and use ArcLib as a compile-only dependency: do not embed another copy of Arc or Rhino. The ArcLib plugin already bundles both under their original public package names. No additional runtime library downloads are needed.

Build only the plugin with `./gradlew -p bukkit build`. `buildAll` collects eight mod artifacts and this one plugin artifact. Bukkit adapters stay separate from common sources. Folia, proxy plugins, hot reload and hybrid servers loading both the ArcLib mod and plugin are outside the supported setup.
