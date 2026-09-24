package dev.bukkitkit.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LifecyclesTest {

    @Test
    void enableAllRollsBackOnFailure() {
        List<String> events = new ArrayList<>();
        Lifecycle ok = new Lifecycle() {
            @Override
            public void onEnable() {
                events.add("ok-on");
            }

            @Override
            public void onDisable() {
                events.add("ok-off");
            }
        };
        Lifecycle boom = new Lifecycle() {
            @Override
            public void onEnable() {
                events.add("boom-on");
                throw new IllegalStateException("nope");
            }

            @Override
            public void onDisable() {
                events.add("boom-off");
            }
        };

        assertThrows(IllegalStateException.class, () -> Lifecycles.enableAll(ok, boom));
        assertEquals(List.of("ok-on", "boom-on", "ok-off"), events);
    }
}
