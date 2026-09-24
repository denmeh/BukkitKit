package dev.bukkitkit.processor.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * Opens {@code jdk.compiler} packages to this processor (Lombok-style),
 * so consumers do not need {@code --add-exports}/{@code --add-opens}.
 */
public final class JavacOpener {

    private static final String[] PACKAGES = {
            "com.sun.tools.javac.api",
            "com.sun.tools.javac.code",
            "com.sun.tools.javac.model",
            "com.sun.tools.javac.processing",
            "com.sun.tools.javac.tree",
            "com.sun.tools.javac.util",
            "com.sun.tools.javac.comp",
            "com.sun.tools.javac.main",
            "com.sun.tools.javac.jvm",
    };

    private static boolean opened;

    private JavacOpener() {
    }

    public static synchronized void open() {
        if (opened) {
            return;
        }
        Module jdkCompiler = ModuleLayer.boot().findModule("jdk.compiler").orElse(null);
        if (jdkCompiler == null) {
            opened = true;
            return;
        }
        try {
            MethodHandle addOpens = implAddOpensHandle();
            Module own = JavacOpener.class.getModule();
            for (String pkg : PACKAGES) {
                addOpens.invokeExact(jdkCompiler, pkg, own);
            }
            ClassLoader loader = JavacOpener.class.getClassLoader();
            if (loader != null) {
                Module unnamed = loader.getUnnamedModule();
                for (String pkg : PACKAGES) {
                    addOpens.invokeExact(jdkCompiler, pkg, unnamed);
                }
            }
        } catch (Throwable ex) {
            throw new IllegalStateException(
                    "BukkitKit: failed to open jdk.compiler internals. Use a full JDK.", ex);
        }
        opened = true;
    }

    private static MethodHandle implAddOpensHandle() throws Throwable {
        MethodHandles.Lookup trusted = trustedLookup();
        return trusted.findVirtual(
                Module.class,
                "implAddOpens",
                MethodType.methodType(void.class, String.class, Module.class));
    }

    /**
     * Obtain the privileged {@code MethodHandles.Lookup} via Unsafe (same idea as Lombok).
     */
    private static MethodHandles.Lookup trustedLookup() throws ReflectiveOperationException {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        Object unsafe = theUnsafeField.get(null);

        Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        Object base = unsafeClass.getMethod("staticFieldBase", Field.class).invoke(unsafe, implLookupField);
        long offset = (Long) unsafeClass.getMethod("staticFieldOffset", Field.class)
                .invoke(unsafe, implLookupField);
        return (MethodHandles.Lookup) unsafeClass
                .getMethod("getObject", Object.class, long.class)
                .invoke(unsafe, base, offset);
    }
}
