# Introduction

BukkitKit is a small API for writing [Paper](https://papermc.io/) (Minecraft) plugins.

If you have written a plugin before, you know the usual pattern: extend `JavaPlugin`, register listeners by hand, schedule tasks with `BukkitScheduler`, and pass `this` (the plugin) around everywhere. That works, but it gets messy as the plugin grows.

BukkitKit gives you a clearer way to structure the same work:

1. Mark a plain class with `@BukkitKit` (name, version, api version — no `JavaPlugin` or `plugin.yml` by hand)
2. Put game logic in small `@Component` classes
3. Ask for what you need with `@Wire`
4. Optionally use `@OnEvent`, `@OnEnable` / `@OnDisable`, and `@Scheduled` for listeners, startup hooks, and repeating work

You still write normal Paper/Bukkit code. BukkitKit does not replace the Minecraft API — it organizes how your classes connect.

## Who this guide is for

You should already know a little Java and have a rough idea of what a Paper plugin is (plugins, events, `plugin.yml`). You do **not** need to know dependency injection, annotation processors, or frameworks like Guice/Spring.

## How to read this guide

Follow the pages in order. Each step builds on the last:

| Step | You learn |
|------|-----------|
| [Setup](./setup) | Add BukkitKit to a Maven project |
| [Your first plugin](./your-first-plugin) | `@BukkitKit` on a blank plugin |
| [Components](./components) | Split logic into `@Component` classes |
| [Wiring](./wiring) | Connect classes with `@Wire` |
| [Built-ins](./built-ins) | Inject `Server`, `Logger`, and friends |
| [Events](./events) | Handle joins, chats, etc. with `@OnEvent` |
| [Lifecycle](./lifecycle) | Run code on enable / disable |
| [Scheduling](./scheduling) | Repeat work with `@Scheduled` |

When you need a quick reminder later, use the [cheat sheet](./cheat-sheet).

## What “compile-time” means (briefly)

When you build your plugin, BukkitKit’s annotation processor reads your annotations and generates bootstrap code. If something cannot be wired (for example a missing dependency), the **build fails** instead of crashing when the server starts. You do not call that generated code yourself — it runs automatically when your plugin enables.
