package dev.bukkitkit.processor.builtins;

import java.util.Map;
import java.util.Set;

/**
 * Platform types provided by {@code PlatformServices} at runtime.
 */
public final class BuiltInTypes {

    private static final Map<String, String> ACCESSORS = Map.of(
            "org.bukkit.plugin.Plugin", "plugin",
            "org.bukkit.plugin.java.JavaPlugin", "javaPlugin",
            "org.bukkit.Server", "server",
            "java.util.logging.Logger", "logger",
            "org.bukkit.plugin.PluginManager", "pluginManager",
            "org.bukkit.plugin.ServicesManager", "servicesManager",
            "org.bukkit.scheduler.BukkitScheduler", "scheduler"
    );

    private BuiltInTypes() {
    }

    public static boolean isBuiltIn(String qualifiedName) {
        return ACCESSORS.containsKey(qualifiedName);
    }

    public static Set<String> all() {
        return ACCESSORS.keySet();
    }

    /**
     * @return PlatformServices accessor name without parentheses, e.g. {@code logger}
     */
    public static String accessor(String qualifiedName) {
        String accessor = ACCESSORS.get(qualifiedName);
        if (accessor == null) {
            throw new IllegalArgumentException("Not a built-in type: " + qualifiedName);
        }
        return accessor;
    }
}
