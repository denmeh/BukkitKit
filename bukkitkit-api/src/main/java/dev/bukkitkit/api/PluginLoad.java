package dev.bukkitkit.api;

/**
 * When the plugin loads relative to the world — maps to {@code plugin.yml} {@code load}.
 */
public enum PluginLoad {

    /** After worlds are loaded (Bukkit default). */
    POSTWORLD,

    /** During server startup, before worlds. */
    STARTUP
}
