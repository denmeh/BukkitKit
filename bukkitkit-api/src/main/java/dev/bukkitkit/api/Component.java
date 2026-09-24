package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a BukkitKit component.
 * <p>
 * Components are discovered at compile time and created by generated bootstrap
 * code. Dependencies are satisfied through constructor injection or {@link Wire}
 * fields.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Component {
}
