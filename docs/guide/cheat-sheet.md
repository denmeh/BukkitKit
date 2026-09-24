# Annotation cheat sheet

Quick reference after you have finished the tutorial.

## `@BukkitKit`

On a plain public top-level class (do **not** extend `JavaPlugin`). Metadata becomes `plugin.yml`; BukkitKit generates a `YourClass_BukkitKit` `JavaPlugin` entry and points `main` at it.

Required: `name`, `version`, `apiVersion`.

Optional:

| Attribute | `plugin.yml` |
|-----------|--------------|
| `description` | `description` |
| `authors` | `authors` |
| `website` | `website` |
| `prefix` | `prefix` |
| `load` (`POSTWORLD` \| `STARTUP`) | `load` (`POSTWORLD` omitted) |
| `depend` | `depend` |
| `softDepend` | `softdepend` |
| `loadBefore` | `loadbefore` |
| `provides` | `provides` |
| `permissions` (`@Permission`) | `permissions` |

### `@Permission` (inside `@BukkitKit`)

`name` (required), `description`, `defaultValue` (`TRUE` \| `FALSE` \| `OP` \| `NOT_OP`), `children`.

## `@Component`

On a concrete class. One singleton instance per plugin. Use a public no-arg constructor when the class has `@Wire` fields.

## `@Wire`

On an instance field of a **managed** class (`@Component`, or a class with `@OnEvent` / `@Command` / …). Injects another managed type (`@Component`, `@Config`) or a [built-in](./built-ins) (`JavaPlugin`, `Logger`, …). Not `static` or `final`. Not allowed on the `@BukkitKit` marker or on `@Config` classes themselves.

## `@OnEvent`

On a `public void` method with one Bukkit `Event` parameter. Registers a listener. Optional `priority`, `ignoreCancelled`. Class becomes managed even without `@Component`.

## `@Command`

On a `public` method. Declares a plugin command (also written to `plugin.yml`) and registers the executor.

- Required: `name`
- Optional: `description`, `usage`, `aliases`, `permission`, `permissionMessage`
- Return: `void` or `boolean` (`false` shows usage)
- Signatures: `(CommandSender)`, `(CommandSender, String[])`, `(CommandSender, String label, String[])`
- Class becomes managed even without `@Component`

## `@TabComplete`

On a `public` method returning `List<String>`. Value is the matching `@Command` name.

Signatures: `(CommandSender, String[])`, `(CommandSender, String alias, String[])`.

## `@Config`

On a concrete public class with a public no-arg constructor. Defines typed settings; BukkitKit creates one singleton and (when `persistent`, default `true`) binds it to a YAML file under the plugin data folder.

- Field name = YAML key
- Fields must be public, non-`static`, non-`final`
- Leaf types: `boolean`/`Boolean`, `int`/`Integer`, `long`/`Long`, `double`/`Double`, `float`/`Float`, `String`, `List<String>`
- Nested public classes (static nested or top-level, public no-arg ctor) become YAML sections
- Optional: `file` (default `config.yml`), `persistent` (`false` = Java defaults only, no disk)
- Do not mix with `@Component`, `@Wire`, events, commands, schedules, or lifecycle
- Do not nest another `@Config` type as a field
- Inject with `@Wire` into other managed classes

## `@OnEnable` / `@OnDisable`

On a `public void` no-arg method. Runs on plugin enable / disable. Dependency order on enable; reverse on disable. Class becomes managed even without `@Component`.

## `@Scheduled`

On a `public` method of a managed class. `every`, `delay`, `unit` (`TICKS` \| `SECONDS` \| `MINUTES`), `async`. Return `void` to repeat until disable, or `boolean` (`false` cancels). `every = -1` for one-shot.

## Built-in injectable types

`Plugin` · `JavaPlugin` · `Server` · `Logger` · `PluginManager` · `ServicesManager` · `BukkitScheduler` · `KitScheduler`
