# Annotation cheat sheet

Quick reference after you have finished the tutorial.

## `@BukkitKit`

On a plain public top-level class (do **not** extend `JavaPlugin`). Metadata becomes `plugin.yml`; BukkitKit generates a `YourClass_BukkitKit` `JavaPlugin` entry and points `main` at it.

Required: `name`, `version`, `apiVersion`. Optional: `description`, `authors`.

## `@Component`

On a concrete class. One singleton instance per plugin. Use a public no-arg constructor when the class has `@Wire` fields.

## `@Wire`

On an instance field of a **component**. Injects another component or a [built-in](./built-ins) (`JavaPlugin`, `Logger`, …). Not `static` or `final`. Not allowed on the `@BukkitKit` marker (and the marker type itself is not injectable).

## `@OnEvent`

On a `public void` method with one Bukkit `Event` parameter. Registers a listener. Optional `priority`, `ignoreCancelled`. Class becomes managed even without `@Component`.

## `@OnEnable` / `@OnDisable`

On a `public void` no-arg method. Runs on plugin enable / disable. Dependency order on enable; reverse on disable. Class becomes managed even without `@Component`.

## `@Scheduled`

On a `public` method of a managed class. `every`, `delay`, `unit` (`TICKS` | `SECONDS` | `MINUTES`), `async`. Return `void` to repeat until disable, or `boolean` (`false` cancels). `every = -1` for one-shot.

## Built-in injectable types

`Plugin` · `JavaPlugin` · `Server` · `Logger` · `PluginManager` · `ServicesManager` · `BukkitScheduler` · `KitScheduler`
