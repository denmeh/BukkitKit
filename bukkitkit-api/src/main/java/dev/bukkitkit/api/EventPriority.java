package dev.bukkitkit.api;

/**
 * Priority for {@link OnEvent} handlers. Maps 1:1 to Bukkit's {@code EventPriority}.
 */
public enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR
}
