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

BukkitKit:

1. Creates `PlayerRepository`
2. Creates `PlayerManager` and sets `repository`
3. Runs `@OnEnable` methods in dependency order

Creation order follows dependencies. If there is a cycle (A needs B and B needs A), the processor fails the build so you can untangle it.

## Wire the plugin into a component

Components often need the plugin instance (for loggers, data folder, registering things). Inject a built-in — not the `@BukkitKit` marker (that class has no Paper methods in source, so the IDE cannot see `getLogger()`):

```java
@Component
public final class PlayerManager {

    @Wire
    private JavaPlugin plugin;

    @Wire
    private PlayerRepository repository;

    public String describe() {
        return "wired for plugin '" + plugin.getName() + "'";
    }
}
```

Or inject `Logger` when you only need logging.

## Rules to remember

| Do | Don’t |
|----|--------|
| `@Wire` on instance fields of components | `@Wire` on the `@BukkitKit` marker |
| `@Wire` `JavaPlugin` / `Logger` / other built-ins | `@Wire` the `@BukkitKit` marker type |
| Instance `@Wire` fields only | `@Wire` on `static` or `final` fields |
| Public no-arg constructor on components that use `@Wire` | Hide the only constructor or make it package-private only |
| Depend on other `@Component` types or [built-ins](./built-ins) | Expect arbitrary `new`-only classes to appear magically |

## Next

[Built-in services](./built-ins) — inject `Server`, `Logger`, and other Paper objects BukkitKit provides for you.
