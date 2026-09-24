package org.bukkit.plugin.java;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

/** Test classpath stub mirroring Bukkit {@code JavaPlugin} lifecycle hooks. */
public abstract class JavaPlugin implements Plugin {

    public void onEnable() {
    }

    public void onDisable() {
    }

    public PluginCommand getCommand(String name) {
        return null;
    }
}
