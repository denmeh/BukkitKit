package dev.bukkitkit.demo;

import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.Wire;

import org.bukkit.Server;

@Component
public final class PlayerManager {

    @Wire
    private PlayerRepository repository;
    @Wire
    private Server server;
    @Wire
    private DemoPlugin plugin;

    public String describe() {
        repository.warmUp();
        return "PlayerManager wired for plugin '" + plugin.getName()
                + "' on server '" + server.getName() + "'";
    }
}
