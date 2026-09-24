package dev.bukkitkit.api;

/**
 * Unchecked failure raised by BukkitKit at bootstrap or resolution time.
 */
public class BukkitKitException extends RuntimeException {

    public BukkitKitException(String message) {
        super(message);
    }

    public BukkitKitException(String message, Throwable cause) {
        super(message, cause);
    }
}
