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
        List<CommandMethod> commandMethods,
        List<TabCompleteMethod> tabCompleteMethods,
        List<LifecycleMethod> onEnableMethods,
        List<LifecycleMethod> onDisableMethods,
        boolean alreadyListener,
        ConfigMeta config
) {

    public enum InjectionKind {
        CONSTRUCTOR,
        FIELD
    }

    public record WiredField(String fieldName, String typeName) {
    }

    /**
     * Present when the type is annotated with {@code @Config}.
     */
    public record ConfigMeta(String file, boolean persistent) {
    }

    public boolean isConfig() {
        return config != null;
    }

    public boolean hasEvents() {
        return !eventMethods.isEmpty();
    }

    public boolean hasCommands() {
        return !commandMethods.isEmpty();
    }

    public boolean hasOnEnable() {
        return !onEnableMethods.isEmpty();
    }

    public boolean hasOnDisable() {
        return !onDisableMethods.isEmpty();
    }
}
