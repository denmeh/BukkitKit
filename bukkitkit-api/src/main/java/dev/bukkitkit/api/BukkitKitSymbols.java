package dev.bukkitkit.api;

/**
 * Resource paths and naming conventions used by the processor and runtime.
 */
public final class BukkitKitSymbols {

    /**
     * Class-path resource listing fully-qualified {@code KitBootstrap} implementation names,
     * one per line.
     */
    public static final String BOOTSTRAP_RESOURCE = "META-INF/bukkitkit/bootstrap";

    /**
     * Suffix appended to a {@link BukkitKit} marker simple name for the generated
     * {@code JavaPlugin} entry (e.g. {@code DemoPlugin} → {@code DemoPlugin_BukkitKit}).
     */
    public static final String GENERATED_PLUGIN_SUFFIX = "_BukkitKit";

    private BukkitKitSymbols() {
    }
}
