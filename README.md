# BukkitKit

An API for [Paper](https://papermc.io/) plugins.

If you have written a plugin before, you know the usual pattern: extend `JavaPlugin`, register listeners and commands by hand, schedule tasks with `BukkitScheduler`, and pass `this` around everywhere. That works, but it gets messy as the plugin grows.

BukkitKit gives you a clearer way to structure the same work:

1. Mark a plain class with `@BukkitKit` — name, version, api version (plus depend, permissions, …); no `JavaPlugin` or `plugin.yml` by hand
2. Put game logic in small `@Component` classes
3. Ask for what you need with `@Wire`
4. Optionally use `@OnEvent`, `@Command`, `@Config`, `@OnEnable` / `@OnDisable`, and `@Scheduled` for listeners, commands, settings, startup hooks, and repeating work

You still write normal Paper/Bukkit code. BukkitKit does not replace the Minecraft API — it organizes how your classes connect.

## Why BukkitKit

- **Start simple** — One annotated marker class is enough to begin. BukkitKit generates the `JavaPlugin` entry and `plugin.yml`. Add features only when you need them.
- **Build in pieces** — Split your plugin into small components. BukkitKit connects them for you at compile time.
- **Less Bukkit glue** — Register listeners, commands, schedulers, and startup hooks with annotations instead of manual boilerplate.
- **Fail at build time** — Missing wiring fails the build, not the server at runtime.

## Documentation

**Guides & tutorial:** [https://denmeh.github.io/BukkitKit/](https://denmeh.github.io/BukkitKit/)

Local preview:

```bash
just docs
```

## Modules

| Module | Role |
|--------|------|
| `bukkitkit-api` | Public annotations (`@BukkitKit`, `@Component`, `@Wire`, …) |
| `bukkitkit-core` | Bootstrap loading + field wiring |
| `bukkitkit-processor` | Compile-time Filer codegen |
| `bukkitkit-paper` | Paper runtime + shaded processor |
| `bukkitkit-demo` | Example plugin |

## Quick start

```bash
just demo    # build the shaded demo JAR
just verify  # full reactor tests
```

See the [docs](https://denmeh.github.io/BukkitKit/) for setup, the step-by-step tutorial, and the annotation cheat sheet.
