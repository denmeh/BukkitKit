# 6. Lifecycle

Goal: run setup and teardown on components in a safe order.

## `@OnEnable` / `@OnDisable`

Mark methods on a component (or any managed class) to run when the plugin starts or stops:

```java
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.OnDisable;
import dev.bukkitkit.api.OnEnable;
import dev.bukkitkit.api.Wire;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public final class PlayerManager {

    @Wire
    private JavaPlugin plugin;

    @OnEnable
    public void start() {
        plugin.getLogger().info("PlayerManager onEnable");
        // open connections, load files, warm caches, ...
    }

    @OnDisable
    public void stop() {
        plugin.getLogger().info("PlayerManager onDisable");
        // flush data, close resources, ...
    }
}
```

Rules:

- `public`, no parameters, return `void`
- Not `static`
- The enclosing class becomes managed automatically (no `@Component` required), same idea as `@OnEvent` / `@Command`

## Order

- **Enable:** after every component is constructed and `@Wire` fields are set, BukkitKit calls `@OnEnable` methods in **dependency order** (dependencies first). Within one class, declaration order.
- **Disable:** `@OnDisable` runs in **reverse** order (within a class: reverse declaration order), then BukkitKit unbinds

So if `PlayerManager` depends on `PlayerRepository`, the repository enables first and disables last.

## Timeline

1. BukkitKit constructs and wires all components
2. `@OnEnable` methods run
3. `@Scheduled` tasks, `@OnEvent` listeners, and `@Command` / `@TabComplete` handlers are registered
4. On stop: schedules/listeners cleanup, then `@OnDisable`, then unbind

Put “start this subsystem” logic on the component that owns it. The `@BukkitKit` marker stays metadata-only.

## Next

[Scheduled tasks](./scheduling) — repeat work on a timer without manual `runTaskTimer` glue.
