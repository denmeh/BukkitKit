package dev.bukkitkit.api;

/**
 * Optional lifecycle hooks for {@link Component} singletons.
 * <p>
 * {@link #onEnable()} runs after all components are constructed and wired,
 * in dependency order. {@link #onDisable()} runs in reverse order before unbind.
 */
public interface Lifecycle {

    default void onEnable() {
    }

    default void onDisable() {
    }
}
