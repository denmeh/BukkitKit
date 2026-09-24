package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public no-arg {@code void} method to run when the plugin disables.
 * <p>
 * The enclosing class becomes a managed singleton automatically (no {@link Component}
 * required). Methods run in reverse dependency order across types and reverse
 * declaration order within a type, before unbind.
 *
 * <pre>{@code
 * @Component
 * public final class PlayerManager {
 *     @OnDisable
 *     public void stop() {
 *         // ...
 *     }
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface OnDisable {
}
