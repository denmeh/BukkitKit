package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for BukkitKit dependency injection.
 * <p>
 * Usable on {@link Component} classes and {@link BukkitKit} plugins.
 * Wired fields must not be {@code static} or {@code final}.
 * Components that use {@code @Wire} must expose a single public no-arg constructor.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Wire {
}
