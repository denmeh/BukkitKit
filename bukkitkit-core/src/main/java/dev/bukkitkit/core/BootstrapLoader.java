package dev.bukkitkit.core;

import dev.bukkitkit.api.BukkitKitSymbols;
import dev.bukkitkit.api.BukkitKitException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads bootstrap implementation FQCNs from {@link BukkitKitSymbols#BOOTSTRAP_RESOURCE}.
 */
public final class BootstrapLoader {

    private BootstrapLoader() {
    }

    /**
     * Instantiates every bootstrap listed for {@code classLoader}.
     *
     * @param type expected bootstrap interface/class (e.g. paper {@code KitBootstrap})
     */
    public static <T> List<T> load(ClassLoader classLoader, Class<T> type) {
        List<T> bootstraps = new ArrayList<>();
        for (String className : readNames(classLoader)) {
            bootstraps.add(instantiate(className, classLoader, type));
        }
        return bootstraps;
    }

    static List<String> readNames(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources(BukkitKitSymbols.BOOTSTRAP_RESOURCE);
            List<String> names = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        names.add(trimmed);
                    }
                }
            }
            return names;
        } catch (IOException ex) {
            throw new BukkitKitException(
                    "BukkitKit: failed to read " + BukkitKitSymbols.BOOTSTRAP_RESOURCE, ex);
        }
    }

    private static <T> T instantiate(String className, ClassLoader classLoader, Class<T> type) {
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            if (!type.isAssignableFrom(clazz)) {
                throw new BukkitKitException(
                        "BukkitKit: " + className + " does not implement " + type.getName());
            }
            return type.cast(clazz.getDeclaredConstructor().newInstance());
        } catch (BukkitKitException ex) {
            throw ex;
        } catch (ReflectiveOperationException ex) {
            throw new BukkitKitException(
                    "BukkitKit: failed to load bootstrap " + className, ex);
        }
    }
}
