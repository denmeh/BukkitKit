package dev.bukkitkit.processor.validation;

import dev.bukkitkit.processor.builtins.BuiltInTypes;
import dev.bukkitkit.processor.model.ComponentModel;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compile-time dependency and cycle validation.
 */
public final class GraphValidator {

    private final Messager messager;

    public GraphValidator(Messager messager) {
        this.messager = messager;
    }

    public boolean validate(
            List<ComponentModel> components,
            RoundEnvironment roundEnv,
            Set<String> pluginTypeNames) {
        Set<String> componentNames = new HashSet<>();
        Map<String, ComponentModel> byName = new HashMap<>();
        for (ComponentModel component : components) {
            componentNames.add(component.typeName());
            byName.put(component.typeName(), component);
        }

        boolean valid = true;
        Set<String> rootTypeNames = rootTypeNames(roundEnv);
        for (ComponentModel component : components) {
            valid &= validateDependencies(component, componentNames, pluginTypeNames, rootTypeNames);
        }
        valid &= validateCycles(components, byName, componentNames);
        return valid;
    }

    /**
     * Returns components in dependency-first topological order.
     */
    public List<ComponentModel> topologicalOrder(List<ComponentModel> components) {
        Set<String> componentNames = new HashSet<>();
        Map<String, ComponentModel> byName = new LinkedHashMap<>();
        for (ComponentModel component : components) {
            componentNames.add(component.typeName());
            byName.put(component.typeName(), component);
        }

        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (ComponentModel component : components) {
            List<String> edges = new ArrayList<>();
            for (String dep : component.dependencies()) {
                if (componentNames.contains(dep)) {
                    edges.add(dep);
                }
            }
            graph.put(component.typeName(), edges);
        }

        List<ComponentModel> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (ComponentModel component : components) {
            topoVisit(component.typeName(), graph, byName, visited, ordered);
        }
        return ordered;
    }

    private void topoVisit(
            String node,
            Map<String, List<String>> graph,
            Map<String, ComponentModel> byName,
            Set<String> visited,
            List<ComponentModel> ordered) {
        if (!visited.add(node)) {
            return;
        }
        for (String dep : graph.getOrDefault(node, List.of())) {
            topoVisit(dep, graph, byName, visited, ordered);
        }
        ordered.add(byName.get(node));
    }

    private boolean validateDependencies(
            ComponentModel component,
            Set<String> componentNames,
            Set<String> pluginTypeNames,
            Set<String> rootTypeNames) {
        boolean valid = true;
        for (int i = 0; i < component.dependencies().size(); i++) {
            String dep = component.dependencies().get(i);
            if (componentNames.contains(dep)
                    || BuiltInTypes.isBuiltIn(dep)
                    || pluginTypeNames.contains(dep)) {
                continue;
            }
            if (rootTypeNames.contains(dep)) {
                error(component.type(),
                        "Unresolved dependency " + dep + " (parameter/field " + i + "). "
                                + "Declare @Component, use a built-in type, or wire the @BukkitKit plugin type.");
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateCycles(
            List<ComponentModel> components,
            Map<String, ComponentModel> byName,
            Set<String> componentNames) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (ComponentModel component : components) {
            List<String> edges = new ArrayList<>();
            for (String dep : component.dependencies()) {
                if (componentNames.contains(dep)) {
                    edges.add(dep);
                }
            }
            graph.put(component.typeName(), edges);
        }

        Set<String> visited = new HashSet<>();
        Set<String> stackSet = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();

        for (ComponentModel component : components) {
            if (detectCycle(component.typeName(), graph, visited, stackSet, path, byName)) {
                return false;
            }
        }
        return true;
    }

    private boolean detectCycle(
            String node,
            Map<String, List<String>> graph,
            Set<String> visited,
            Set<String> stackSet,
            Deque<String> path,
            Map<String, ComponentModel> byName) {
        if (stackSet.contains(node)) {
            List<String> cycle = new ArrayList<>();
            boolean recording = false;
            for (String frame : path) {
                if (frame.equals(node)) {
                    recording = true;
                }
                if (recording) {
                    cycle.add(frame);
                }
            }
            cycle.add(node);
            ComponentModel model = byName.get(node);
            error(model != null ? model.type() : null,
                    "Dependency cycle detected: " + String.join(" → ", cycle));
            return true;
        }
        if (!visited.add(node)) {
            return false;
        }

        stackSet.add(node);
        path.addLast(node);
        for (String dep : graph.getOrDefault(node, List.of())) {
            if (detectCycle(dep, graph, visited, stackSet, path, byName)) {
                return true;
            }
        }
        path.removeLast();
        stackSet.remove(node);
        return false;
    }

    private static Set<String> rootTypeNames(RoundEnvironment roundEnv) {
        Set<String> names = new HashSet<>();
        for (Element root : roundEnv.getRootElements()) {
            if (root instanceof TypeElement typeElement) {
                names.add(typeElement.getQualifiedName().toString());
            }
        }
        return names;
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "BukkitKit: " + message, element);
    }
}
