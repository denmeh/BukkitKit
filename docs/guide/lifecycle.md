# 6. Lifecycle

Goal: run setup and teardown on components in a safe order.

## Plugin vs component lifecycle

You already know `JavaPlugin.onEnable` / `onDisable`. Components can opt into the same idea by implementing `Lifecycle`:

```java
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.Lifecycle;
import dev.bukkitkit.api.Wire;

@Component
public final class PlayerManager implements Lifecycle {

    @Wire
    private HelloPlugin plugin;

    @Override
    public void onEnable() {
        plugin.getLogger().info("PlayerManager onEnable");
        // open connections, load files, warm caches, ...
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("PlayerManager onDisable");
        // flush data, close resources, ...
    }
}
```

Both methods are optional — `Lifecycle` provides empty defaults, so you can override only what you need.

## Order

- **Enable:** after every component is constructed and `@Wire` fields are set, BukkitKit calls `onEnable()` in **dependency order** (dependencies first)
- **Disable:** `onDisable()` runs in **reverse** order, then BukkitKit unbinds

So if `PlayerManager` depends on `PlayerRepository`, the repository enables first and disables last.

## Relation to the plugin class

Typical timeline:

1. BukkitKit constructs and wires all components
2. Component `Lifecycle.onEnable()` methods run
3. Your `JavaPlugin.onEnable()` body runs
4. On stop: your `onDisable()` body, then schedules/listeners cleanup, then component `onDisable()` (via BukkitKit’s shutdown path)

Put “start this subsystem” logic on the component that owns it. Keep the plugin class as the thin entry point.

## Next

[Scheduled tasks](./scheduling) — repeat work on a timer without manual `runTaskTimer` glue.
