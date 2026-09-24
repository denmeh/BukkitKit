package dev.bukkitkit.demo;

import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.Lifecycle;
import dev.bukkitkit.api.ScheduleUnit;
import dev.bukkitkit.api.Scheduled;
import dev.bukkitkit.api.Wire;

import org.bukkit.Server;

@Component
public final class PlayerManager implements Lifecycle {

    @Wire
    private PlayerRepository repository;
    @Wire
    private Server server;
    @Wire
    private DemoPlugin plugin;

    private int ticks;

    @Override
    public void onEnable() {
        plugin.getLogger().info("PlayerManager onEnable");
    }

    @Override
    public void onDisable() {
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
