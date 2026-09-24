package dev.bukkitkit.processor.inject;

import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.util.JavaC;
import dev.bukkitkit.processor.util.JavacContext;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Turns a {@code @BukkitKit} marker into a {@code JavaPlugin} with enable/disable hooks.
 */
public final class PluginInjector {

    private static final String SUPPORT = "dev.bukkitkit.paper.BukkitKitSupport";
    private static final String JAVA_PLUGIN = "org.bukkit.plugin.java.JavaPlugin";

    private final JavaC jc;
    private final Elements elements;

    public PluginInjector(JavacContext javac, Elements elements) {
        this.jc = JavaC.of(javac);
        this.elements = elements;
    }

    public void inject(PluginModel model) {
        JCClassDecl classDecl = jc.classAst(model.type());
        if (!(classDecl.sym instanceof Symbol.ClassSymbol classSymbol)) {
            throw new IllegalStateException("BukkitKit: missing ClassSymbol for " + model.typeName());
        }

        JavaC at = jc.at(classDecl.pos);
        ensureExtendsJavaPlugin(at, classDecl, classSymbol);
        ensureLifecycleHooks(at, classDecl, classSymbol);
    }

    private void ensureExtendsJavaPlugin(JavaC jc, JCClassDecl classDecl, Symbol.ClassSymbol classSymbol) {
        if (classDecl.extending != null && classDecl.extending.toString().contains("JavaPlugin")) {
            return;
        }
        classDecl.extending = jc.qual(JAVA_PLUGIN);

        TypeElement javaPluginElement = elements.getTypeElement(JAVA_PLUGIN);
        if (javaPluginElement instanceof Symbol.ClassSymbol javaPlugin
                && classSymbol.type instanceof Type.ClassType classType) {
            classType.supertype_field = javaPlugin.type;
        }
    }

    private void ensureLifecycleHooks(JavaC jc, JCClassDecl classDecl, Symbol.ClassSymbol classSymbol) {
        if (findMethod(classDecl, "onEnable") == null) {
            classDecl.defs = classDecl.defs.append(createOnEnable(jc, classSymbol));
        }
        if (findMethod(classDecl, "onDisable") == null) {
            classDecl.defs = classDecl.defs.append(createOnDisable(jc, classSymbol));
        }
    }

    private JCMethodDecl createOnEnable(JavaC jc, Symbol.ClassSymbol owner) {
        return jc.method("onEnable", owner)
                .makePublic()
                .returnsVoid()
                .body(jc.exec(jc.call(jc.qual(SUPPORT), "enable", jc.this_())))
                .build();
    }

    private JCMethodDecl createOnDisable(JavaC jc, Symbol.ClassSymbol owner) {
        return jc.method("onDisable", owner)
                .makePublic()
                .returnsVoid()
                .body(jc.exec(jc.call(jc.qual(SUPPORT), "disable", jc.this_())))
                .build();
    }

    private static JCMethodDecl findMethod(JCClassDecl classDecl, String name) {
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCMethodDecl method && method.name.contentEquals(name)
                    && method.params.isEmpty()) {
                return method;
            }
        }
        return null;
    }
}
