# 9. Plugin metadata (`plugin.yml`)

Goal: keep `plugin.yml` complete from `@BukkitKit` and `@Command` — no hand-edited YAML for the common fields.

## What BukkitKit writes

| Source | `plugin.yml` keys |
|--------|-------------------|
| `@BukkitKit` required | `name`, `version`, `main`, `api-version` |
| `@BukkitKit` optional | `description`, `authors`, `website`, `prefix`, `load`, `depend`, `softdepend`, `loadbefore`, `provides`, `permissions` |
| `@Command` | `commands` |

`main` always points at the generated `YourClass_BukkitKit` entry. `load: POSTWORLD` is the Bukkit default and is **omitted** from the file; set `load = PluginLoad.STARTUP` when you need early load.

## Example

```java
import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Permission;
import dev.bukkitkit.api.PermissionDefault;
import dev.bukkitkit.api.PluginLoad;

@BukkitKit(
        name = "Hello",
        version = "1.0.0",
        apiVersion = "1.21",
        description = "Says hello",
        authors = {"you"},
        website = "https://example.com",
        prefix = "Hello",
        load = PluginLoad.POSTWORLD,
        depend = {"Vault"},
        softDepend = {"WorldGuard"},
        loadBefore = {"SomeOtherPlugin"},
        provides = {"HelloApi"},
        permissions = {
                @Permission(
                        name = "hello.use",
                        description = "Use /hello",
                        defaultValue = PermissionDefault.TRUE,
                        children = {"hello.admin"})
        }
)
public final class HelloPlugin {
}
```

Generated permissions look like:

```yaml
permissions:
  hello.use:
    description: Use /hello
    default: true
    children:
      hello.admin: true
```

`PermissionDefault`: `TRUE`, `FALSE`, `OP`, `NOT_OP` (writes as `true` / `false` / `op` / `not op`).

## Commands

Command entries come from `@Command` methods — see [Commands](./commands). You do not list them again on `@BukkitKit`.

## Where to go next

You now have the full starter toolkit:

1. `@BukkitKit` marker (+ generated `JavaPlugin` entry and `plugin.yml`)
2. `@Component` services
3. `@Wire` between them and built-ins
4. `@OnEvent` for listeners
5. `@OnEnable` / `@OnDisable` for start/stop
6. `@Scheduled` for timers
7. `@Command` / `@TabComplete` for commands
8. Full `plugin.yml` metadata (deps, permissions, …)

Use the [cheat sheet](./cheat-sheet) as a quick reference, and look at `bukkitkit-demo` in the repository for a complete example.
