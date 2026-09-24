package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a permission node for {@code plugin.yml}. Used inside {@link BukkitKit#permissions()}.
 *
 * <pre>{@code
 * @BukkitKit(
 *     // ...
 *     permissions = {
 *         @Permission(name = "demo.hello", description = "Use /hello", defaultValue = PermissionDefault.TRUE)
 *     }
 * )
 * }</pre>
 */
@Documented
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Permission {

    /** Permission node (e.g. {@code demo.hello}). */
    String name();

    /** Human-readable description; omitted from {@code plugin.yml} when empty. */
    String description() default "";

    /** Who has the permission by default. */
    PermissionDefault defaultValue() default PermissionDefault.OP;

    /**
     * Child permission nodes that inherit this permission when granted
     * ({@code children: { child: true }} in {@code plugin.yml}).
     */
    String[] children() default {};
}
