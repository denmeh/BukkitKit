package dev.bukkitkit.paper;

import java.util.function.BooleanSupplier;

/**
 * Test stub mirroring the real paper type so generated bootstraps compile in processor tests.
 */
public final class KitScheduler {

    public void runRepeating(long delayTicks, long periodTicks, boolean async, BooleanSupplier tick) {
    }

    public void runLater(long delayTicks, boolean async, Runnable runnable) {
    }
}
