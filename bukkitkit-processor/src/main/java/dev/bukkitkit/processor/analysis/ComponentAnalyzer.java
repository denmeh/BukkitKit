package dev.bukkitkit.processor.analysis;

import dev.bukkitkit.api.OnDisable;
import dev.bukkitkit.api.OnEnable;
import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.api.Scheduled;
import dev.bukkitkit.api.Wire;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.model.ComponentModel.InjectionKind;
import dev.bukkitkit.processor.model.ComponentModel.WiredField;
import dev.bukkitkit.processor.model.EventMethod;
import dev.bukkitkit.processor.model.LifecycleMethod;
import dev.bukkitkit.processor.model.ScheduledMethod;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates and extracts dependency / schedule / event metadata from a {@code @Component} type.
 */
public final class ComponentAnalyzer {

    private static final String BUKKIT_EVENT = "org.bukkit.event.Event";
    private static final String BUKKIT_LISTENER = "org.bukkit.event.Listener";

    private final Elements elements;
    private final Types types;
    private final Messager messager;

    public ComponentAnalyzer(Elements elements, Types types, Messager messager) {
        this.elements = elements;
        this.types = types;
        this.messager = messager;
    }

    public ComponentModel analyze(TypeElement type) {
        if (type.getKind() != ElementKind.CLASS) {
            error(type, "@Component is only valid on concrete classes");
            return null;
        }
        if (type.getModifiers().contains(Modifier.ABSTRACT)) {
            error(type, "@Component class must not be abstract");
            return null;
        }
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            error(type, "@Component class must be public");
            return null;
        }
        if (type.getNestingKind().isNested() && !type.getModifiers().contains(Modifier.STATIC)) {
            error(type, "@Component nested class must be static");
            return null;
        }

        List<WiredField> wiredFields = readWiredFields(type);
        List<ScheduledMethod> scheduledMethods = readScheduledMethods(type);
        List<EventMethod> eventMethods = readEventMethods(type);
        List<LifecycleMethod> onEnableMethods = readLifecycleMethods(type, OnEnable.class, "@OnEnable");
        List<LifecycleMethod> onDisableMethods = readLifecycleMethods(type, OnDisable.class, "@OnDisable");
        boolean alreadyListener = implementsListener(type);

        List<ExecutableElement> publicConstructors = ElementFilter.constructorsIn(type.getEnclosedElements())
                .stream()
                .filter(ctor -> ctor.getModifiers().contains(Modifier.PUBLIC))
                .toList();

        if (publicConstructors.isEmpty()) {
            error(type, "@Component requires exactly one public constructor");
            return null;
        }
        if (publicConstructors.size() > 1) {
            error(type, "@Component must have exactly one public constructor (found "
                    + publicConstructors.size() + ")");
            return null;
        }

        ExecutableElement constructor = publicConstructors.getFirst();

        if (!wiredFields.isEmpty()) {
            if (!constructor.getParameters().isEmpty()) {
                error(type, "@Wire field injection requires a public no-arg constructor "
                        + "(do not mix with constructor parameters)");
                return null;
            }
            List<String> dependencies = wiredFields.stream().map(WiredField::typeName).toList();
            return new ComponentModel(
                    type,
                    type.getQualifiedName().toString(),
                    type.getSimpleName().toString(),
                    elements.getPackageOf(type).getQualifiedName().toString(),
                    InjectionKind.FIELD,
                    List.copyOf(dependencies),
                    List.copyOf(wiredFields),
                    List.copyOf(scheduledMethods),
                    List.copyOf(eventMethods),
                    List.copyOf(onEnableMethods),
                    List.copyOf(onDisableMethods),
                    alreadyListener);
        }

        List<String> dependencies = new ArrayList<>();
        for (VariableElement parameter : constructor.getParameters()) {
            TypeMirror paramType = parameter.asType();
            if (paramType.getKind() != TypeKind.DECLARED) {
                error(parameter, "Unsupported constructor parameter type: " + paramType);
                return null;
            }
            TypeElement paramElement = (TypeElement) ((DeclaredType) paramType).asElement();
            dependencies.add(paramElement.getQualifiedName().toString());
        }

        return new ComponentModel(
                type,
                type.getQualifiedName().toString(),
                type.getSimpleName().toString(),
                elements.getPackageOf(type).getQualifiedName().toString(),
                InjectionKind.CONSTRUCTOR,
                List.copyOf(dependencies),
                List.of(),
                List.copyOf(scheduledMethods),
                List.copyOf(eventMethods),
                List.copyOf(onEnableMethods),
                List.copyOf(onDisableMethods),
                alreadyListener);
    }

    private boolean implementsListener(TypeElement type) {
        TypeElement listener = elements.getTypeElement(BUKKIT_LISTENER);
        if (listener == null) {
            return false;
        }
        return types.isAssignable(type.asType(), listener.asType());
    }

    private List<WiredField> readWiredFields(TypeElement type) {
        List<WiredField> fields = new ArrayList<>();
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getAnnotation(Wire.class) == null) {
                continue;
            }
            if (field.getModifiers().contains(Modifier.STATIC)) {
                error(field, "@Wire field must not be static");
                continue;
            }
            if (field.getModifiers().contains(Modifier.FINAL)) {
                error(field, "@Wire field must not be final");
                continue;
            }
            TypeMirror fieldType = field.asType();
            if (fieldType.getKind() != TypeKind.DECLARED) {
                error(field, "Unsupported @Wire field type: " + fieldType);
                continue;
            }
            TypeElement fieldTypeElement = (TypeElement) ((DeclaredType) fieldType).asElement();
            fields.add(new WiredField(
                    field.getSimpleName().toString(),
                    fieldTypeElement.getQualifiedName().toString()));
        }
        return fields;
    }

    private List<ScheduledMethod> readScheduledMethods(TypeElement type) {
        List<ScheduledMethod> methods = new ArrayList<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            Scheduled annotation = method.getAnnotation(Scheduled.class);
            if (annotation == null) {
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, "@Scheduled method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, "@Scheduled method must be public");
                continue;
            }
            if (!method.getParameters().isEmpty()) {
                error(method, "@Scheduled method must not take parameters");
                continue;
            }
            TypeMirror returnType = method.getReturnType();
            boolean booleanReturn = returnType.getKind() == TypeKind.BOOLEAN;
            if (returnType.getKind() != TypeKind.VOID && !booleanReturn) {
                error(method, "@Scheduled method must return void or boolean");
                continue;
            }
            if (annotation.every() == 0) {
                error(method, "@Scheduled every() must be > 0 or -1 (one-shot)");
                continue;
            }
            if (annotation.every() < -1) {
                error(method, "@Scheduled every() must be > 0 or -1 (one-shot)");
                continue;
            }
            if (annotation.delay() < 0) {
                error(method, "@Scheduled delay() must be >= 0");
                continue;
            }
            methods.add(new ScheduledMethod(
                    method,
                    method.getSimpleName().toString(),
                    annotation.every(),
                    annotation.delay(),
                    annotation.unit(),
                    annotation.async(),
                    booleanReturn));
        }
        return methods;
    }

    private List<LifecycleMethod> readLifecycleMethods(
            TypeElement type,
            Class<? extends java.lang.annotation.Annotation> annotationType,
            String label) {
        List<LifecycleMethod> methods = new ArrayList<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getAnnotation(annotationType) == null) {
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, label + " method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, label + " method must be public");
                continue;
            }
            if (!method.getParameters().isEmpty()) {
                error(method, label + " method must not take parameters");
                continue;
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                error(method, label + " method must return void");
                continue;
            }
            methods.add(new LifecycleMethod(method, method.getSimpleName().toString()));
        }
        return methods;
    }

    private List<EventMethod> readEventMethods(TypeElement type) {
        List<EventMethod> methods = new ArrayList<>();
        TypeElement eventBase = elements.getTypeElement(BUKKIT_EVENT);
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            OnEvent annotation = method.getAnnotation(OnEvent.class);
            if (annotation == null) {
                continue;
            }
            if (eventBase == null) {
                error(method, "@OnEvent requires org.bukkit.event.Event on the classpath (Paper/Bukkit API)");
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, "@OnEvent method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, "@OnEvent method must be public");
                continue;
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                error(method, "@OnEvent method must return void");
                continue;
            }
            if (method.getParameters().size() != 1) {
                error(method, "@OnEvent method must take exactly one Event parameter");
                continue;
            }
            VariableElement parameter = method.getParameters().getFirst();
            TypeMirror paramType = parameter.asType();
            if (paramType.getKind() != TypeKind.DECLARED) {
                error(parameter, "@OnEvent parameter must be a Bukkit Event type");
                continue;
            }
            if (!types.isAssignable(paramType, eventBase.asType())) {
                error(parameter, "@OnEvent parameter must be a subtype of org.bukkit.event.Event");
                continue;
            }
            TypeElement eventType = (TypeElement) ((DeclaredType) paramType).asElement();
            methods.add(new EventMethod(
                    method,
                    method.getSimpleName().toString(),
                    eventType.getQualifiedName().toString(),
                    annotation.priority(),
                    annotation.ignoreCancelled()));
        }
        return methods;
    }

    private void error(javax.lang.model.element.Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "BukkitKit: " + message, element);
    }
}
