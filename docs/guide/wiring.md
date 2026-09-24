# 3. Wiring dependencies

Goal: let one component use another without manual `new` and constructors full of arguments.

## The problem

Suppose `PlayerManager` needs a `PlayerRepository`:

```java
// Without BukkitKit you might write:
PlayerRepository repo = new PlayerRepository();
PlayerManager manager = new PlayerManager(repo);
```

That is fine for two classes. With ten services and shared platform objects (`Server`, logger, …), construction order and “who creates what” become hard to track.

## Wire component → component

```java
@Component
public final class PlayerRepository {

    public void warmUp() {
        // load data, prepare caches, ...
    }
}
```

```java
@Component
public final class PlayerManager {

    @Wire
    private PlayerRepository repository;

    public String describe() {
        repository.warmUp();
        return "PlayerManager ready";
    }
}
```

```java
@BukkitKit
public final class HelloPlugin extends JavaPlugin {

    @Wire
    private PlayerManager players;

    @Override
    public void onEnable() {
        getLogger().info(players.describe());
    }
}
```

BukkitKit:

1. Creates `PlayerRepository`
2. Creates `PlayerManager` and sets `repository`
3. Sets `players` on the plugin
4. Then runs your `onEnable`

Creation order follows dependencies. If there is a cycle (A needs B and B needs A), the processor fails the build so you can untangle it.

## Wire the plugin into a component

Components often need the plugin instance (for loggers, data folder, registering things):

```java
@Component
public final class PlayerManager {

    @Wire
    private HelloPlugin plugin;

    @Wire
    private PlayerRepository repository;

    public String describe() {
        return "wired for plugin '" + plugin.getName() + "'";
    }
}
```

You can also `@Wire` the plugin type on the plugin class itself (same instance as `this`):

```java
@BukkitKit
public final class HelloPlugin extends JavaPlugin {

    @Wire
    private HelloPlugin self;

    @Wire
    private PlayerManager players;
}
```

## Rules to remember

| Do | Don’t |
|----|--------|
| `@Wire` on instance fields | `@Wire` on `static` or `final` fields |
| Public no-arg constructor on components that use `@Wire` | Hide the only constructor or make it package-private only |
| Depend on other `@Component` types or [built-ins](./built-ins) | Expect arbitrary `new`-only classes to appear magically |

## Next

[Built-in services](./built-ins) — inject `Server`, `Logger`, and other Paper objects BukkitKit provides for you.
