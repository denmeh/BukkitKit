package dev.bukkitkit.paper;

import dev.bukkitkit.api.BukkitKitException;
import dev.bukkitkit.core.BootstrapLoader;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime entry points invoked from AST-rewritten {@code onEnable}/{@code onDisable}.
 */
public final class BukkitKitSupport {

    private record Session(List<KitBootstrap> bootstraps, KitScheduler kitScheduler) {
    }

    private static final Map<JavaPlugin, Session> ACTIVE = new ConcurrentHashMap<>();

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
            ACTIVE.put(plugin, new Session(List.copyOf(started), services.kitScheduler()));
            return services;
        } catch (RuntimeException ex) {
            services.kitScheduler().cancelAll();
            stopAll(started);
            if (ex instanceof BukkitKitException) {
                throw ex;
            }
            throw new BukkitKitException("BukkitKit: bootstrap failed for " + plugin.getName(), ex);
        }
    }

    /**
     * Stops schedules then bootstraps for {@code plugin}. Called at the end of {@code onDisable}.
     */
    public static void disable(JavaPlugin plugin) {
        Session session = ACTIVE.remove(plugin);
        if (session == null) {
            return;
        }
        try {
            HandlerList.unregisterAll(plugin);
            session.kitScheduler().cancelAll();
        } finally {
            stopAll(session.bootstraps());
        }
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
