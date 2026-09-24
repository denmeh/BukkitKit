package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a plain class as the BukkitKit plugin entry metadata. BukkitKit generates a
 * {@code JavaPlugin} subclass ({@code YourClass_BukkitKit}) and writes {@code plugin.yml}
 * from the annotation values.
 *
 * <pre>{@code
 * @BukkitKit(
 *     name = "Hello",
 *     version = "1.0.0",
 *     apiVersion = "1.21",
 *     description = "Says hello",
 *     authors = {"denmeh"},
 *     website = "https://example.com",
 *     depend = {"Vault"},
 *     softDepend = {"WorldGuard"},
 *     permissions = {
 *         @Permission(name = "hello.use", description = "Use /hello", defaultValue = PermissionDefault.TRUE)
 *     }
 * )
 * public final class HelloPlugin {
 * }
 * }</pre>
 *
 * Put startup logic on components with {@link OnEnable} / {@link OnDisable}.
 * Declare commands with {@link Command} on component methods.
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

    /** {@code plugin.yml} {@code website}; omitted when empty. */
    String website() default "";

    /** {@code plugin.yml} {@code prefix} (log prefix); omitted when empty. */
    String prefix() default "";

    /**
     * {@code plugin.yml} {@code load}. {@link PluginLoad#POSTWORLD} is the Bukkit default
     * and is omitted from the file.
     */
    PluginLoad load() default PluginLoad.POSTWORLD;

    /** {@code plugin.yml} {@code depend}; omitted when empty. */
    String[] depend() default {};

    /** {@code plugin.yml} {@code softdepend}; omitted when empty. */
    String[] softDepend() default {};

    /** {@code plugin.yml} {@code loadbefore}; omitted when empty. */
    String[] loadBefore() default {};

    /** {@code plugin.yml} {@code provides}; omitted when empty. */
    String[] provides() default {};

    /** {@code plugin.yml} {@code permissions}; omitted when empty. */
    Permission[] permissions() default {};
}
