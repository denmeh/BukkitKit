# Annotation cheat sheet

Quick reference after you have finished the tutorial.

## `@BukkitKit`

On a `JavaPlugin` subclass. Opts the plugin into BukkitKit bootstrap (wire fields, start components, register events/schedules).

## `@Component`

On a concrete class. One singleton instance per plugin. Use a public no-arg constructor when the class has `@Wire` fields.

## `@Wire`

On an instance field of a plugin or component. Injects another component, the plugin, or a [built-in](./built-ins). Not `static` or `final`.

## `@OnEvent`

On a `public void` method with one Bukkit `Event` parameter. Registers a listener. Optional `priority`, `ignoreCancelled`. Class becomes managed even without `@Component`.

## `Lifecycle`

Interface for components: `onEnable()` / `onDisable()`. Dependency order on enable; reverse on disable.

## `@Scheduled`

On a `public` method of a `@Component`. `every`, `delay`, `unit` (`TICKS` | `SECONDS` | `MINUTES`), `async`. Return `void` to repeat until disable, or `boolean` (`false` cancels). `every = -1` for one-shot.

## Built-in injectable types

Plugin class · `Plugin` · `JavaPlugin` · `Server` · `Logger` · `PluginManager` · `ServicesManager` · `BukkitScheduler` · `KitScheduler`
