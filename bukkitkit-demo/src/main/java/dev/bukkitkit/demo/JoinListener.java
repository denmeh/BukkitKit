package dev.bukkitkit.demo;

import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.api.Wire;

import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener-only class — no {@code @Component}; {@code @OnEvent} makes it a managed singleton.
 */
public final class JoinListener {

    @Wire
    private DemoPlugin plugin;

    public JoinListener() {
    }

    @OnEvent
    public void onJoin(PlayerJoinEvent event) {
        plugin.getLogger().info("Player joined: " + event.getPlayer().getName());
    }
}
