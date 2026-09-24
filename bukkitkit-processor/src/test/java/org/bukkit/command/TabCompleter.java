package org.bukkit.command;

import java.util.List;

/** Test classpath stub. */
@FunctionalInterface
public interface TabCompleter {

    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);
}
