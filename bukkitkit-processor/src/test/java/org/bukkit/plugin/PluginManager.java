package org.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public interface PluginManager {

    void registerEvent(
            Class<? extends Event> event,
            Listener listener,
            EventPriority priority,
            EventExecutor executor,
            Plugin plugin,
            boolean ignoreCancelled);
}
