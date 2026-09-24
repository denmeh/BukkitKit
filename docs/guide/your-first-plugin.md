# 1. Your first plugin

Goal: declare a BukkitKit plugin with metadata — no `JavaPlugin` subclass and no hand-written `plugin.yml`.

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

Plus a hand-written `plugin.yml` that points `main` at this class.

## With `@BukkitKit`

```java
package com.example.hello;

import dev.bukkitkit.api.BukkitKit;

@BukkitKit(
        name = "Hello",
        version = "1.0.0",
        apiVersion = "1.21"
)
public final class HelloPlugin {
}
```

That is the whole change for step 1.

What happens under the hood (you do not write this):

- BukkitKit turns `HelloPlugin` into a `JavaPlugin` at compile time
- It writes `plugin.yml` from the annotation (`name`, `version`, `main`, `api-version`, …)
- When the server enables the plugin, BukkitKit runs bootstrap (components, events, schedules)

For this empty plugin there are no components yet, so bootstrap is a no-op beyond setup. Add startup logic later with `@OnEnable` on components.

## Checklist

- Public top-level class annotated with `@BukkitKit`
- Metadata: at least `name`, `version`, `apiVersion`
- Do **not** extend `JavaPlugin` or write `plugin.yml` yourself
- Setup from the previous page is in place (dependency + processor + shade)

## Next

[Components](./components) — put plugin logic in small service classes.
