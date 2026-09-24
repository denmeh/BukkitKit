package dev.bukkitkit.processor.model;

import dev.bukkitkit.api.BukkitKitSymbols;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * A {@code @BukkitKit} marker class and its {@code plugin.yml} metadata.
 */
public record PluginModel(
        TypeElement type,
        String typeName,
        String packageName,
        String name,
        String version,
        String apiVersion,
        String description,
        List<String> authors
) {

    /** Simple name of the generated {@code JavaPlugin} entry (Filer). */
    public String generatedSimpleName() {
        return type.getSimpleName() + BukkitKitSymbols.GENERATED_PLUGIN_SUFFIX;
    }

    /** Fully-qualified name of the generated {@code JavaPlugin} entry. */
    public String generatedTypeName() {
        if (packageName.isEmpty()) {
            return generatedSimpleName();
        }
        return packageName + "." + generatedSimpleName();
    }
}
