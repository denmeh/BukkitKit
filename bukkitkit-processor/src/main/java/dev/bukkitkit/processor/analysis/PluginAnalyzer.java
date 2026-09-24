package dev.bukkitkit.processor.analysis;

import dev.bukkitkit.api.Wire;
import dev.bukkitkit.processor.builtins.BuiltInTypes;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.model.PluginModel.FieldKind;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
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
import java.util.Set;

/**
 * Analyzes {@code @BukkitKit} plugin classes and their {@code @Wire} fields.
 */
public final class PluginAnalyzer {

    private static final String JAVA_PLUGIN = "org.bukkit.plugin.java.JavaPlugin";

    private final Elements elements;
    private final Types types;
    private final Messager messager;

    public PluginAnalyzer(Elements elements, Types types, Messager messager) {
        this.elements = elements;
        this.types = types;
        this.messager = messager;
    }

    public PluginModel analyze(
            TypeElement type,
            Set<String> componentTypeNames,
            Set<String> pluginTypeNames) {
        if (type.getKind() != ElementKind.CLASS) {
            error(type, "@BukkitKit is only valid on classes");
            return null;
        }
        if (!isJavaPlugin(type)) {
            error(type, "@BukkitKit class must extend org.bukkit.plugin.java.JavaPlugin");
            return null;
        }

        List<PluginModel.InjectedField> fields = new ArrayList<>();
        boolean ok = true;
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getAnnotation(Wire.class) == null) {
                continue;
            }
            if (field.getModifiers().contains(Modifier.STATIC)) {
                error(field, "@Wire field must not be static");
                ok = false;
                continue;
            }
            if (field.getModifiers().contains(Modifier.FINAL)) {
                error(field, "@Wire field must not be final");
                ok = false;
                continue;
            }
            TypeMirror fieldType = field.asType();
            if (fieldType.getKind() != TypeKind.DECLARED) {
                error(field, "Unsupported @Wire field type: " + fieldType);
                ok = false;
                continue;
            }
            TypeElement fieldTypeElement = (TypeElement) ((DeclaredType) fieldType).asElement();
            String fieldTypeName = fieldTypeElement.getQualifiedName().toString();

            FieldKind kind;
            if (BuiltInTypes.isBuiltIn(fieldTypeName)) {
                kind = FieldKind.PLATFORM;
            } else if (pluginTypeNames.contains(fieldTypeName)) {
                kind = FieldKind.PLUGIN;
            } else if (componentTypeNames.contains(fieldTypeName)) {
                kind = FieldKind.COMPONENT;
            } else {
                error(field, "Unresolved @Wire dependency " + fieldTypeName
                        + ". Declare @Component, use a built-in type, or wire the @BukkitKit plugin type.");
                ok = false;
                continue;
            }
            fields.add(new PluginModel.InjectedField(
                    field,
                    field.getSimpleName().toString(),
                    fieldTypeName,
                    kind));
        }

        if (!ok) {
            return null;
        }
        return new PluginModel(type, type.getQualifiedName().toString(), List.copyOf(fields));
    }

    private boolean isJavaPlugin(TypeElement type) {
        TypeElement javaPlugin = elements.getTypeElement(JAVA_PLUGIN);
        if (javaPlugin == null) {
            return true;
        }
        return types.isSubtype(type.asType(), javaPlugin.asType());
    }

    private void error(javax.lang.model.element.Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "BukkitKit: " + message, element);
    }
}
