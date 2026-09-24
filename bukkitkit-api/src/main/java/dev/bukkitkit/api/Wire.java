package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for BukkitKit dependency injection.
 * <p>
 * Usable on managed classes ({@link Component}, or classes hosting {@link OnEvent} /
 * {@link Command} / lifecycle methods). Inject another managed type ({@link Component},
 * {@link Config}) or a built-in ({@code JavaPlugin}, {@code Logger}, …).
 * Wired fields must not be {@code static} or {@code final}.
 * Classes that use {@code @Wire} must expose a single public no-arg constructor.
 * {@link Config} classes themselves must not declare {@code @Wire} fields.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Wire {
}
