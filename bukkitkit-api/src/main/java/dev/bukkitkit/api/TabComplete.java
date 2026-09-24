package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public method as the tab-completer for a {@link Command}.
 * <p>
 * The method must return {@code List<String>} (or a raw {@code List}) and use one of:
 * <ul>
 *   <li>{@code (CommandSender sender, String[] args)}</li>
 *   <li>{@code (CommandSender sender, String alias, String[] args)}</li>
 * </ul>
 *
 * <pre>{@code
 * @TabComplete("hello")
 * public List<String> helloTab(CommandSender sender, String[] args) {
 *     return List.of("world", "friend");
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface TabComplete {

    /** Primary command {@link Command#name()} this completer belongs to. */
    String value();
}
