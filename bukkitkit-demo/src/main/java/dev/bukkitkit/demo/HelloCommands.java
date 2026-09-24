package dev.bukkitkit.demo;

import dev.bukkitkit.api.Command;
import dev.bukkitkit.api.TabComplete;
import dev.bukkitkit.api.Wire;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Command-only class — no {@code @Component}; {@code @Command} makes it a managed singleton.
 */
public final class HelloCommands {

    @Wire
    private Logger logger;

    public HelloCommands() {
    }

    @Command(
            name = "hello",
            description = "Say hello",
            usage = "/hello [name]",
            aliases = {"hi"},
            permission = "bukkitkitdemo.hello")
    public void hello(CommandSender sender, String[] args) {
        String target = args.length > 0 ? args[0] : "world";
        sender.sendMessage("Hello, " + target + "!");
        logger.info("hello invoked for " + target);
    }

    @TabComplete("hello")
    public List<String> helloTab(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        for (String option : List.of("world", "friend", "server")) {
            if (option.regionMatches(true, 0, args[0], 0, args[0].length())) {
                suggestions.add(option);
            }
        }
        return suggestions;
    }
}
