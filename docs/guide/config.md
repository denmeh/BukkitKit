# 9. Config

Goal: define plugin settings in Java and let BukkitKit create / load `config.yml` for you.

## The idea

You write defaults as public fields on a `@Config` class. BukkitKit:

1. Creates one singleton instance (Java field initializers are the defaults)
2. On first **plugin enable** (on the server), writes `plugins/<plugin>/config.yml` from those defaults
3. On later enables, loads values from the file into the same object
4. Fills in any **missing** keys with the Java defaults and saves them back

Operators edit YAML. Authors stay in typed Java. Wire the config like any other managed type.

Building the JAR (`just demo`) does **not** create the YAML — that only happens when Paper loads and enables the plugin.

## Example

```java
package com.example.hello;

import dev.bukkitkit.api.Config;
import java.util.List;

@Config
public final class HelloConfig {

    public String welcomeMessage = "Welcome!";
    public boolean joinMessageEnabled = true;
    public int maxHomes = 3;
    public List<String> motdLines = List.of("Have fun!");
    public Database database = new Database();

    public HelloConfig() {
    }

    public static final class Database {
        public String host = "localhost";
        public int port = 3306;

        public Database() {
        }
    }
}
```

```java
package com.example.hello;

import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.api.Wire;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinListener {

    @Wire
    private HelloConfig config;

    public JoinListener() {
    }

    @OnEvent
    public void onJoin(PlayerJoinEvent event) {
        if (config.joinMessageEnabled) {
            event.getPlayer().sendMessage(config.welcomeMessage);
        }
    }
}
```

After the first enable, look under the server:

```text
plugins/
  Hello/
    config.yml
```

```yaml
welcomeMessage: Welcome!
joinMessageEnabled: true
maxHomes: 3
motdLines:
  - Have fun!
database:
  host: localhost
  port: 3306
```

## Nested sections

A public field whose type is another public class (static nested or top-level) becomes a YAML section. Nested types follow the same field rules as the root `@Config` class: public non-`final` fields, supported leaf types, and a public no-arg constructor.

You can nest sections arbitrarily deep. Do **not** put another `@Config` type as a field — use a plain class for sections, or `@Wire` a separate `@Config` if you want a second file.
## How bind works

At enable, generated bootstrap does roughly:

```java
this.helloConfig = new HelloConfig();           // Java defaults
KitConfig.bind(plugin, this.helloConfig, "config.yml");
```

| Situation | Result |
|-----------|--------|
| File missing | Create from Java defaults |
| File present | Read matching keys into fields |
| Key missing in file | Write Java default for that key, save |
| Key has wrong type | Plugin enable fails with a `BukkitKit:` error |

There is no hot-reload API yet — restart (or re-enable) the plugin to re-read the file.

## Rules

- Public class, public no-arg constructor
- At least one **public**, non-`static`, non-`final` field (on that class; superclass fields are ignored)
- Field name = YAML key
- Supported leaf types: `boolean`/`Boolean`, `int`/`Integer`, `long`/`Long`, `double`/`Double`, `float`/`Float`, `String`, `List<String>`
- Nested public classes (static nested or top-level) become YAML sections — same field rules, public no-arg constructor
- Do **not** mix with `@Component`, `@Wire`, events, commands, schedules, or lifecycle hooks
- Do **not** nest another `@Config` type as a field (plain class for sections; separate `@Config` + `@Wire` for another file)
- `file` must be relative to the plugin data folder (no absolute paths, no `..`)
- Two `@Config` classes cannot share the same `file`

## Options

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `file` | `"config.yml"` | Relative path under the plugin data folder |
| `persistent` | `true` | When `false`, keep Java defaults and never touch disk |

```java
@Config(file = "messages.yml")
public final class Messages { /* … */ }

@Config(file = "data/settings.yml") // subfolders are created as needed
public final class Settings { /* … */ }

@Config(persistent = false) // tests / no operator settings
public final class FixedSettings { /* … */ }
```

## Next

[Plugin metadata](./plugin-yml) — depend, softdepend, permissions, and the rest of `plugin.yml`.
