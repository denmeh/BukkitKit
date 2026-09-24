# 4. Built-in services

Goal: inject common Paper objects instead of reaching for `Bukkit.getServer()` or `plugin.getLogger()` everywhere.

## What BukkitKit provides

When your plugin enables, BukkitKit can wire these types into `@Wire` fields:

| Type | Typical use |
|------|-------------|
| `org.bukkit.plugin.java.JavaPlugin` | Plugin handle (usual choice) |
| `org.bukkit.plugin.Plugin` | Generic plugin handle |
| `org.bukkit.Server` | Worlds, players, broadcast, … |
| `java.util.logging.Logger` | Logging |
| `org.bukkit.plugin.PluginManager` | Register/query plugins (advanced) |
| `org.bukkit.plugin.ServicesManager` | Bukkit services API |
| `org.bukkit.scheduler.BukkitScheduler` | Manual scheduling if you need it |
| `dev.bukkitkit.paper.KitScheduler` | BukkitKit’s scheduler helper |

You still use the normal Paper API on these objects. BukkitKit only injects the reference.

## Example

```java
@Component
public final class PlayerRepository {

    @Wire
    private Logger logger;

    public void warmUp() {
        logger.info("PlayerRepository ready");
    }
}
```

```java
@Component
public final class PlayerManager {

    @Wire
    private PlayerRepository repository;
    @Wire
    private Server server;
    @Wire
    private JavaPlugin plugin;

    public String describe() {
        repository.warmUp();
        return "PlayerManager wired for plugin '" + plugin.getName()
                + "' on server '" + server.getName() + "'";
    }
}
```

Prefer injecting `Logger` or `JavaPlugin` over static `Bukkit` calls when you can — it keeps classes easier to test and reason about.

## What is not built-in?

Anything else must be one of:

- Another `@Component` in your plugin, or
- Created inside a component yourself (plain Java)

BukkitKit will not invent a `MySqlClient` unless you mark that class as a component (or construct it manually).

## Next

[Events](./events) — respond to players joining, chatting, and other Bukkit events.
