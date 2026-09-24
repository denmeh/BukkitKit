package dev.bukkitkit.processor.model;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * A {@code @BukkitKit} marker class and its {@code plugin.yml} metadata.
 */
public record PluginModel(
        TypeElement type,
        String typeName,
        String name,
        String version,
        String apiVersion,
        String description,
        List<String> authors
) {
}
