package com.github.squi2rel.arc;

import arc.Core;
import arc.Settings;
import arc.files.Fi;
import arc.mock.MockAudio;
import arc.mock.MockGraphics;
import arc.mock.MockInput;
import com.github.squi2rel.arc.runtime.GameFiles;
import com.github.squi2rel.arc.runtime.HeadlessApplication;
import com.github.squi2rel.arc.script.ScriptEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class ArcLib {
    public static final String MOD_ID = "arclib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean headlessInitialized;

    private ArcLib() {
    }

    /**
     * Returns the lazily initialized default JavaScript environment shared by ArcLib tools and scripts.
     */
    public static ScriptEnvironment scripts() {
        return ScriptsHolder.instance;
    }

    /**
     * Initializes the dedicated-server backend in the loader's game directory.
     */
    public static synchronized void initHeadless(File gameDirectory) {
        initHeadless(gameDirectory, gameDirectory.toPath().resolve("config/arclib").toFile());
    }

    /**
     * Initializes the headless backend with a platform-owned settings directory.
     */
    public static synchronized void initHeadless(File gameDirectory, File settingsDirectory) {
        if (headlessInitialized) return;

        Core.app = new HeadlessApplication();
        Core.graphics = new MockGraphics();
        Core.audio = new MockAudio();
        Core.input = new MockInput();
        Core.files = new GameFiles(gameDirectory);
        Core.settings = new Settings();
        Core.settings.setDataDirectory(new Fi(settingsDirectory));
        Core.settings.load();

        headlessInitialized = true;
        LOGGER.info("ArcLib headless backend initialized");
    }

    private static final class ScriptsHolder {
        private static final ScriptEnvironment instance = new ScriptEnvironment();
    }
}
