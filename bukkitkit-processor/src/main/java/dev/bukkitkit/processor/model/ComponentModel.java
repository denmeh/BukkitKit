package dev.bukkitkit.processor.model;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Analyzed component type ready for bootstrap generation.
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
        List<LifecycleMethod> onEnableMethods,
        List<LifecycleMethod> onDisableMethods,
        boolean alreadyListener
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

    public boolean hasOnEnable() {
        return !onEnableMethods.isEmpty();
    }

    public boolean hasOnDisable() {
        return !onDisableMethods.isEmpty();
    }
}
