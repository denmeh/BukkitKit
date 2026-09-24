package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a plain class as the BukkitKit plugin entry. BukkitKit turns it into a
 * {@code JavaPlugin} at compile time and writes {@code plugin.yml} from the metadata.
 *
 * <pre>{@code
 * @BukkitKit(
 *     name = "Hello",
 *     version = "1.0.0",
 *     apiVersion = "1.21"
 * )
 * public final class HelloPlugin {
 * }
 * }</pre>
 *
 * Put startup logic on components with {@link OnEnable} / {@link OnDisable}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface BukkitKit {

    /** {@code plugin.yml} {@code name}. */
    String name();

    /** {@code plugin.yml} {@code version}. */
    String version();

    /** {@code plugin.yml} {@code api-version} (e.g. {@code "1.21"}). */
    String apiVersion();

    /** {@code plugin.yml} {@code description}; omitted when empty. */
    String description() default "";

    /** {@code plugin.yml} {@code authors}; omitted when empty. */
    String[] authors() default {};
}
