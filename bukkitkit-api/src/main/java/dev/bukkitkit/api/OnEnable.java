package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public no-arg {@code void} method to run when the plugin enables.
 * <p>
 * The enclosing class becomes a managed singleton automatically (no {@link Component}
 * required). Methods run after construction and {@link Wire} injection, in dependency
 * order across types and declaration order within a type.
 *
 * <pre>{@code
 * @Component
 * public final class PlayerManager {
 *     @OnEnable
 *     public void start() {
 *         // ...
 *     }
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface OnEnable {
}
