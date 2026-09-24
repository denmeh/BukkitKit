package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Schedules a public method on a {@link Component} singleton.
 * <p>
 * Return {@code void} to keep repeating until plugin disable.
 * Return {@code boolean} where {@code false} cancels that task.
 * <p>
 * Default thread is the server main thread; set {@link #async()} for async.
 *
 * <pre>{@code
 * @Scheduled(every = 10, unit = ScheduleUnit.SECONDS)
 * public boolean autosave() {
 *     if (!dirty) return true;
 *     save();
 *     return keepGoing;
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Scheduled {

    /**
     * Period between runs. Use {@code -1} for a one-shot (only {@link #delay()} applies).
     */
    long every() default -1L;

    /**
     * Initial delay before the first run.
     */
    long delay() default 0L;

    ScheduleUnit unit() default ScheduleUnit.SECONDS;

    /**
     * When {@code true}, runs off the main server thread.
     */
    boolean async() default false;
}
