package dev.bukkitkit.processor;

import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.OnDisable;
import dev.bukkitkit.api.OnEnable;
import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.processor.analysis.ComponentAnalyzer;
import dev.bukkitkit.processor.analysis.PluginAnalyzer;
import dev.bukkitkit.processor.generate.BootstrapGenerator;
import dev.bukkitkit.processor.generate.PluginGenerator;
import dev.bukkitkit.processor.generate.PluginYmlGenerator;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.validation.GraphValidator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discovers {@link Component} / {@link OnEvent} / {@link OnEnable} / {@link OnDisable} /
 * {@link BukkitKit} types and generates bootstrap + plugin entry sources via {@code Filer}.
 */
@SupportedAnnotationTypes({
        "dev.bukkitkit.api.Component",
        "dev.bukkitkit.api.OnEvent",
        "dev.bukkitkit.api.OnEnable",
        "dev.bukkitkit.api.OnDisable",
        "dev.bukkitkit.api.BukkitKit"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class ComponentProcessor extends AbstractProcessor {

    private ComponentAnalyzer componentAnalyzer;
    private PluginAnalyzer pluginAnalyzer;
    private GraphValidator validator;
    private BootstrapGenerator bootstrapGenerator;
    private PluginGenerator pluginGenerator;
    private PluginYmlGenerator pluginYmlGenerator;
    private boolean processed;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.componentAnalyzer = new ComponentAnalyzer(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                processingEnv.getMessager());
        this.pluginAnalyzer = new PluginAnalyzer(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                processingEnv.getMessager());
        this.validator = new GraphValidator(processingEnv.getMessager());
        this.bootstrapGenerator = new BootstrapGenerator(processingEnv.getFiler());
        this.pluginGenerator = new PluginGenerator(processingEnv.getFiler());
        this.pluginYmlGenerator = new PluginYmlGenerator(processingEnv.getFiler());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || processed) {
            return false;
        }

        Set<String> pluginTypeNames = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(BukkitKit.class)) {
            if (element instanceof TypeElement typeElement) {
                pluginTypeNames.add(typeElement.getQualifiedName().toString());
            }
        }

        Map<String, TypeElement> managedTypes = collectManagedTypes(roundEnv, pluginTypeNames);
        List<ComponentModel> components = new ArrayList<>();
        for (TypeElement typeElement : managedTypes.values()) {
            ComponentModel model = componentAnalyzer.analyze(typeElement);
            if (model != null) {
                components.add(model);
            }
        }

        List<PluginModel> plugins = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(BukkitKit.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: @BukkitKit is only valid on types",
                        element);
                continue;
            }
            PluginModel model = pluginAnalyzer.analyze(typeElement);
            if (model != null) {
                plugins.add(model);
            }
        }

        if (plugins.size() > 1) {
            for (PluginModel plugin : plugins) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: only one @BukkitKit marker is allowed per compilation",
                        plugin.type());
            }
            return false;
        }

        if (components.isEmpty() && plugins.isEmpty()) {
            return false;
        }

        components.sort(Comparator.comparing(ComponentModel::typeName));
        if (!components.isEmpty() && !validator.validate(components, roundEnv, pluginTypeNames)) {
            return false;
        }

        List<ComponentModel> ordered = components.isEmpty()
                ? List.of()
                : validator.topologicalOrder(components);

        if (!ordered.isEmpty()) {
            try {
                bootstrapGenerator.write(ordered);
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: failed to write bootstrap: " + ex.getMessage());
                return false;
            }
        }

        for (PluginModel plugin : plugins) {
            try {
                pluginGenerator.write(plugin);
                pluginYmlGenerator.write(plugin);
            } catch (RuntimeException | IOException ex) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: failed to generate plugin entry for " + plugin.typeName()
                                + ": " + ex.getMessage(),
                        plugin.type());
                return false;
            }
        }

        processed = true;
        return false;
    }

    /**
     * Types managed as singletons: {@code @Component} classes plus enclosing classes of
     * {@code @OnEvent} / {@code @OnEnable} / {@code @OnDisable} methods.
     */
    private Map<String, TypeElement> collectManagedTypes(
            RoundEnvironment roundEnv,
            Set<String> pluginTypeNames) {
        Map<String, TypeElement> managed = new LinkedHashMap<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(Component.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: @Component is only valid on types",
                        element);
                continue;
            }
            managed.put(typeElement.getQualifiedName().toString(), typeElement);
        }

        collectMethodHostedTypes(roundEnv, pluginTypeNames, managed, OnEvent.class, "@OnEvent");
        collectMethodHostedTypes(roundEnv, pluginTypeNames, managed, OnEnable.class, "@OnEnable");
        collectMethodHostedTypes(roundEnv, pluginTypeNames, managed, OnDisable.class, "@OnDisable");

        return managed;
    }

    private void collectMethodHostedTypes(
            RoundEnvironment roundEnv,
            Set<String> pluginTypeNames,
            Map<String, TypeElement> managed,
            Class<? extends java.lang.annotation.Annotation> annotation,
            String label) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (!(element instanceof ExecutableElement)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: " + label + " is only valid on methods",
                        element);
                continue;
            }
            Element enclosing = element.getEnclosingElement();
            if (!(enclosing instanceof TypeElement typeElement)
                    || typeElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: " + label + " method must be declared on a class",
                        element);
                continue;
            }
            String typeName = typeElement.getQualifiedName().toString();
            if (pluginTypeNames.contains(typeName)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: " + label + " is not supported on @BukkitKit markers; "
                                + "move the handler to a component class",
                        element);
                continue;
            }
            managed.putIfAbsent(typeName, typeElement);
        }
    }
}
