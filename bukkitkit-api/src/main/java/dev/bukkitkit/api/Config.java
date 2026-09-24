package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a typed plugin configuration.
 * <p>
 * Define defaults in Java with public fields. BukkitKit creates one singleton,
 * loads matching values from the YAML file under the plugin data folder, and
 * writes the file on first run (or fills in any missing keys). Inject the class
 * with {@link Wire} like any other managed type.
 *
 * <pre>{@code
 * @Config
 * public final class DemoConfig {
 *     public String welcomeMessage = "Welcome!";
 *     public boolean joinMessageEnabled = true;
 *     public Database database = new Database();
 *
 *     public static final class Database {
 *         public String host = "localhost";
 *         public int port = 3306;
 *     }
 * }
 * }</pre>
 *
 * Field names become YAML keys. Nested public classes become YAML sections.
 * Supported leaf types: {@code boolean}/{@code Boolean}, {@code int}/{@code Integer},
 * {@code long}/{@code Long}, {@code double}/{@code Double}, {@code float}/{@code Float},
 * {@code String}, and {@code List<String>}.
 * <p>
 * Do not mix with {@link Component}, {@link Wire}, lifecycle, events, commands, or
 * schedules — a config class is data only.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Config {

    /**
     * File name under the plugin data folder (e.g. {@code config.yml}).
     */
    String file() default "config.yml";

    /**
     * When {@code false}, keep Java field defaults and never read or write disk.
     * Useful for tests or plugins with no operator-facing settings.
     */
    boolean persistent() default true;
}
