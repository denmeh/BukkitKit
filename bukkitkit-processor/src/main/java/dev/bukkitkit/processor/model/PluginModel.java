package dev.bukkitkit.processor.model;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * A {@code @BukkitKit} plugin class and its {@code @Wire} fields.
 */
public record PluginModel(
        TypeElement type,
        String typeName,
        List<InjectedField> injectedFields
) {

    public enum FieldKind {
        COMPONENT,
        PLATFORM,
        PLUGIN
    }

    public record InjectedField(
            VariableElement element,
            String fieldName,
            String typeName,
            FieldKind kind
    ) {
    }
}
