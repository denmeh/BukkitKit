package dev.bukkitkit.processor.inject;

import dev.bukkitkit.api.BukkitKitSymbols;
import dev.bukkitkit.processor.builtins.BuiltInTypes;
import dev.bukkitkit.processor.model.PluginModel;
import dev.bukkitkit.processor.util.JavacContext;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * Wires {@code @BukkitKit} plugins: {@code @Wire} field injection + onEnable/onDisable hooks.
 */
public final class PluginInjector {

    private final JavacContext javac;
    private final Symtab symtab;
    private final JavacTrees trees;

    public PluginInjector(JavacContext javac) {
        this.javac = javac;
        this.symtab = Symtab.instance(javac.javacEnv().getContext());
        this.trees = JavacTrees.instance(javac.javacEnv());
    }

    public void inject(PluginModel model) {
        JCTree ast = trees.getTree(model.type());
        if (!(ast instanceof JCClassDecl classDecl)) {
            throw new IllegalStateException("BukkitKit: no class AST for " + model.typeName());
        }
        if (!(classDecl.sym instanceof Symbol.ClassSymbol classSymbol)) {
            throw new IllegalStateException("BukkitKit: missing ClassSymbol for " + model.typeName());
        }

        TreeMaker maker = javac.treeMaker().at(classDecl.pos);
        Names names = javac.names();

        if (!hasMethodNamed(classDecl, BukkitKitSymbols.WIRE_METHOD)) {
            classDecl.defs = classDecl.defs.append(createWireMethod(maker, names, classSymbol, model));
        }

        ensureLifecycleHooks(maker, names, classDecl, classSymbol);
    }

    private void ensureLifecycleHooks(
            TreeMaker maker,
            Names names,
            JCClassDecl classDecl,
            Symbol.ClassSymbol classSymbol) {
        JCMethodDecl onEnable = findMethod(classDecl, "onEnable");
        if (onEnable == null) {
            classDecl.defs = classDecl.defs.append(createOnEnable(maker, names, classSymbol));
        } else {
            prependEnableHooks(maker, names, onEnable);
        }

        JCMethodDecl onDisable = findMethod(classDecl, "onDisable");
        if (onDisable == null) {
            classDecl.defs = classDecl.defs.append(createOnDisable(maker, names, classSymbol));
        } else {
            appendDisableHook(maker, names, onDisable);
        }
    }

    private void prependEnableHooks(TreeMaker maker, Names names, JCMethodDecl onEnable) {
        if (onEnable.body == null || alreadyHooked(onEnable.body, "enable")) {
            return;
        }
        Name servicesName = names.fromString("bukkitKitServices");
        JCExpression support = qualify(maker, names, "dev", "bukkitkit", "paper", "BukkitKitSupport");
        JCExpression platformType = qualify(maker, names, "dev", "bukkitkit", "paper", "PlatformServices");

        JCVariableDecl services = maker.VarDef(
                maker.Modifiers(Flags.FINAL),
                servicesName,
                platformType,
                maker.Apply(
                        List.nil(),
                        maker.Select(support, names.fromString("enable")),
                        List.of(maker.Ident(names._this))));

        ListBuffer<JCStatement> stats = new ListBuffer<>();
        stats.append(services);
        stats.append(maker.Exec(maker.Apply(
                List.nil(),
                maker.Ident(names.fromString(BukkitKitSymbols.WIRE_METHOD)),
                List.of(maker.Ident(servicesName)))));
        for (JCStatement statement : onEnable.body.stats) {
            stats.append(statement);
        }
        onEnable.body = maker.Block(0, stats.toList());
    }

    private void appendDisableHook(TreeMaker maker, Names names, JCMethodDecl onDisable) {
        if (onDisable.body == null || alreadyHooked(onDisable.body, "disable")) {
            return;
        }
        JCBlock original = onDisable.body;
        JCStatement cleanup = callSupport(maker, names, "disable");
        onDisable.body = maker.Block(0, List.of(
                maker.Try(original, List.nil(), maker.Block(0, List.of(cleanup)))));
    }

    private boolean alreadyHooked(JCBlock body, String supportMethod) {
        for (JCStatement statement : body.stats) {
            if (statement.toString().contains("BukkitKitSupport." + supportMethod)) {
                return true;
            }
        }
        return false;
    }

    private JCMethodDecl createOnEnable(TreeMaker maker, Names names, Symbol.ClassSymbol classSymbol) {
        Name servicesName = names.fromString("bukkitKitServices");
        JCExpression support = qualify(maker, names, "dev", "bukkitkit", "paper", "BukkitKitSupport");
        JCExpression platformType = qualify(maker, names, "dev", "bukkitkit", "paper", "PlatformServices");
        JCVariableDecl services = maker.VarDef(
                maker.Modifiers(Flags.FINAL),
                servicesName,
                platformType,
                maker.Apply(
                        List.nil(),
                        maker.Select(support, names.fromString("enable")),
                        List.of(maker.Ident(names._this))));
        JCBlock body = maker.Block(0, List.of(
                services,
                maker.Exec(maker.Apply(
                        List.nil(),
                        maker.Ident(names.fromString(BukkitKitSymbols.WIRE_METHOD)),
                        List.of(maker.Ident(servicesName))))));
        return createLifecycleMethod(maker, names, classSymbol, "onEnable", body);
    }

    private JCMethodDecl createOnDisable(TreeMaker maker, Names names, Symbol.ClassSymbol classSymbol) {
        JCBlock body = maker.Block(0, List.of(callSupport(maker, names, "disable")));
        return createLifecycleMethod(maker, names, classSymbol, "onDisable", body);
    }

    private JCMethodDecl createLifecycleMethod(
            TreeMaker maker,
            Names names,
            Symbol.ClassSymbol classSymbol,
            String methodName,
            JCBlock body) {
        Name name = names.fromString(methodName);
        Type.MethodType methodType = new Type.MethodType(List.nil(), symtab.voidType, List.nil(), classSymbol);
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                name,
                methodType,
                classSymbol);
        classSymbol.members().enter(methodSymbol);
        JCMethodDecl method = maker.MethodDef(
                maker.Modifiers(Flags.PUBLIC),
                name,
                maker.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.nil(),
                List.nil(),
                body,
                null);
        method.sym = methodSymbol;
        return method;
    }

    private JCStatement callSupport(TreeMaker maker, Names names, String method) {
        JCExpression support = qualify(maker, names, "dev", "bukkitkit", "paper", "BukkitKitSupport");
        return maker.Exec(maker.Apply(
                List.nil(),
                maker.Select(support, names.fromString(method)),
                List.of(maker.Ident(names._this))));
    }

    private JCMethodDecl createWireMethod(
            TreeMaker maker,
            Names names,
            Symbol.ClassSymbol classSymbol,
            PluginModel model) {
        Name servicesName = names.fromString("s");
        JCExpression platformType = qualify(maker, names, "dev", "bukkitkit", "paper", "PlatformServices");

        Symbol.VarSymbol servicesSymbol = new Symbol.VarSymbol(
                Flags.PARAMETER | Flags.FINAL,
                servicesName,
                symtab.objectType,
                null);

        JCVariableDecl servicesParam = maker.VarDef(
                maker.Modifiers(Flags.PARAMETER | Flags.FINAL),
                servicesName,
                platformType,
                null);
        servicesParam.sym = servicesSymbol;

        ListBuffer<JCStatement> stats = new ListBuffer<>();
        for (PluginModel.InjectedField field : model.injectedFields()) {
            JCExpression value = switch (field.kind()) {
                case PLATFORM -> maker.Apply(
                        List.nil(),
                        maker.Select(
                                maker.Ident(servicesName),
                                names.fromString(BuiltInTypes.accessor(field.typeName()))),
                        List.nil());
                case PLUGIN -> maker.Apply(
                        List.nil(),
                        maker.Select(maker.Ident(servicesName), names.fromString("plugin")),
                        List.of(maker.Select(
                                qualify(maker, names, field.typeName().split("\\.")),
                                names._class)));
                case COMPONENT -> maker.Select(
                        qualify(maker, names, field.typeName().split("\\.")),
                        names.fromString(BukkitKitSymbols.INSTANCE_FIELD));
            };
            stats.append(maker.Exec(maker.Assign(
                    maker.Select(maker.Ident(names._this), names.fromString(field.fieldName())),
                    value)));
        }

        Name methodName = names.fromString(BukkitKitSymbols.WIRE_METHOD);
        Type.MethodType methodType = new Type.MethodType(
                List.of(symtab.objectType),
                symtab.voidType,
                List.nil(),
                classSymbol);
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                Flags.PRIVATE,
                methodName,
                methodType,
                classSymbol);
        servicesSymbol.owner = methodSymbol;
        methodSymbol.params = List.of(servicesSymbol);
        classSymbol.members().enter(methodSymbol);

        JCMethodDecl method = maker.MethodDef(
                maker.Modifiers(Flags.PRIVATE),
                methodName,
                maker.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.of(servicesParam),
                List.nil(),
                maker.Block(0, stats.toList()),
                null);
        method.sym = methodSymbol;
        return method;
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

    private static JCExpression qualify(TreeMaker maker, Names names, String... parts) {
        JCExpression expr = maker.Ident(names.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expr = maker.Select(expr, names.fromString(parts[i]));
        }
        return expr;
    }
}
