package dev.bukkitkit.demo;

import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Permission;
import dev.bukkitkit.api.PermissionDefault;
import dev.bukkitkit.api.PluginLoad;

@BukkitKit(
        name = "BukkitKitDemo",
        version = "1.0-SNAPSHOT",
        apiVersion = "1.21",
        description = "Demo Paper plugin for BukkitKit",
        authors = {"denmeh"},
        website = "https://github.com/denmeh/BukkitKit",
        prefix = "BukkitKitDemo",
        load = PluginLoad.POSTWORLD,
        permissions = {
                @Permission(
                        name = "bukkitkitdemo.hello",
                        description = "Use /hello",
                        defaultValue = PermissionDefault.TRUE)
        }
)
public final class DemoPlugin {
}
