package dev.bukkitkit.processor.model;

import dev.bukkitkit.api.EventPriority;

import javax.lang.model.element.ExecutableElement;

/**
 * An {@code @OnEvent} method on a component.
 */
public record EventMethod(
        ExecutableElement method,
        String methodName,
        String eventTypeName,
        EventPriority priority,
        boolean ignoreCancelled
) {
}
