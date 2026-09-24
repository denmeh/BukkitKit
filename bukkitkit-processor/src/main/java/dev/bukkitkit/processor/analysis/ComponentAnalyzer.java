package dev.bukkitkit.processor.analysis;

import dev.bukkitkit.api.Wire;
import dev.bukkitkit.processor.builtins.BuiltInTypes;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.model.ComponentModel.InjectionKind;
import dev.bukkitkit.processor.model.ComponentModel.WiredField;

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
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates and extracts dependency metadata from a {@code @Component} type.
 */
public final class ComponentAnalyzer {

    private final Elements elements;
    private final Messager messager;

    public ComponentAnalyzer(Elements elements, Messager messager) {
        this.elements = elements;
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
                    List.copyOf(wiredFields));
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
                List.of());
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

    private void error(javax.lang.model.element.Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "BukkitKit: " + message, element);
    }
}
