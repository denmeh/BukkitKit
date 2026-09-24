package dev.bukkitkit.demo;

import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.Wire;

import java.util.logging.Logger;

@Component
public final class PlayerRepository {

    @Wire
    private Logger logger;

    public void warmUp() {
        logger.info("PlayerRepository ready");
    }
}
