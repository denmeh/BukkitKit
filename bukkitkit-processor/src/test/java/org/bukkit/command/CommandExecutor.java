package org.bukkit.command;

/** Test classpath stub. */
@FunctionalInterface
public interface CommandExecutor {

    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
