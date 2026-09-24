package dev.bukkitkit.api;

/**
 * Helpers for {@link Lifecycle} invocation with rollback on failure.
 */
public final class Lifecycles {

    private Lifecycles() {
    }

    public static void enable(Object instance) {
        if (instance instanceof Lifecycle lifecycle) {
            lifecycle.onEnable();
        }
    }

    public static void disable(Object instance) {
        if (instance instanceof Lifecycle lifecycle) {
            lifecycle.onDisable();
        }
    }

    /**
     * Enables instances in order; on failure disables those already enabled (reverse).
     */
    public static void enableAll(Object... instances) {
        int enabled = 0;
        try {
            for (Object instance : instances) {
                enable(instance);
                enabled++;
            }
        } catch (RuntimeException ex) {
            for (int i = enabled - 1; i >= 0; i--) {
                try {
                    disable(instances[i]);
                } catch (RuntimeException ignored) {
                    // best-effort rollback
                }
            }
            throw ex;
        }
    }

    /**
     * Disables instances in reverse order.
     */
    public static void disableAll(Object... instances) {
        for (int i = instances.length - 1; i >= 0; i--) {
            try {
                disable(instances[i]);
            } catch (RuntimeException ignored) {
                // best-effort shutdown
            }
        }
    }
}
