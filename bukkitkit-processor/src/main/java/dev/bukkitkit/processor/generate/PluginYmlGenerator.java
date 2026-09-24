package dev.bukkitkit.processor.generate;

import dev.bukkitkit.api.PermissionDefault;
import dev.bukkitkit.api.PluginLoad;
import dev.bukkitkit.processor.model.CommandMethod;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.model.PluginModel.PermissionModel;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Writes {@code plugin.yml} from {@code @BukkitKit} metadata and {@code @Command} handlers.
 */
public final class PluginYmlGenerator {

    private final Filer filer;

    public PluginYmlGenerator(Filer filer) {
        this.filer = filer;
    }

    public void write(PluginModel plugin, List<ComponentModel> components) throws IOException {
        FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "plugin.yml");
        try (Writer writer = resource.openWriter()) {
            writeScalar(writer, "name", plugin.name());
            writeScalar(writer, "version", plugin.version());
            writer.write("main: ");
            writer.write(plugin.generatedTypeName());
            writer.write('\n');
            writeScalar(writer, "api-version", plugin.apiVersion());

            if (!plugin.description().isBlank()) {
                writeScalar(writer, "description", plugin.description());
            }
            writeStringList(writer, "authors", plugin.authors());
            if (!plugin.website().isBlank()) {
                writeScalar(writer, "website", plugin.website());
            }
            if (!plugin.prefix().isBlank()) {
                writeScalar(writer, "prefix", plugin.prefix());
            }
            if (plugin.load() == PluginLoad.STARTUP) {
                writer.write("load: STARTUP\n");
            }
            writeStringList(writer, "depend", plugin.depend());
            writeStringList(writer, "softdepend", plugin.softDepend());
            writeStringList(writer, "loadbefore", plugin.loadBefore());
            writeStringList(writer, "provides", plugin.provides());

            List<CommandMethod> commands = collectCommands(components);
            if (!commands.isEmpty()) {
                writer.write("commands:\n");
                for (CommandMethod command : commands) {
                    writer.write("  ");
                    writer.write(yamlKey(command.name()));
                    writer.write(":\n");
                    if (!command.description().isBlank()) {
                        writer.write("    description: ");
                        writer.write(yamlScalar(command.description()));
                        writer.write('\n');
                    }
                    if (!command.usage().isBlank()) {
                        writer.write("    usage: ");
                        writer.write(yamlScalar(command.usage()));
                        writer.write('\n');
                    }
                    if (!command.aliases().isEmpty()) {
                        writer.write("    aliases: ");
                        writer.write(yamlInlineList(command.aliases()));
                        writer.write('\n');
                    }
                    if (!command.permission().isBlank()) {
                        writer.write("    permission: ");
                        writer.write(yamlScalar(command.permission()));
                        writer.write('\n');
                    }
                    if (!command.permissionMessage().isBlank()) {
                        writer.write("    permission-message: ");
                        writer.write(yamlScalar(command.permissionMessage()));
                        writer.write('\n');
                    }
                    if (command.description().isBlank()
                            && command.usage().isBlank()
                            && command.aliases().isEmpty()
                            && command.permission().isBlank()
                            && command.permissionMessage().isBlank()) {
                        writer.write("    {}\n");
                    }
                }
            }

            if (!plugin.permissions().isEmpty()) {
                writer.write("permissions:\n");
                for (PermissionModel permission : plugin.permissions()) {
                    writer.write("  ");
                    writer.write(yamlKey(permission.name()));
                    writer.write(":\n");
                    if (!permission.description().isBlank()) {
                        writer.write("    description: ");
                        writer.write(yamlScalar(permission.description()));
                        writer.write('\n');
                    }
                    writer.write("    default: ");
                    writer.write(permissionDefaultYaml(permission.defaultValue()));
                    writer.write('\n');
                    if (!permission.children().isEmpty()) {
                        writer.write("    children:\n");
                        for (String child : permission.children()) {
                            writer.write("      ");
                            writer.write(yamlKey(child));
                            writer.write(": true\n");
                        }
                    }
                }
            }
        }
    }

    private static List<CommandMethod> collectCommands(List<ComponentModel> components) {
        List<CommandMethod> commands = new ArrayList<>();
        for (ComponentModel component : components) {
            commands.addAll(component.commandMethods());
        }
        commands.sort(Comparator.comparing(CommandMethod::name));
        return commands;
    }

    private static void writeScalar(Writer writer, String key, String value) throws IOException {
        writer.write(key);
        writer.write(": ");
        writer.write(yamlScalar(value));
        writer.write('\n');
    }

    private static void writeStringList(Writer writer, String key, List<String> values) throws IOException {
        if (values.isEmpty()) {
            return;
        }
        writer.write(key);
        writer.write(": ");
        writer.write(yamlInlineList(values));
        writer.write('\n');
    }

    private static String yamlInlineList(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(yamlScalar(values.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String permissionDefaultYaml(PermissionDefault value) {
        return switch (value) {
            case TRUE -> "true";
            case FALSE -> "false";
            case OP -> "op";
            case NOT_OP -> "not op";
        };
    }

    private static String yamlKey(String value) {
        if (needsQuotes(value)) {
            return yamlScalar(value);
        }
        return value;
    }

    private static String yamlScalar(String value) {
        if (needsQuotes(value)) {
            return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return value;
    }

    private static boolean needsQuotes(String value) {
        if (value.isEmpty()) {
            return true;
        }
        char first = value.charAt(0);
        if (Character.isDigit(first) || first == '.' || first == '-') {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ':' || c == '#' || c == '{' || c == '}' || c == '[' || c == ']'
                    || c == ',' || c == '&' || c == '*' || c == '!' || c == '|' || c == '>'
                    || c == '\'' || c == '"' || c == '%' || c == '@' || c == '`'
                    || Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }
}
