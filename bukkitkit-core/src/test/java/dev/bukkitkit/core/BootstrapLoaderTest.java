package dev.bukkitkit.core;

import dev.bukkitkit.api.BukkitKitSymbols;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsBootstrapFromResourceIndex() throws Exception {
        Path resource = tempDir.resolve(BukkitKitSymbols.BOOTSTRAP_RESOURCE);
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, TestBootstrap.class.getName() + "\n");

        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                getClass().getClassLoader())) {
            List<Runnable> bootstraps = BootstrapLoader.load(loader, Runnable.class);
            assertEquals(1, bootstraps.size());
            bootstraps.getFirst().run();
            assertTrue(TestBootstrap.RAN.get());
        }
    }

    public static final class TestBootstrap implements Runnable {
        static final AtomicBoolean RAN = new AtomicBoolean();

        @Override
        public void run() {
            RAN.set(true);
        }
    }
}
