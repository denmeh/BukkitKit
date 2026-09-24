package dev.bukkitkit.paper;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the singleton bind contract used by generated bootstraps.
 */
class SingletonBindContractTest {

    @Test
    void bindsAndExposesSingleton() {
        Logger logger = Logger.getLogger("bukkitkit-test");
        PlayerRepository.__bukkitKit_bind(new PlayerRepository(logger));
        PlayerManager.__bukkitKit_bind(new PlayerManager(PlayerRepository.__bukkitKit_instance, logger));

        try {
            assertSame(PlayerRepository.__bukkitKit_instance, PlayerManager.__bukkitKit_instance.repository());
            assertSame(logger, PlayerManager.__bukkitKit_instance.logger());
        } finally {
            PlayerManager.__bukkitKit_unbind();
            PlayerRepository.__bukkitKit_unbind();
        }
    }

    static final class PlayerRepository {
        static volatile PlayerRepository __bukkitKit_instance;
        private final Logger logger;

        PlayerRepository(Logger logger) {
            this.logger = logger;
        }

        static void __bukkitKit_bind(PlayerRepository instance) {
            if (__bukkitKit_instance != null) {
                throw new IllegalStateException("already bound");
            }
            __bukkitKit_instance = instance;
        }

        static void __bukkitKit_unbind() {
            __bukkitKit_instance = null;
        }
    }

    static final class PlayerManager {
        static volatile PlayerManager __bukkitKit_instance;
        private final PlayerRepository repository;
        private final Logger logger;

        PlayerManager(PlayerRepository repository, Logger logger) {
            this.repository = repository;
            this.logger = logger;
        }

        PlayerRepository repository() {
            return repository;
        }

        Logger logger() {
            return logger;
        }

        static void __bukkitKit_bind(PlayerManager instance) {
            if (__bukkitKit_instance != null) {
                throw new IllegalStateException("already bound");
            }
            __bukkitKit_instance = instance;
        }

        static void __bukkitKit_unbind() {
            __bukkitKit_instance = null;
        }
    }
}
