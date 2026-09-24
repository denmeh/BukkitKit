package dev.bukkitkit.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code JavaPlugin} subclass as a BukkitKit plugin.
 * <p>
 * Component-typed fields are filled automatically at the start of {@code onEnable}
 * after singletons are bootstrapped. Prefer declaring fields over calling static accessors.
 *
 * <pre>{@code
 * @BukkitKit
 * public final class MyPlugin extends JavaPlugin {
 *     private PlayerManager players;
 *
 *     @Override
 *     public void onEnable() {
 *         players.load();
 *     }
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface BukkitKit {
}
