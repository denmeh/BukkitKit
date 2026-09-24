package dev.bukkitkit.processor.model;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

/**
 * An {@code @Command} method on a component.
 */
public record CommandMethod(
        ExecutableElement method,
        String methodName,
        String name,
        String description,
        String usage,
        List<String> aliases,
        String permission,
        String permissionMessage,
        Signature signature,
        boolean booleanReturn
) {

    public enum Signature {
        SENDER,
        SENDER_ARGS,
        SENDER_LABEL_ARGS
    }
}
