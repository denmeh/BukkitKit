package dev.bukkitkit.paper;

/**
 * Test stub mirroring the real paper type so generated bootstraps compile in processor tests.
 */
public final class PlatformServices {

    public KitScheduler kitScheduler() {
        return new KitScheduler();
    }
}
