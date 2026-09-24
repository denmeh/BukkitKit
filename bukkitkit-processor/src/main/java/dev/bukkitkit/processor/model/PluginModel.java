package dev.bukkitkit.processor.model;

import dev.bukkitkit.api.PermissionDefault;
import dev.bukkitkit.api.PluginLoad;
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
        List<String> authors,
        String website,
        String prefix,
        PluginLoad load,
        List<String> depend,
        List<String> softDepend,
        List<String> loadBefore,
        List<String> provides,
        List<PermissionModel> permissions
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

    /**
     * A permission node for {@code plugin.yml}.
     */
    public record PermissionModel(
            String name,
            String description,
            PermissionDefault defaultValue,
            List<String> children
    ) {
    }
}
