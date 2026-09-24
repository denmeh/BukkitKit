package dev.bukkitkit.demo;

import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.api.Wire;

import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Logger;

/**
 * Listener-only class — no {@code @Component}; {@code @OnEvent} makes it a managed singleton.
 */
public final class JoinListener {

    @Wire
    private Logger logger;

    public JoinListener() {
    }

    @OnEvent
    public void onJoin(PlayerJoinEvent event) {
        logger.info("Player joined: " + event.getPlayer().getName());
    }
}
