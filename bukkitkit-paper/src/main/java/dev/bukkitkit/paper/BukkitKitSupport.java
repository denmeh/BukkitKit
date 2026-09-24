package dev.bukkitkit.paper;

import dev.bukkitkit.api.BukkitKitException;
import dev.bukkitkit.core.BootstrapLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime entry points invoked from AST-rewritten {@code onEnable}/{@code onDisable}.
 */
public final class BukkitKitSupport {

    private static final Map<JavaPlugin, List<KitBootstrap>> ACTIVE = new ConcurrentHashMap<>();

    private BukkitKitSupport() {
    }

    /**
     * Starts generated bootstraps for {@code plugin}. Called at the beginning of {@code onEnable}.
     *
     * @return platform services for field wiring
     */
    public static PlatformServices enable(JavaPlugin plugin) {
        if (ACTIVE.containsKey(plugin)) {
            throw new BukkitKitException("BukkitKit: already enabled for " + plugin.getName());
        }
        PlatformServices services = PlatformServices.from(plugin);
        List<KitBootstrap> loaded = BootstrapLoader.load(plugin.getClass().getClassLoader(), KitBootstrap.class);
        List<KitBootstrap> started = new ArrayList<>();
        try {
            for (KitBootstrap bootstrap : loaded) {
                bootstrap.start(services);
                started.add(bootstrap);
            }
            ACTIVE.put(plugin, List.copyOf(started));
            return services;
        } catch (RuntimeException ex) {
            stopAll(started);
            throw new BukkitKitException("BukkitKit: bootstrap failed for " + plugin.getName(), ex);
        }
    }

    /**
     * Stops bootstraps for {@code plugin}. Called at the end of {@code onDisable}.
     */
    public static void disable(JavaPlugin plugin) {
        List<KitBootstrap> bootstraps = ACTIVE.remove(plugin);
        if (bootstraps == null) {
            return;
        }
        stopAll(bootstraps);
    }

    private static void stopAll(List<KitBootstrap> bootstraps) {
        for (int i = bootstraps.size() - 1; i >= 0; i--) {
            try {
                bootstraps.get(i).stop();
            } catch (RuntimeException ignored) {
                // best-effort shutdown
            }
        }
    }
}
