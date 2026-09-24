package dev.bukkitkit.paper;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test stub mirroring the real paper type so generated bootstraps compile in processor tests.
 */
public final class PlatformServices {

    public KitScheduler kitScheduler() {
        return new KitScheduler();
    }

    public PluginManager pluginManager() {
        return null;
    }

    public JavaPlugin javaPlugin() {
        return null;
    }
}
