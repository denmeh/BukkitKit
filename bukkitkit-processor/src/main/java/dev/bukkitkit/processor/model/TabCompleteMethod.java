package dev.bukkitkit.processor.model;

import javax.lang.model.element.ExecutableElement;

/**
 * A {@code @TabComplete} method on a component.
 */
public record TabCompleteMethod(
        ExecutableElement method,
        String methodName,
        String commandName,
        Signature signature
) {

    public enum Signature {
        SENDER_ARGS,
        SENDER_ALIAS_ARGS
    }
}
