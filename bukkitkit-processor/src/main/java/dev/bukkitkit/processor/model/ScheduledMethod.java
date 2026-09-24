package dev.bukkitkit.processor.model;

import dev.bukkitkit.api.ScheduleUnit;

import javax.lang.model.element.ExecutableElement;

/**
 * A {@code @Scheduled} method on a component.
 */
public record ScheduledMethod(
        ExecutableElement method,
        String methodName,
        long every,
        long delay,
        ScheduleUnit unit,
        boolean async,
        boolean booleanReturn
) {

    public long delayTicks() {
        return toTicks(delay, unit);
    }

    public long periodTicks() {
        if (every < 0) {
            return -1L;
        }
        return toTicks(every, unit);
    }

    public boolean repeating() {
        return every > 0;
    }

    private static long toTicks(long amount, ScheduleUnit unit) {
        return switch (unit) {
            case TICKS -> amount;
            case SECONDS -> amount * 20L;
            case MINUTES -> amount * 20L * 60L;
        };
    }
}
