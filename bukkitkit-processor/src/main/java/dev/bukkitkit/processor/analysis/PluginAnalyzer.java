package dev.bukkitkit.processor.analysis;

import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.Permission;
import dev.bukkitkit.api.Wire;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.model.PluginModel.PermissionModel;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzes {@code @BukkitKit} marker classes (metadata only; no user {@code JavaPlugin}).
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

    public PluginModel analyze(TypeElement type) {
        if (type.getKind() != ElementKind.CLASS) {
            error(type, "@BukkitKit is only valid on classes");
            return null;
        }
        if (type.getNestingKind().isNested()) {
            error(type, "@BukkitKit class must be a top-level class");
            return null;
        }
        if (type.getModifiers().contains(Modifier.ABSTRACT)) {
            error(type, "@BukkitKit class must not be abstract");
            return null;
        }
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            error(type, "@BukkitKit class must be public");
            return null;
        }
        if (type.getAnnotation(Component.class) != null) {
            error(type, "@BukkitKit class must not also be a @Component");
            return null;
        }
        if (alreadyJavaPlugin(type)) {
            error(type, "@BukkitKit class must not extend JavaPlugin "
                    + "(BukkitKit generates a separate JavaPlugin entry)");
            return null;
        }
        if (extendsSomethingOtherThanObject(type)) {
            error(type, "@BukkitKit class must not declare a superclass "
                    + "(marker is metadata only; BukkitKit generates the JavaPlugin entry)");
            return null;
        }

        boolean ok = true;
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getAnnotation(Wire.class) != null) {
                error(field, "@Wire is not supported on @BukkitKit markers; "
                        + "inject into @Component types instead");
                ok = false;
            }
        }
        for (var method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            String name = method.getSimpleName().toString();
            if (("onEnable".equals(name) || "onDisable".equals(name))
                    && method.getParameters().isEmpty()) {
                error(method, "@BukkitKit marker must not declare " + name
                        + "(); use @OnEnable / @OnDisable on components instead");
                ok = false;
            }
        }

        BukkitKit annotation = type.getAnnotation(BukkitKit.class);
        if (annotation == null) {
            return null;
        }
        if (annotation.name().isBlank()) {
            error(type, "@BukkitKit name() must not be blank");
            ok = false;
        }
        if (annotation.version().isBlank()) {
            error(type, "@BukkitKit version() must not be blank");
            ok = false;
        }
        if (annotation.apiVersion().isBlank()) {
            error(type, "@BukkitKit apiVersion() must not be blank");
            ok = false;
        }

        List<PermissionModel> permissions = new ArrayList<>();
        Set<String> permissionNames = new HashSet<>();
        for (Permission permission : annotation.permissions()) {
            if (permission.name().isBlank()) {
                error(type, "@Permission name() must not be blank");
                ok = false;
                continue;
            }
            if (!permissionNames.add(permission.name())) {
                error(type, "Duplicate @Permission name: " + permission.name());
                ok = false;
                continue;
            }
            permissions.add(new PermissionModel(
                    permission.name(),
                    permission.description(),
                    permission.defaultValue(),
                    List.copyOf(Arrays.asList(permission.children()))));
        }

        if (!ok) {
            return null;
        }

        return new PluginModel(
                type,
                type.getQualifiedName().toString(),
                elements.getPackageOf(type).getQualifiedName().toString(),
                annotation.name(),
                annotation.version(),
                annotation.apiVersion(),
                annotation.description(),
                List.copyOf(Arrays.asList(annotation.authors())),
                annotation.website(),
                annotation.prefix(),
                annotation.load(),
                List.copyOf(Arrays.asList(annotation.depend())),
                List.copyOf(Arrays.asList(annotation.softDepend())),
                List.copyOf(Arrays.asList(annotation.loadBefore())),
                List.copyOf(Arrays.asList(annotation.provides())),
                List.copyOf(permissions));
    }

    private boolean extendsSomethingOtherThanObject(TypeElement type) {
        TypeMirror superclass = type.getSuperclass();
        if (superclass.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement object = elements.getTypeElement("java.lang.Object");
        if (object == null) {
            return false;
        }
        return !types.isSameType(superclass, object.asType());
    }

    private boolean alreadyJavaPlugin(TypeElement type) {
        TypeElement javaPlugin = elements.getTypeElement(JAVA_PLUGIN);
        if (javaPlugin == null) {
            return false;
        }
        return types.isSubtype(type.asType(), javaPlugin.asType())
                && !types.isSameType(type.asType(), javaPlugin.asType());
    }

    private void error(javax.lang.model.element.Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "BukkitKit: " + message, element);
    }
}
