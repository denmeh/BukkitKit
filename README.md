# BukkitKit

Lightweight compile-time dependency injection for [Paper](https://papermc.io/) / Bukkit plugins.

Wire plugins, components, events, and schedules with annotations — the processor builds the graph at compile time so missing wiring fails the build, not the server.

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
| `bukkitkit-core` | Bootstrap loading |
| `bukkitkit-processor` | Compile-time codegen |
| `bukkitkit-paper` | Paper runtime + shaded processor |
| `bukkitkit-demo` | Example plugin |

## Quick start

```bash
just demo    # build the shaded demo JAR
just verify  # full reactor tests
```

See the [docs](https://denmeh.github.io/BukkitKit/) for setup, the step-by-step tutorial, and the annotation cheat sheet.
