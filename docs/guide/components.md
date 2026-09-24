# 2. Components

Goal: put plugin logic in a dedicated class instead of stuffing everything into `JavaPlugin`.

## Why split things up?

As plugins grow, `onEnable` becomes a dumping ground: load configs, create managers, register listeners, start tasks. Components let you name a piece of behavior (`PlayerManager`, `WarpService`, …) and keep the plugin class thin.

## Create a `@Component`

```java
package com.example.hello;

import dev.bukkitkit.api.Component;

@Component
public final class GreetingService {

    public String message() {
        return "Welcome to the server!";
    }
}
```

Rules for now:

- Annotate the class with `@Component`
- Give it a **public no-arg constructor** (the default one is fine if you declare no other constructors)
- Keep it a concrete class (not an interface)

BukkitKit finds this class at compile time and creates **one shared instance** (a singleton) when the plugin enables.

## Use it from the plugin

Ask the plugin for the component with `@Wire`:

```java
package com.example.hello;

import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Wire;
import org.bukkit.plugin.java.JavaPlugin;

@BukkitKit
public final class HelloPlugin extends JavaPlugin {

    @Wire
    private GreetingService greetings;

    @Override
    public void onEnable() {
        getLogger().info(greetings.message());
    }
}
```

Important details:

- The field is filled **before** your `onEnable` body runs
- Do not use `static` or `final` on `@Wire` fields
- You do not call `new GreetingService()` yourself

If `GreetingService` is missing `@Component`, or the processor is not configured, the build or enable path will not wire correctly — fix setup first.

## Mental model

```
Server starts plugin
  → BukkitKit creates each @Component once
  → BukkitKit fills @Wire fields on the plugin
  → your onEnable() runs
```

## Next

[Wiring dependencies](./wiring) — let components depend on each other.
