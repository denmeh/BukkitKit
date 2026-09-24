package dev.bukkitkit.api;

/**
 * Synthetic member names and resource paths used by the processor and runtime.
 */
public final class BukkitKitSymbols {

    /**
     * Class-path resource listing fully-qualified {@code KitBootstrap} implementation names,
     * one per line.
     */
    public static final String BOOTSTRAP_RESOURCE = "META-INF/bukkitkit/bootstrap";

    /** Synthetic singleton field injected into {@link Component} classes (not for app use). */
    public static final String INSTANCE_FIELD = "__bukkitKit_instance";

    /** Synthetic bind method injected into {@link Component} classes (not for app use). */
    public static final String BIND_METHOD = "__bukkitKit_bind";

    /** Synthetic unbind method injected into {@link Component} classes (not for app use). */
    public static final String UNBIND_METHOD = "__bukkitKit_unbind";

    /** Instance method on {@link BukkitKit} plugins that assigns component fields. */
    public static final String WIRE_METHOD = "__bukkitKit_wire";

    private BukkitKitSymbols() {
    }
}
