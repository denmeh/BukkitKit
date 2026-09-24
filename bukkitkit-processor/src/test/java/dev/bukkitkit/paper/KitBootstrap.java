package dev.bukkitkit.paper;

/**
 * Test stub mirroring the real paper type so generated bootstraps compile in processor tests.
 */
public interface KitBootstrap {
    void start(PlatformServices services);

    void stop();
}
