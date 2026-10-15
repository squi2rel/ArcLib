package com.github.squi2rel.arc;

import arc.Core;
import arc.Settings;
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

	/** Returns the lazily initialized default JavaScript environment shared by ArcLib tools and scripts. */
	public static ScriptEnvironment scripts() {
		return ScriptsHolder.instance;
	}

	private static final class ScriptsHolder {
		private static final ScriptEnvironment instance = new ScriptEnvironment();
	}

	/**
	 * Initializes Arc's headless runtime with persistent settings for dedicated servers.
	 */
	public static void initHeadless() {
		initHeadless(new File(""));
	}

	/** Initializes the dedicated-server backend in the loader's game directory. */
	public static synchronized void initHeadless(File gameDirectory) {
		if (headlessInitialized) return;

		Core.app = new HeadlessApplication();
		Core.graphics = new MockGraphics();
		Core.audio = new MockAudio();
		Core.input = new MockInput();
		Core.files = new GameFiles(gameDirectory);
		Core.settings = new Settings();
		Core.settings.setDataDirectory(Core.files.local("config/arclib"));
		Core.settings.load();

		headlessInitialized = true;
		LOGGER.info("ArcLib headless backend initialized");
	}
}
