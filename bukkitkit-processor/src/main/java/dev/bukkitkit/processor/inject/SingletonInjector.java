package dev.bukkitkit.processor.inject;

import dev.bukkitkit.api.BukkitKitSymbols;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.util.JavaC;
import dev.bukkitkit.processor.util.JavacContext;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

/**
 * Injects package-private singleton storage into a {@code @Component} class (no public accessor).
 */
public final class SingletonInjector {

    private final JavaC jc;

    public SingletonInjector(JavacContext javac) {
        this.jc = JavaC.of(javac);
    }

    public void inject(ComponentModel model) {
        JCClassDecl classDecl = jc.classAst(model.type());
        if (!(classDecl.sym instanceof Symbol.ClassSymbol classSymbol)) {
            throw new IllegalStateException("BukkitKit: missing ClassSymbol for " + model.typeName());
        }
        if (alreadyInjected(classDecl)) {
            return;
        }

        JavaC at = jc.at(classDecl.pos);
        Type classType = classSymbol.type;

        com.sun.tools.javac.util.List<JCTree> defs = classDecl.defs
                .append(createInstanceField(at, classSymbol, classType))
                .append(createBindMethod(at, classSymbol, classType))
                .append(createUnbindMethod(at, classSymbol));
        if (model.kind() == ComponentModel.InjectionKind.FIELD) {
            defs = defs.append(createWireMethod(at, classSymbol, classDecl, model));
        }
        classDecl.defs = defs;
    }

    private boolean alreadyInjected(JCClassDecl classDecl) {
        Name fieldName = jc.name(BukkitKitSymbols.INSTANCE_FIELD);
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCVariableDecl variable && variable.name.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private JCVariableDecl createInstanceField(JavaC jc, Symbol.ClassSymbol owner, Type classType) {
        return jc.field(BukkitKitSymbols.INSTANCE_FIELD, classType, owner)
                .publicStaticVolatile()
                .build();
    }

    private JCMethodDecl createBindMethod(JavaC jc, Symbol.ClassSymbol owner, Type classType) {
        JCExpression alreadyBound = jc.ne(
                jc.select(owner, BukkitKitSymbols.INSTANCE_FIELD),
                jc.nullLit());
        JCStatement ifBound = jc.ifThen(
                alreadyBound,
                jc.throw_(jc.newClass(
                        jc.qual("dev.bukkitkit.api.BukkitKitException"),
                        jc.lit("BukkitKit: " + owner.flatName() + " is already bound"))));
        JCStatement assign = jc.exec(jc.assign(
                jc.select(owner, BukkitKitSymbols.INSTANCE_FIELD),
                jc.id("instance")));

        return jc.method(BukkitKitSymbols.BIND_METHOD, owner)
                .publicStatic()
                .returnsVoid()
                .param("instance", classType)
                .body(ifBound, assign)
                .build();
    }

    private JCMethodDecl createUnbindMethod(JavaC jc, Symbol.ClassSymbol owner) {
        JCStatement assign = jc.exec(jc.assign(
                jc.select(owner, BukkitKitSymbols.INSTANCE_FIELD),
                jc.nullLit()));

        return jc.method(BukkitKitSymbols.UNBIND_METHOD, owner)
                .publicStatic()
                .returnsVoid()
                .body(assign)
                .build();
    }

    private JCMethodDecl createWireMethod(
            JavaC jc,
            Symbol.ClassSymbol owner,
            JCClassDecl classDecl,
            ComponentModel model) {
        JavaC.MethodBuilder method = jc.method(BukkitKitSymbols.WIRE_METHOD, owner)
                .makePublic()
                .returnsVoid();

        ListBuffer<JCStatement> stats = new ListBuffer<>();
        for (ComponentModel.WiredField wired : model.wiredFields()) {
            JCVariableDecl fieldDecl = findField(classDecl, wired.fieldName());
            if (fieldDecl == null || fieldDecl.sym == null) {
                throw new IllegalStateException(
                        "BukkitKit: missing field AST for " + model.typeName() + "." + wired.fieldName());
            }
            method.param(wired.fieldName(), fieldDecl.sym.type);
            stats.append(jc.exec(jc.assign(
                    jc.select(jc.this_(), wired.fieldName()),
                    jc.id(wired.fieldName()))));
        }

        return method.body(stats).build();
    }

    private static JCVariableDecl findField(JCClassDecl classDecl, String fieldName) {
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCVariableDecl variable && variable.name.contentEquals(fieldName)) {
                return variable;
            }
        }
        return null;
    }
}
