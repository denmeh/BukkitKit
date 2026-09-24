package dev.bukkitkit.core;

import dev.bukkitkit.api.BukkitKitException;

import java.lang.reflect.Field;

/**
 * Assigns {@code @Wire} fields from generated bootstrap code.
 * Private fields require reflective access (Filer cannot mutate user classes).
 */
public final class FieldWire {

    private FieldWire() {
    }

    public static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new BukkitKitException(
                    "BukkitKit: failed to wire field '" + fieldName + "' on "
                            + target.getClass().getName(),
                    ex);
        }
    }
}
