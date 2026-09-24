# 7. Scheduled tasks

Goal: run repeating or delayed work on a component with `@Scheduled`.

## Basic repeating task

```java
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.ScheduleUnit;
import dev.bukkitkit.api.Scheduled;
import dev.bukkitkit.api.Wire;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public final class PlayerManager {

    @Wire
    private JavaPlugin plugin;

    private int ticks;

    @Scheduled(every = 30, unit = ScheduleUnit.SECONDS)
    public void heartbeat() {
        ticks++;
        plugin.getLogger().info("heartbeat #" + ticks);
    }
}
```

The method must be `public`. By default it runs on the **main server thread**, like most Bukkit APIs expect.

Units: `TICKS`, `SECONDS`, `MINUTES`.

## Delay before the first run

```java
@Scheduled(every = 1, unit = ScheduleUnit.MINUTES, delay = 10)
public void autosave() {
    // waits 10 minutes, then every 1 minute
}
```

`delay` uses the same `unit` as `every`.

## One-shot

Set `every` to `-1` so only `delay` applies:

```java
@Scheduled(every = -1, delay = 5, unit = ScheduleUnit.SECONDS)
public void afterStart() {
    plugin.getLogger().info("ran once, five seconds after enable");
}
```

## Stop a repeating task from inside

Return `boolean` instead of `void`. Return `false` to cancel that task:

```java
@Scheduled(every = 10, unit = ScheduleUnit.SECONDS)
public boolean syncUntilIdle() {
    if (!dirty) {
        return false; // stop scheduling
    }
    save();
    return true; // keep going
}
```

## Async

```java
@Scheduled(every = 1, unit = ScheduleUnit.MINUTES, async = true)
public void heavyBackup() {
    // off the main thread — do not touch most Bukkit API here
}
```

Only use `async = true` when you know the work is thread-safe. Prefer the main thread (the default) for anything that talks to players, worlds, or inventories.

## Cleanup

When the plugin disables, BukkitKit cancels the schedules it started. You do not need to keep task IDs yourself for `@Scheduled` methods.

## Where to go next

[Commands](./commands) — register `/commands` with `@Command` instead of `plugin.yml` + `setExecutor`.
