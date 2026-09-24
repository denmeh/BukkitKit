package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public method as a plugin command handler.
 * <p>
 * The enclosing class becomes a managed singleton automatically (no {@link Component}
 * required). BukkitKit writes the command into {@code plugin.yml} and registers the
 * executor at bootstrap (no {@code getCommand(...).setExecutor(...)} required).
 * <p>
 * Supported signatures (return {@code void} or {@code boolean}):
 * <ul>
 *   <li>{@code (CommandSender sender)}</li>
 *   <li>{@code (CommandSender sender, String[] args)}</li>
 *   <li>{@code (CommandSender sender, String label, String[] args)}</li>
 * </ul>
 * A {@code void} return is treated as {@code true} (command handled). Returning
 * {@code false} shows the usage string from {@link #usage()}.
 *
 * <pre>{@code
 * public final class HelloCommands {
 *     @Command(name = "hello", description = "Say hello", usage = "/hello [name]")
 *     public void hello(CommandSender sender, String[] args) {
 *         sender.sendMessage("Hello!");
 *     }
 * }
 * }</pre>
 *
 * @see TabComplete
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Command {

    /** Primary command name (as typed after {@code /}). */
    String name();

    /** {@code plugin.yml} description; omitted when empty. */
    String description() default "";

    /** {@code plugin.yml} usage; shown when the handler returns {@code false}. */
    String usage() default "";

    /** Alternate names. */
    String[] aliases() default {};

    /** Required permission node; omitted when empty. */
    String permission() default "";

    /** Message when the sender lacks {@link #permission()}; omitted when empty. */
    String permissionMessage() default "";
}
