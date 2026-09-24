package dev.bukkitkit.paper;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

/**
 * Built-in Paper/Bukkit objects passed into generated bootstraps.
 */
public record PlatformServices(
        Plugin plugin,
        JavaPlugin javaPlugin,
        Server server,
        Logger logger,
        PluginManager pluginManager,
        ServicesManager servicesManager,
        BukkitScheduler scheduler,
        KitScheduler kitScheduler
) {

    public static PlatformServices from(JavaPlugin plugin) {
        Server server = plugin.getServer();
        return new PlatformServices(
                plugin,
                plugin,
                server,
                plugin.getLogger(),
                server.getPluginManager(),
                server.getServicesManager(),
                server.getScheduler(),
                new KitScheduler(plugin)
        );
    }
}
