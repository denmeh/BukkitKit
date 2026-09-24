package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public method as an event handler.
 * <p>
 * The enclosing class becomes a managed singleton automatically (no {@link Component}
 * required). If the class is already a {@code @Component}, it is registered only once.
 * <p>
 * The method must take exactly one parameter that is a Bukkit {@code Event} subtype
 * and return {@code void}. BukkitKit registers the handler at bootstrap (no
 * {@code implements Listener} or {@code registerEvents} required).
 *
 * <pre>{@code
 * public final class JoinListener {
 *     @OnEvent
 *     public void onJoin(PlayerJoinEvent event) {
 *         // ...
 *     }
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface OnEvent {

    EventPriority priority() default EventPriority.NORMAL;

    /**
     * When {@code true}, cancelled events are not delivered to this handler.
     */
    boolean ignoreCancelled() default false;
}
