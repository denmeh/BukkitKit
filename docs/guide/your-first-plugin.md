# 1. Your first plugin

Goal: turn a normal Paper plugin into a BukkitKit plugin with one annotation.

## Before BukkitKit

A typical empty plugin looks like this:

```java
package com.example.hello;

import org.bukkit.plugin.java.JavaPlugin;

public final class HelloPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Hello enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Hello disabled");
    }
}
```

Plus a `plugin.yml` that points `main` at this class. That does not change with BukkitKit.

## Add `@BukkitKit`

```java
package com.example.hello;

import dev.bukkitkit.api.BukkitKit;
import org.bukkit.plugin.java.JavaPlugin;

@BukkitKit
public final class HelloPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Hello enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Hello disabled");
    }
}
```

That is the whole change for step 1.

What happens under the hood (you do not write this):

- When the server calls `onEnable`, BukkitKit runs first and starts its bootstrap for this plugin
- When `onDisable` finishes your code, BukkitKit cleans up (listeners it registered, schedules, etc.)

For this empty plugin there are no components yet, so bootstrap is a no-op beyond setup. The annotation is still useful: it opts the plugin into BukkitKit so later features work.

## Checklist

- Class extends `JavaPlugin`
- Class is annotated with `@BukkitKit`
- `plugin.yml` `main` matches the class
- Setup from the previous page is in place (dependency + processor + shade)

## Next

[Components](./components) — move logic out of the plugin class into a small service.
