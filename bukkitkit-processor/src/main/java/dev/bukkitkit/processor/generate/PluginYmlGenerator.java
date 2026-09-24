package dev.bukkitkit.processor.generate;

import dev.bukkitkit.processor.model.PluginModel;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes {@code plugin.yml} from {@code @BukkitKit} metadata.
 */
public final class PluginYmlGenerator {

    private final Filer filer;

    public PluginYmlGenerator(Filer filer) {
        this.filer = filer;
    }

    public void write(PluginModel plugin) throws IOException {
        FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "plugin.yml");
        try (Writer writer = resource.openWriter()) {
            writer.write("name: ");
            writer.write(yamlScalar(plugin.name()));
            writer.write('\n');
            writer.write("version: ");
            writer.write(yamlScalar(plugin.version()));
            writer.write('\n');
            writer.write("main: ");
            writer.write(plugin.generatedTypeName());
            writer.write('\n');
            writer.write("api-version: ");
            writer.write(yamlScalar(plugin.apiVersion()));
            writer.write('\n');
            if (!plugin.description().isBlank()) {
                writer.write("description: ");
                writer.write(yamlScalar(plugin.description()));
                writer.write('\n');
            }
            List<String> authors = plugin.authors();
            if (!authors.isEmpty()) {
                writer.write("authors: [");
                for (int i = 0; i < authors.size(); i++) {
                    if (i > 0) {
                        writer.write(", ");
                    }
                    writer.write(yamlScalar(authors.get(i)));
                }
                writer.write("]\n");
            }
        }
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
