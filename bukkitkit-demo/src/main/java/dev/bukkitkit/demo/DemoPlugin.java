package dev.bukkitkit.demo;

import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Wire;

import org.bukkit.plugin.java.JavaPlugin;

@BukkitKit
public final class DemoPlugin extends JavaPlugin {

    @Wire
    private PlayerManager playerManager;

    /** Concrete plugin instance — same as {@code this}. */
    @Wire
    private DemoPlugin self;

    @Override
    public void onEnable() {
        getLogger().info("BukkitKit demo enabled — " + playerManager.describe()
                + " (self=" + self.getName() + ")");
    }

    @Override
    public void onDisable() {
        getLogger().info("BukkitKit demo disabled");
    }
}
