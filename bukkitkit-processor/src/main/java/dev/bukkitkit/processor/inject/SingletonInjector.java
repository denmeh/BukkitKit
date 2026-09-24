package dev.bukkitkit.processor.inject;

import dev.bukkitkit.api.BukkitKitSymbols;
import dev.bukkitkit.processor.model.ComponentModel;
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
 * Injects package-private singleton storage into a {@code @Component} class (no public accessor).
 */
public final class SingletonInjector {

    private final JavacContext javac;
    private final Symtab symtab;
    private final JavacTrees trees;

    public SingletonInjector(JavacContext javac) {
        this.javac = javac;
        this.symtab = Symtab.instance(javac.javacEnv().getContext());
        this.trees = JavacTrees.instance(javac.javacEnv());
    }

    public void inject(ComponentModel model) {
        JCTree ast = trees.getTree(model.type());
        if (!(ast instanceof JCClassDecl classDecl)) {
            throw new IllegalStateException("BukkitKit: no class AST for " + model.typeName());
        }
        if (!(classDecl.sym instanceof Symbol.ClassSymbol classSymbol)) {
            throw new IllegalStateException("BukkitKit: missing ClassSymbol for " + model.typeName());
        }
        if (alreadyInjected(classDecl)) {
            return;
        }

        TreeMaker maker = javac.treeMaker().at(classDecl.pos);
        Names names = javac.names();
        Type classType = classSymbol.type;

        com.sun.tools.javac.util.List<JCTree> defs = classDecl.defs
                .append(createInstanceField(maker, names, classSymbol, classType))
                .append(createBindMethod(maker, names, classSymbol, classType))
                .append(createUnbindMethod(maker, names, classSymbol, classType));
        if (model.kind() == ComponentModel.InjectionKind.FIELD) {
            defs = defs.append(createWireMethod(maker, names, classSymbol, classDecl, model));
        }
        classDecl.defs = defs;
    }

    private boolean alreadyInjected(JCClassDecl classDecl) {
        Name fieldName = javac.names().fromString(BukkitKitSymbols.INSTANCE_FIELD);
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCVariableDecl variable && variable.name.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private JCVariableDecl createInstanceField(
            TreeMaker maker,
            Names names,
            Symbol.ClassSymbol classSymbol,
            Type classType) {
        Name fieldName = names.fromString(BukkitKitSymbols.INSTANCE_FIELD);
        // public so generated bootstrap / plugin wire can access across packages
        long flags = Flags.PUBLIC | Flags.STATIC | Flags.VOLATILE;
        Symbol.VarSymbol symbol = new Symbol.VarSymbol(flags, fieldName, classType, classSymbol);
        classSymbol.members().enter(symbol);
        return maker.VarDef(symbol, null);
    }

    private JCMethodDecl createBindMethod(
            TreeMaker maker,
            Names names,
            Symbol.ClassSymbol classSymbol,
            Type classType) {
        Name methodName = names.fromString(BukkitKitSymbols.BIND_METHOD);
        Name fieldName = names.fromString(BukkitKitSymbols.INSTANCE_FIELD);
        Name paramName = names.fromString("instance");

        Symbol.VarSymbol paramSymbol = new Symbol.VarSymbol(
                Flags.PARAMETER | Flags.FINAL,
                paramName,
                classType,
                null);

        JCExpression alreadyBound = maker.Binary(
                JCTree.Tag.NE,
                maker.Select(maker.Ident(classSymbol), fieldName),
                maker.Literal(TypeTag.BOT, null));
        JCStatement throwStmt = maker.Throw(newException(
                maker,
                names,
                "BukkitKit: " + classSymbol.flatName() + " is already bound"));
        JCStatement ifBound = maker.If(alreadyBound, maker.Block(0, List.of(throwStmt)), null);
        JCStatement assign = maker.Exec(maker.Assign(
                maker.Select(maker.Ident(classSymbol), fieldName),
                maker.Ident(paramName)));
        JCBlock body = maker.Block(0, List.of(ifBound, assign));

        Type.MethodType methodType = new Type.MethodType(
                List.of(classType),
                symtab.voidType,
                List.nil(),
                classSymbol);
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                Flags.PUBLIC | Flags.STATIC,
                methodName,
                methodType,
                classSymbol);
        paramSymbol.owner = methodSymbol;
        methodSymbol.params = List.of(paramSymbol);
        classSymbol.members().enter(methodSymbol);

        JCVariableDecl param = maker.VarDef(
                maker.Modifiers(Flags.PARAMETER | Flags.FINAL),
                paramName,
                maker.Type(classType),
                null);
        param.sym = paramSymbol;

        JCMethodDecl method = maker.MethodDef(
                maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
                methodName,
                maker.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.of(param),
                List.nil(),
                body,
                null);
        method.sym = methodSymbol;
        return method;
    }

    private JCMethodDecl createUnbindMethod(
            TreeMaker maker,
            Names names,
            Symbol.ClassSymbol classSymbol,
            Type classType) {
        Name methodName = names.fromString(BukkitKitSymbols.UNBIND_METHOD);
        Name fieldName = names.fromString(BukkitKitSymbols.INSTANCE_FIELD);

        JCStatement assign = maker.Exec(maker.Assign(
                maker.Select(maker.Ident(classSymbol), fieldName),
                maker.Literal(TypeTag.BOT, null)));
        JCBlock body = maker.Block(0, List.of(assign));

        Type.MethodType methodType = new Type.MethodType(List.nil(), symtab.voidType, List.nil(), classSymbol);
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                Flags.PUBLIC | Flags.STATIC,
                methodName,
                methodType,
                classSymbol);
        classSymbol.members().enter(methodSymbol);
        return maker.MethodDef(methodSymbol, body);
    }

    private JCExpression newException(TreeMaker maker, Names names, String message) {
        JCExpression type = qualify(maker, names, "dev", "bukkitkit", "api", "BukkitKitException");
        return maker.NewClass(
                null,
                List.nil(),
                type,
                List.of(maker.Literal(message)),
                null);
    }

    private JCMethodDecl createWireMethod(
            TreeMaker maker,
            Names names,
            Symbol.ClassSymbol classSymbol,
            JCClassDecl classDecl,
            ComponentModel model) {
        ListBuffer<JCVariableDecl> params = new ListBuffer<>();
        ListBuffer<Type> paramTypes = new ListBuffer<>();
        ListBuffer<JCStatement> stats = new ListBuffer<>();
        ListBuffer<Symbol.VarSymbol> paramSymbols = new ListBuffer<>();

        for (ComponentModel.WiredField wired : model.wiredFields()) {
            JCVariableDecl fieldDecl = findField(classDecl, wired.fieldName());
            if (fieldDecl == null || fieldDecl.sym == null) {
                throw new IllegalStateException(
                        "BukkitKit: missing field AST for " + model.typeName() + "." + wired.fieldName());
            }
            Type fieldType = fieldDecl.sym.type;
            Name paramName = names.fromString(wired.fieldName());
            Symbol.VarSymbol paramSymbol = new Symbol.VarSymbol(
                    Flags.PARAMETER | Flags.FINAL,
                    paramName,
                    fieldType,
                    null);
            paramSymbols.append(paramSymbol);
            paramTypes.append(fieldType);

            JCVariableDecl param = maker.VarDef(
                    maker.Modifiers(Flags.PARAMETER | Flags.FINAL),
                    paramName,
                    maker.Type(fieldType),
                    null);
            param.sym = paramSymbol;
            params.append(param);

            stats.append(maker.Exec(maker.Assign(
                    maker.Select(maker.Ident(names._this), names.fromString(wired.fieldName())),
                    maker.Ident(paramName))));
        }

        Name methodName = names.fromString(BukkitKitSymbols.WIRE_METHOD);
        Type.MethodType methodType = new Type.MethodType(
                paramTypes.toList(),
                symtab.voidType,
                List.nil(),
                classSymbol);
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                methodName,
                methodType,
                classSymbol);
        methodSymbol.params = paramSymbols.toList();
        for (Symbol.VarSymbol paramSymbol : paramSymbols) {
            paramSymbol.owner = methodSymbol;
        }
        classSymbol.members().enter(methodSymbol);

        JCMethodDecl method = maker.MethodDef(
                maker.Modifiers(Flags.PUBLIC),
                methodName,
                maker.TypeIdent(TypeTag.VOID),
                List.nil(),
                params.toList(),
                List.nil(),
                maker.Block(0, stats.toList()),
                null);
        method.sym = methodSymbol;
        return method;
    }

    private static JCVariableDecl findField(JCClassDecl classDecl, String fieldName) {
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCVariableDecl variable && variable.name.contentEquals(fieldName)) {
                return variable;
            }
        }
        return null;
    }

    private static JCExpression qualify(TreeMaker maker, Names names, String... parts) {
        JCExpression expr = maker.Ident(names.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expr = maker.Select(expr, names.fromString(parts[i]));
        }
        return expr;
    }
}
