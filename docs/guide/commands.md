# 8. Commands

Goal: declare plugin commands without writing `plugin.yml` by hand or calling `getCommand(...).setExecutor(...)`.

## The usual Paper way

```java
// plugin.yml
commands:
  hello:
    description: Say hello
    usage: /hello [name]
    permission: demo.hello

// Java
getCommand("hello").setExecutor((sender, command, label, args) -> {
    sender.sendMessage("Hello!");
    return true;
});
```

## The BukkitKit way

```java
package com.example.hello;

import dev.bukkitkit.api.Command;
import dev.bukkitkit.api.TabComplete;
import dev.bukkitkit.api.Wire;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.logging.Logger;

public final class HelloCommands {

    @Wire
    private Logger logger;

    @Command(
            name = "hello",
            description = "Say hello",
            usage = "/hello [name]",
            aliases = {"hi"},
            permission = "demo.hello")
    public void hello(CommandSender sender, String[] args) {
        String target = args.length > 0 ? args[0] : "world";
        sender.sendMessage("Hello, " + target + "!");
        logger.info("hello → " + target);
    }

    @TabComplete("hello")
    public List<String> helloTab(CommandSender sender, String[] args) {
        return List.of("world", "friend");
    }
}
```

Notes:

- **No** `@Component` required — `@Command` (or `@TabComplete`) is enough to make the class a managed singleton
- BukkitKit writes the `commands:` section in `plugin.yml` and registers the executor at enable
- Method must be `public` and return `void` or `boolean` (`false` shows the usage string)
- Supported parameter lists:
  - `(CommandSender sender)`
  - `(CommandSender sender, String[] args)`
  - `(CommandSender sender, String label, String[] args)`

Declare the matching permission on `@BukkitKit` if you want it in `plugin.yml` too — see [plugin metadata](./plugin-yml).

## Tab completion

`@TabComplete("commandName")` must match an existing `@Command(name = "...")`. Signatures:

- `(CommandSender sender, String[] args)`
- `(CommandSender sender, String alias, String[] args)`

Return type must be `List<String>`.

## Next

[Config](./config) — typed settings in Java with auto-generated `config.yml`.
