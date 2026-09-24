package dev.bukkitkit.paper;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Plugin-scoped scheduler with main/async support and bulk cancel on disable.
 */
public final class KitScheduler {

    private final JavaPlugin plugin;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public KitScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Repeating task. {@code tick} returning {@code false} cancels this task.
     */
    public synchronized void runRepeating(
            long delayTicks,
            long periodTicks,
            boolean async,
            BooleanSupplier tick) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                boolean cont;
                try {
                    cont = tick.getAsBoolean();
                } catch (RuntimeException ex) {
                    plugin.getLogger().severe("BukkitKit: scheduled task failed: " + ex.getMessage());
                    ex.printStackTrace();
                    cancel();
                    return;
                }
                if (!cont) {
                    cancel();
                }
            }
        };
        BukkitTask task = async
                ? runnable.runTaskTimerAsynchronously(plugin, delayTicks, periodTicks)
                : runnable.runTaskTimer(plugin, delayTicks, periodTicks);
        tasks.add(task);
    }

    /**
     * One-shot task after {@code delayTicks}.
     */
    public synchronized void runLater(long delayTicks, boolean async, Runnable action) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (RuntimeException ex) {
                    plugin.getLogger().severe("BukkitKit: scheduled task failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };
        BukkitTask task = async
                ? runnable.runTaskLaterAsynchronously(plugin, delayTicks)
                : runnable.runTaskLater(plugin, delayTicks);
        tasks.add(task);
    }

    /**
     * Cancels every task started through this scheduler.
     */
    public synchronized void cancelAll() {
        for (BukkitTask task : tasks) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
                // best-effort
            }
        }
        tasks.clear();
    }
}
