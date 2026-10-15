package com.github.squi2rel.arc.bukkit;

import arc.Core;
import com.github.squi2rel.arc.ArcLib;
import com.github.squi2rel.arc.runtime.HeadlessApplication;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * Bukkit adapter only; the shared Arc runtime remains independent of the game platform.
 */
public final class ArcLibPlugin extends JavaPlugin {
    private HeadlessApplication application;
    private BukkitTask ticking;

    @Override
    public void onEnable() {
        ArcLib.initHeadless(new File("").getAbsoluteFile(), getDataFolder());
        application = (HeadlessApplication) Core.app;
        application.start(() -> getServer().shutdown());
        ticking = getServer().getScheduler().runTaskTimer(this, application::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (ticking != null) {
            ticking.cancel();
            ticking = null;
        }
        if (application != null) {
            application.requestExit();
            application.dispose();
            application = null;
        }
    }
}
