package dev.bukkitkit.demo;

import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.OnDisable;
import dev.bukkitkit.api.OnEnable;
import dev.bukkitkit.api.ScheduleUnit;
import dev.bukkitkit.api.Scheduled;
import dev.bukkitkit.api.Wire;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public final class PlayerManager {

    @Wire
    private PlayerRepository repository;
    @Wire
    private Server server;
    @Wire
    private JavaPlugin plugin;

    private int ticks;

    @OnEnable
    public void start() {
        plugin.getLogger().info("PlayerManager onEnable — " + describe());
    }

    @OnDisable
    public void stop() {
        plugin.getLogger().info("PlayerManager onDisable (ticks=" + ticks + ")");
    }

    @Scheduled(every = 30, unit = ScheduleUnit.SECONDS)
    public void heartbeat() {
        ticks++;
        plugin.getLogger().info("PlayerManager heartbeat #" + ticks);
    }

    public String describe() {
        repository.warmUp();
        return "PlayerManager wired for plugin '" + plugin.getName()
                + "' on server '" + server.getName() + "'";
    }
}
