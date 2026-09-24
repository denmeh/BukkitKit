package dev.bukkitkit.processor.model;

import javax.lang.model.element.ExecutableElement;

/**
 * A {@code @OnEnable} or {@code @OnDisable} method on a component.
 */
public record LifecycleMethod(
        ExecutableElement element,
        String methodName
) {
}
