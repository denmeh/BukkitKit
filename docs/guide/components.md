# 2. Components

Goal: put plugin logic in a dedicated class instead of one giant entry point.

## Why split things up?

As plugins grow, enable logic becomes a dumping ground: load configs, create managers, register listeners and commands, start tasks. Components let you name a piece of behavior (`PlayerManager`, `WarpService`, …) and keep concerns separate.

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

## Use it from another component

Wire it into a component that runs on enable:

```java
package com.example.hello;

import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.OnEnable;
import dev.bukkitkit.api.Wire;
import java.util.logging.Logger;

@Component
public final class Startup {

    @Wire
    private GreetingService greetings;
    @Wire
    private Logger logger;

    @OnEnable
    public void start() {
        logger.info(greetings.message());
    }
}
```

Important details:

- `@Wire` fields are filled **before** `@OnEnable` runs
- Do not use `static` or `final` on `@Wire` fields
- You do not call `new GreetingService()` yourself

If `GreetingService` is missing `@Component`, or the processor is not configured, the build fails — fix setup first.

## Mental model

```
Server starts plugin
  → BukkitKit creates each managed class once
  → BukkitKit fills @Wire fields
  → @OnEnable methods run (dependency order)
  → events, commands, and schedules are registered
```

Classes can also become managed without `@Component` when they only host `@OnEvent`, `@Command`, `@TabComplete`, `@OnEnable`, or `@OnDisable` methods.

## Next

[Wiring dependencies](./wiring) — let components depend on each other.
