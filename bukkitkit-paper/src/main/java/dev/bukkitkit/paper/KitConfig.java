package dev.bukkitkit.paper;

import dev.bukkitkit.api.BukkitKitException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads public fields on a {@code @Config} instance from YAML and writes defaults
 * when the file is missing or keys are absent. Nested POJO fields become YAML sections.
 */
public final class KitConfig {

    private KitConfig() {
    }

    /**
     * Bind {@code instance} to {@code fileName} under the plugin data folder.
     * Creates the file from Java defaults when it does not exist; otherwise loads
     * present keys and appends any missing defaults back to disk.
     */
    public static void bind(JavaPlugin plugin, Object instance, String fileName) {
        if (plugin == null) {
            throw new BukkitKitException("BukkitKit: config bind requires a JavaPlugin");
        }
        if (instance == null) {
            throw new BukkitKitException("BukkitKit: config bind requires a non-null instance");
        }
        validateRelativeFileName(fileName);

        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new BukkitKitException(
                        "BukkitKit: could not create data folder for " + plugin.getName());
            }
            File file = resolveUnderDataFolder(dataFolder, fileName);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new BukkitKitException(
                        "BukkitKit: could not create config directory for '" + fileName + "'");
            }

            FileConfiguration yaml = new YamlConfiguration();
            boolean existed = file.isFile();
            if (existed) {
                yaml.load(file);
            }

            boolean dirty = apply(instance, yaml, existed);
            if (dirty || !existed) {
                yaml.save(file);
            }
        } catch (BukkitKitException ex) {
            throw ex;
        } catch (IOException | InvalidConfigurationException ex) {
            throw new BukkitKitException(
                    "BukkitKit: failed to bind config '" + fileName + "' for " + instance.getClass().getName(),
                    ex);
        } catch (ReflectiveOperationException ex) {
            throw new BukkitKitException(
                    "BukkitKit: failed to read/write config fields on " + instance.getClass().getName(),
                    ex);
        }
    }

    /**
     * Ensures {@code fileName} is a relative path under the plugin data folder
     * (no absolute paths, no {@code ..} segments).
     */
    static void validateRelativeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BukkitKitException("BukkitKit: config file name must not be blank");
        }
        if (fileName.indexOf('\0') >= 0) {
            throw new BukkitKitException("BukkitKit: config file name is invalid: " + fileName);
        }
        Path path = Path.of(fileName);
        if (path.isAbsolute()) {
            throw new BukkitKitException(
                    "BukkitKit: config file must be relative to the plugin data folder: " + fileName);
        }
        Path normalized = path.normalize();
        if (normalized.startsWith("..") || normalized.toString().equals("..")) {
            throw new BukkitKitException(
                    "BukkitKit: config file must not contain '..': " + fileName);
        }
        if (normalized.toString().isEmpty() || normalized.toString().equals(".")) {
            throw new BukkitKitException("BukkitKit: config file name must not be blank");
        }
    }

    static File resolveUnderDataFolder(File dataFolder, String fileName) {
        validateRelativeFileName(fileName);
        File file = new File(dataFolder, fileName).getAbsoluteFile();
        File root = dataFolder.getAbsoluteFile();
        if (!file.toPath().normalize().startsWith(root.toPath().normalize())) {
            throw new BukkitKitException(
                    "BukkitKit: config file escapes the plugin data folder: " + fileName);
        }
        return file;
    }

    /**
     * @param existed {@code true} when loading an existing file (read values); {@code false} when seeding defaults
     * @return {@code true} when the YAML gained new keys and should be saved
     */
    static boolean apply(Object instance, ConfigurationSection section, boolean existed)
            throws ReflectiveOperationException {
        return apply(instance, section, existed, new HashSet<>());
    }

    private static boolean apply(
            Object instance,
            ConfigurationSection section,
            boolean existed,
            Set<Class<?>> visiting)
            throws ReflectiveOperationException {
        Class<?> type = instance.getClass();
        if (!visiting.add(type)) {
            throw new BukkitKitException(
                    "BukkitKit: cyclic nested config type " + type.getName());
        }
        try {
            boolean dirty = false;
            for (Field field : configFields(type)) {
                String key = field.getName();
                Class<?> fieldType = field.getType();

                if (isNestedConfigType(fieldType)) {
                    dirty |= applyNested(instance, field, key, section, existed, visiting);
                    continue;
                }

                if (!existed || !section.contains(key)) {
                    setYamlValue(section, key, toYamlValue(field.get(instance), new HashSet<>()));
                    dirty = true;
                    continue;
                }
                Object raw = section.get(key);
                Object coerced = coerce(field, raw, key);
                field.set(instance, coerced);
            }
            return dirty;
        } finally {
            visiting.remove(type);
        }
    }

    private static boolean applyNested(
            Object parent,
            Field field,
            String key,
            ConfigurationSection section,
            boolean existed,
            Set<Class<?>> visiting)
            throws ReflectiveOperationException {
        Object nested = field.get(parent);
        if (nested == null) {
            nested = newNestedInstance(field.getType(), key);
            field.set(parent, nested);
        }

        if (!existed || !section.contains(key)) {
            setYamlValue(section, key, toYamlValue(nested, new HashSet<>(visiting)));
            return true;
        }

        Object raw = section.get(key);
        if (raw == null) {
            setYamlValue(section, key, toYamlValue(nested, new HashSet<>(visiting)));
            return true;
        }
        if (raw instanceof ConfigurationSection child) {
            return apply(nested, child, true, visiting);
        }
        if (raw instanceof Map<?, ?>) {
            ConfigurationSection child = asSection(raw, key);
            boolean childDirty = apply(nested, child, true, visiting);
            if (childDirty) {
                // Map values are not linked to the parent section — write the merged object back.
                setYamlValue(section, key, toYamlValue(nested, new HashSet<>(visiting)));
            }
            return childDirty;
        }
        throw new BukkitKitException(
                "BukkitKit: config key '" + key + "' expected a section but got "
                        + raw.getClass().getSimpleName());
    }

    /**
     * Writes a value into {@code section}. Nested {@link Map}s become real
     * {@link ConfigurationSection}s so dotted-path reads work.
     */
    private static void setYamlValue(ConfigurationSection section, String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            section.createSection(key, map);
            return;
        }
        section.set(key, value);
    }

    private static Object newNestedInstance(Class<?> type, String key) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new BukkitKitException(
                    "BukkitKit: nested config '" + key + "' type " + type.getName()
                            + " needs a public no-arg constructor",
                    ex);
        }
    }

    private static ConfigurationSection asSection(Object raw, String key) {
        if (raw instanceof ConfigurationSection configurationSection) {
            return configurationSection;
        }
        if (raw instanceof Map<?, ?> map) {
            MemoryConfiguration memory = new MemoryConfiguration();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                memory.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            return memory;
        }
        throw new BukkitKitException(
                "BukkitKit: config key '" + key + "' expected a section but got "
                        + (raw == null ? "null" : raw.getClass().getSimpleName()));
    }

    private static List<Field> configFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isFinal(mods) || !Modifier.isPublic(mods)) {
                continue;
            }
            fields.add(field);
        }
        return fields;
    }

    private static Object toYamlValue(Object value, Set<Class<?>> visiting)
            throws ReflectiveOperationException {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        Class<?> type = value.getClass();
        if (isLeafType(type)) {
            return value;
        }
        if (!visiting.add(type)) {
            throw new BukkitKitException(
                    "BukkitKit: cyclic nested config type " + type.getName());
        }
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Field field : configFields(type)) {
                map.put(field.getName(), toYamlValue(field.get(value), visiting));
            }
            return map;
        } finally {
            visiting.remove(type);
        }
    }

    private static Object coerce(Field field, Object raw, String key) {
        Class<?> type = field.getType();
        if (raw == null) {
            if (type.isPrimitive()) {
                throw new BukkitKitException(
                        "BukkitKit: config key '" + key + "' is null but field type is primitive "
                                + type.getName());
            }
            return null;
        }

        if (type == String.class) {
            return String.valueOf(raw);
        }
        if (type == boolean.class || type == Boolean.class) {
            if (raw instanceof Boolean b) {
                return b;
            }
            throw typeError(key, type, raw);
        }
        if (type == int.class || type == Integer.class) {
            return toNumber(raw, key, type).intValue();
        }
        if (type == long.class || type == Long.class) {
            return toNumber(raw, key, type).longValue();
        }
        if (type == double.class || type == Double.class) {
            return toNumber(raw, key, type).doubleValue();
        }
        if (type == float.class || type == Float.class) {
            return toNumber(raw, key, type).floatValue();
        }
        if (List.class.isAssignableFrom(type)) {
            return coerceStringList(field, raw, key);
        }
        throw new BukkitKitException(
                "BukkitKit: unsupported config field type " + type.getName() + " for key '" + key + "'");
    }

    private static boolean isLeafType(Class<?> type) {
        return type == String.class
                || type == Boolean.class
                || type == boolean.class
                || type == Integer.class
                || type == int.class
                || type == Long.class
                || type == long.class
                || type == Double.class
                || type == double.class
                || type == Float.class
                || type == float.class
                || List.class.isAssignableFrom(type);
    }

    /**
     * Nested config section: concrete public class with public fields (not a leaf / list).
     */
    static boolean isNestedConfigType(Class<?> type) {
        if (type.isPrimitive() || type.isArray() || type.isEnum() || type.isInterface()
                || Modifier.isAbstract(type.getModifiers())
                || isLeafType(type)) {
            return false;
        }
        if (!Modifier.isPublic(type.getModifiers())) {
            return false;
        }
        return !configFields(type).isEmpty();
    }

    private static Number toNumber(Object raw, String key, Class<?> type) {
        if (raw instanceof Number number) {
            return number;
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ex) {
                throw typeError(key, type, raw);
            }
        }
        throw typeError(key, type, raw);
    }

    private static List<String> coerceStringList(Field field, Object raw, String key) {
        if (!isStringList(field)) {
            throw new BukkitKitException(
                    "BukkitKit: config List fields must be List<String> (key '" + key + "')");
        }
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object element : list) {
                out.add(element == null ? null : String.valueOf(element));
            }
            return out;
        }
        throw typeError(key, List.class, raw);
    }

    private static boolean isStringList(Field field) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType parameterized)) {
            return false;
        }
        Type[] args = parameterized.getActualTypeArguments();
        return args.length == 1 && args[0] == String.class;
    }

    private static BukkitKitException typeError(String key, Class<?> expected, Object raw) {
        return new BukkitKitException(
                "BukkitKit: config key '" + key + "' expected " + expected.getSimpleName()
                        + " but got " + (raw == null ? "null" : raw.getClass().getSimpleName()));
    }
}
