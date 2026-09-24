# 5. Events

Goal: handle Bukkit events without implementing `Listener` and calling `registerEvents` yourself.

## The usual Paper way

```java
public final class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // ...
    }
}

// somewhere in onEnable:
getServer().getPluginManager().registerEvents(new JoinListener(), this);
```

## The BukkitKit way

```java
package com.example.hello;

import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.api.Wire;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Logger;

public final class JoinListener {

    @Wire
    private Logger logger;

    @OnEvent
    public void onJoin(PlayerJoinEvent event) {
        logger.info("Player joined: " + event.getPlayer().getName());
    }
}
```

Notes:

- **No** `@Component` required — `@OnEvent` is enough to make the class a managed singleton
- **No** `implements Listener` required — BukkitKit registers the handler for you
- The method must be `public`, return `void`, and take **exactly one** parameter (a Bukkit `Event` subtype)
- You can still `@Wire` dependencies into the listener class

If the class is already a `@Component`, it stays a single instance; event methods on it are registered once.

## Priority and cancelled events

```java
@OnEvent(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onChat(AsyncPlayerChatEvent event) {
    // ...
}
```

`EventPriority` here mirrors Bukkit’s priorities (`LOWEST` … `MONITOR`).  
`ignoreCancelled = true` skips events that another plugin already cancelled.

## When to use a component vs an event-only class

| Use | When |
|-----|------|
| Class with only `@OnEvent` | Pure listeners (join, quit, interact) |
| `@Component` + `@OnEvent` | A service that both holds state and listens |

## Next

[Lifecycle](./lifecycle) — run setup and teardown with `@OnEnable` / `@OnDisable`.
