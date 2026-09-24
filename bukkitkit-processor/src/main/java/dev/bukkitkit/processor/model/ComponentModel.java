package dev.bukkitkit.processor.model;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Analyzed {@code @Component} type ready for injection and bootstrap generation.
 */
public record ComponentModel(
        TypeElement type,
        String typeName,
        String simpleName,
        String packageName,
        InjectionKind kind,
        List<String> dependencies,
        List<WiredField> wiredFields,
        List<ScheduledMethod> scheduledMethods,
        List<EventMethod> eventMethods,
        boolean needsListener
) {

    public enum InjectionKind {
        CONSTRUCTOR,
        FIELD
    }

    public record WiredField(String fieldName, String typeName) {
    }

    public boolean hasEvents() {
        return !eventMethods.isEmpty();
    }
}
