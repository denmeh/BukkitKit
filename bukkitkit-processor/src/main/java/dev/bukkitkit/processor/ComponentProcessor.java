package dev.bukkitkit.processor;

import dev.bukkitkit.api.BukkitKit;
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.processor.analysis.ComponentAnalyzer;
import dev.bukkitkit.processor.analysis.PluginAnalyzer;
import dev.bukkitkit.processor.generate.BootstrapGenerator;
import dev.bukkitkit.processor.inject.PluginInjector;
import dev.bukkitkit.processor.inject.SingletonInjector;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.util.JavacContext;
import dev.bukkitkit.processor.util.JavacOpener;
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
 * Discovers {@link Component} / {@link OnEvent} / {@link BukkitKit} types,
 * injects singletons and plugin wiring.
 */
@SupportedAnnotationTypes({
        "dev.bukkitkit.api.Component",
        "dev.bukkitkit.api.OnEvent",
        "dev.bukkitkit.api.BukkitKit"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class ComponentProcessor extends AbstractProcessor {

    private ComponentAnalyzer componentAnalyzer;
    private PluginAnalyzer pluginAnalyzer;
    private GraphValidator validator;
    private SingletonInjector singletonInjector;
    private PluginInjector pluginInjector;
    private BootstrapGenerator generator;
    private boolean processed;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        JavacOpener.open();
        this.componentAnalyzer = new ComponentAnalyzer(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                processingEnv.getMessager());
        this.pluginAnalyzer = new PluginAnalyzer(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                processingEnv.getMessager());
        this.validator = new GraphValidator(processingEnv.getMessager());
        JavacContext javac = JavacContext.from(processingEnv);
        this.singletonInjector = new SingletonInjector(javac, processingEnv.getElementUtils());
        this.pluginInjector = new PluginInjector(javac);
        this.generator = new BootstrapGenerator(processingEnv.getFiler());
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

        Set<String> componentNames = new HashSet<>();
        for (ComponentModel component : components) {
            componentNames.add(component.typeName());
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
            PluginModel model = pluginAnalyzer.analyze(typeElement, componentNames, pluginTypeNames);
            if (model != null) {
                plugins.add(model);
            }
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

        for (ComponentModel component : ordered) {
            try {
                singletonInjector.inject(component);
            } catch (RuntimeException ex) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: failed to inject singleton into " + component.typeName()
                                + ": " + ex.getMessage(),
                        component.type());
                return false;
            }
        }

        if (!ordered.isEmpty()) {
            try {
                generator.write(ordered, pluginTypeNames);
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: failed to write bootstrap: " + ex.getMessage());
                return false;
            }
        }

        for (PluginModel plugin : plugins) {
            try {
                pluginInjector.inject(plugin);
            } catch (RuntimeException ex) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: failed to wire plugin " + plugin.typeName()
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
     * {@code @OnEvent} methods. A type that is both is included once.
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

        for (Element element : roundEnv.getElementsAnnotatedWith(OnEvent.class)) {
            if (!(element instanceof ExecutableElement)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: @OnEvent is only valid on methods",
                        element);
                continue;
            }
            Element enclosing = element.getEnclosingElement();
            if (!(enclosing instanceof TypeElement typeElement)
                    || typeElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: @OnEvent method must be declared on a class",
                        element);
                continue;
            }
            String typeName = typeElement.getQualifiedName().toString();
            if (pluginTypeNames.contains(typeName)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "BukkitKit: @OnEvent is not supported on @BukkitKit plugins; "
                                + "move the handler to its own class",
                        element);
                continue;
            }
            managed.putIfAbsent(typeName, typeElement);
        }

        return managed;
    }
}
