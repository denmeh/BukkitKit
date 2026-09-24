package dev.bukkitkit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldWireTest {

    @Test
    void setsPrivateField() {
        Target target = new Target();
        FieldWire.set(target, "value", "hello");
        assertEquals("hello", target.value);
    }

    @Test
    void failsOnMissingField() {
        Target target = new Target();
        assertThrows(dev.bukkitkit.api.BukkitKitException.class,
                () -> FieldWire.set(target, "missing", "x"));
    }

    static final class Target {
        private String value;
    }
}
