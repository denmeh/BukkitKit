package dev.bukkitkit.processor.util;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * Unwraps the javac processing environment for AST mutation.
 */
public final class JavacContext {

    private final TreeMaker treeMaker;
    private final Names names;
    private final JavacProcessingEnvironment javacEnv;

    private JavacContext(JavacProcessingEnvironment javacEnv, TreeMaker treeMaker, Names names) {
        this.javacEnv = javacEnv;
        this.treeMaker = treeMaker;
        this.names = names;
    }

    public static JavacContext from(ProcessingEnvironment processingEnvironment) {
        ProcessingEnvironment unwrapped = unwrap(processingEnvironment);
        if (!(unwrapped instanceof JavacProcessingEnvironment javacEnv)) {
            throw new IllegalStateException(
                    "BukkitKit processor requires javac (JavacProcessingEnvironment), got "
                            + unwrapped.getClass().getName());
        }
        Context context = javacEnv.getContext();
        return new JavacContext(javacEnv, TreeMaker.instance(context), Names.instance(context));
    }

    public TreeMaker treeMaker() {
        return treeMaker;
    }

    public Names names() {
        return names;
    }

    public JavacProcessingEnvironment javacEnv() {
        return javacEnv;
    }

    private static ProcessingEnvironment unwrap(ProcessingEnvironment env) {
        // IntelliJ and some build tools wrap the environment in a proxy / delegate.
        try {
            if (env.getClass().getName().contains("Proxy") || env.getClass().getName().contains("Ide")) {
                var field = env.getClass().getDeclaredField("delegate");
                field.setAccessible(true);
                Object delegate = field.get(env);
                if (delegate instanceof ProcessingEnvironment processingEnvironment) {
                    return unwrap(processingEnvironment);
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // use env as-is
        }
        try {
            var method = env.getClass().getMethod("getProcessingEnvironment");
            Object inner = method.invoke(env);
            if (inner instanceof ProcessingEnvironment processingEnvironment && inner != env) {
                return unwrap(processingEnvironment);
            }
        } catch (ReflectiveOperationException ignored) {
            // use env as-is
        }
        return env;
    }
}
