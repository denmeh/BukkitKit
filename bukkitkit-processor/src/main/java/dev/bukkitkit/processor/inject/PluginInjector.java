package dev.bukkitkit.processor.inject;

import dev.bukkitkit.api.BukkitKitSymbols;
import dev.bukkitkit.processor.builtins.BuiltInTypes;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.util.JavaC;
import dev.bukkitkit.processor.util.JavacContext;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Wires {@code @BukkitKit} plugins: {@code @Wire} field injection + onEnable/onDisable hooks.
 */
public final class PluginInjector {

    private static final String SUPPORT = "dev.bukkitkit.paper.BukkitKitSupport";
    private static final String PLATFORM = "dev.bukkitkit.paper.PlatformServices";

    private final JavaC jc;

    public PluginInjector(JavacContext javac) {
        this.jc = JavaC.of(javac);
    }

    public void inject(PluginModel model) {
        JCClassDecl classDecl = jc.classAst(model.type());
        if (!(classDecl.sym instanceof Symbol.ClassSymbol classSymbol)) {
            throw new IllegalStateException("BukkitKit: missing ClassSymbol for " + model.typeName());
        }

        JavaC at = jc.at(classDecl.pos);

        if (!hasMethodNamed(classDecl, BukkitKitSymbols.WIRE_METHOD)) {
            classDecl.defs = classDecl.defs.append(createWireMethod(at, classSymbol, model));
        }

        ensureLifecycleHooks(at, classDecl, classSymbol);
    }

    private void ensureLifecycleHooks(JavaC jc, JCClassDecl classDecl, Symbol.ClassSymbol classSymbol) {
        JCMethodDecl onEnable = findMethod(classDecl, "onEnable");
        if (onEnable == null) {
            classDecl.defs = classDecl.defs.append(createOnEnable(jc, classSymbol));
        } else {
            prependEnableHooks(jc, onEnable);
        }

        JCMethodDecl onDisable = findMethod(classDecl, "onDisable");
        if (onDisable == null) {
            classDecl.defs = classDecl.defs.append(createOnDisable(jc, classSymbol));
        } else {
            appendDisableHook(jc, onDisable);
        }
    }

    private void prependEnableHooks(JavaC jc, JCMethodDecl onEnable) {
        if (onEnable.body == null || alreadyHooked(onEnable.body, "enable")) {
            return;
        }

        ListBuffer<JCStatement> stats = new ListBuffer<>();
        stats.append(servicesLocal(jc));
        stats.append(jc.exec(jc.call(BukkitKitSymbols.WIRE_METHOD, jc.id("bukkitKitServices"))));
        for (JCStatement statement : onEnable.body.stats) {
            stats.append(statement);
        }
        onEnable.body = jc.block(stats);
    }

    private void appendDisableHook(JavaC jc, JCMethodDecl onDisable) {
        if (onDisable.body == null || alreadyHooked(onDisable.body, "disable")) {
            return;
        }
        JCBlock original = onDisable.body;
        onDisable.body = jc.block(jc.tryFinally(original, callSupport(jc, "disable")));
    }

    private boolean alreadyHooked(JCBlock body, String supportMethod) {
        for (JCStatement statement : body.stats) {
            if (statement.toString().contains("BukkitKitSupport." + supportMethod)) {
                return true;
            }
        }
        return false;
    }

    private JCMethodDecl createOnEnable(JavaC jc, Symbol.ClassSymbol owner) {
        return jc.method("onEnable", owner)
                .makePublic()
                .returnsVoid()
                .body(
                        servicesLocal(jc),
                        jc.exec(jc.call(BukkitKitSymbols.WIRE_METHOD, jc.id("bukkitKitServices"))))
                .build();
    }

    private JCMethodDecl createOnDisable(JavaC jc, Symbol.ClassSymbol owner) {
        return jc.method("onDisable", owner)
                .makePublic()
                .returnsVoid()
                .body(callSupport(jc, "disable"))
                .build();
    }

    private JCVariableDecl servicesLocal(JavaC jc) {
        return jc.local(
                "bukkitKitServices",
                jc.qual(PLATFORM),
                jc.call(jc.qual(SUPPORT), "enable", jc.this_()));
    }

    private JCStatement callSupport(JavaC jc, String method) {
        return jc.exec(jc.call(jc.qual(SUPPORT), method, jc.this_()));
    }

    private JCMethodDecl createWireMethod(JavaC jc, Symbol.ClassSymbol owner, PluginModel model) {
        ListBuffer<JCStatement> stats = new ListBuffer<>();
        for (PluginModel.InjectedField field : model.injectedFields()) {
            JCExpression value = switch (field.kind()) {
                case PLATFORM -> jc.call(
                        jc.id("s"),
                        BuiltInTypes.accessor(field.typeName()));
                case PLUGIN -> jc.call(
                        jc.id("s"),
                        "plugin",
                        jc.classLit(jc.qual(field.typeName())));
                case COMPONENT -> jc.select(
                        jc.qual(field.typeName()),
                        BukkitKitSymbols.INSTANCE_FIELD);
            };
            stats.append(jc.exec(jc.assign(
                    jc.select(jc.this_(), field.fieldName()),
                    value)));
        }

        return jc.method(BukkitKitSymbols.WIRE_METHOD, owner)
                .makePrivate()
                .returnsVoid()
                .param("s", jc.symtab().objectType, jc.qual(PLATFORM))
                .body(stats)
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

    private static boolean hasMethodNamed(JCClassDecl classDecl, String name) {
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCMethodDecl method && method.name.contentEquals(name)) {
                return true;
            }
        }
        return false;
    }
}
