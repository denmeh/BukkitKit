package dev.bukkitkit.processor.util;

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
 * Readable facade over javac {@link TreeMaker} for AST injection.
 *
 * <pre>{@code
 * JavaC jc = JavaC.of(ctx).at(classDecl.pos);
 * JCStatement assign = jc.exec(jc.assign(jc.select(type, "field"), jc.id("value")));
 * JCMethodDecl bind = jc.method("__bukkitKit_bind", owner)
 *         .publicStatic()
 *         .returnsVoid()
 *         .param("instance", classType)
 *         .body(ifBound, assign)
 *         .build();
 * }</pre>
 */
public final class JavaC {

    private final TreeMaker maker;
    private final Names names;
    private final Symtab symtab;
    private final JavacTrees trees;

    private JavaC(TreeMaker maker, Names names, Symtab symtab, JavacTrees trees) {
        this.maker = maker;
        this.names = names;
        this.symtab = symtab;
        this.trees = trees;
    }

    public static JavaC of(JavacContext ctx) {
        return new JavaC(
                ctx.treeMaker(),
                ctx.names(),
                Symtab.instance(ctx.javacEnv().getContext()),
                JavacTrees.instance(ctx.javacEnv()));
    }

    public JavaC at(int pos) {
        return new JavaC(maker.at(pos), names, symtab, trees);
    }

    public JCClassDecl classAst(javax.lang.model.element.TypeElement type) {
        JCTree ast = trees.getTree(type);
        if (!(ast instanceof JCClassDecl classDecl)) {
            throw new IllegalStateException("BukkitKit: no class AST for " + type.getQualifiedName());
        }
        return classDecl;
    }

    public Symtab symtab() {
        return symtab;
    }

    public Names names() {
        return names;
    }

    // ── names / types ──────────────────────────────────────────────────

    public Name name(String s) {
        return names.fromString(s);
    }

    public JCExpression type(Type type) {
        return maker.Type(type);
    }

    public JCExpression voidType() {
        return maker.TypeIdent(TypeTag.VOID);
    }

    // ── expressions ────────────────────────────────────────────────────

    public JCExpression id(String name) {
        return maker.Ident(this.name(name));
    }

    public JCExpression id(Name name) {
        return maker.Ident(name);
    }

    public JCExpression id(Symbol symbol) {
        return maker.Ident(symbol);
    }

    public JCExpression this_() {
        return maker.Ident(names._this);
    }

    public JCExpression nullLit() {
        return maker.Literal(TypeTag.BOT, null);
    }

    public JCExpression lit(String value) {
        return maker.Literal(value);
    }

    public JCExpression lit(boolean value) {
        return maker.Literal(value);
    }

    public JCExpression lit(long value) {
        return maker.Literal(value);
    }

    /** Qualified name: {@code a.b.C} or a single identifier. */
    public JCExpression qual(String fqcn) {
        return qual(fqcn.split("\\."));
    }

    public JCExpression qual(String... parts) {
        if (parts.length == 0) {
            throw new IllegalArgumentException("empty qualified name");
        }
        JCExpression expr = id(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            expr = select(expr, parts[i]);
        }
        return expr;
    }

    public JCExpression select(JCExpression receiver, String member) {
        return maker.Select(receiver, name(member));
    }

    public JCExpression select(JCExpression receiver, Name member) {
        return maker.Select(receiver, member);
    }

    public JCExpression select(Symbol.ClassSymbol type, String member) {
        return select(id(type), member);
    }

    public JCExpression qualIdent(Symbol symbol) {
        return maker.QualIdent(symbol);
    }

    public JCExpression classLit(JCExpression typeExpr) {
        return select(typeExpr, names._class);
    }

    public JCExpression ne(JCExpression left, JCExpression right) {
        return maker.Binary(JCTree.Tag.NE, left, right);
    }

    public JCExpression eq(JCExpression left, JCExpression right) {
        return maker.Binary(JCTree.Tag.EQ, left, right);
    }

    public JCExpression assign(JCExpression lhs, JCExpression rhs) {
        return maker.Assign(lhs, rhs);
    }

    public JCExpression call(JCExpression method, JCExpression... args) {
        return maker.Apply(List.nil(), method, list(args));
    }

    public JCExpression call(String method, JCExpression... args) {
        return call(id(method), args);
    }

    public JCExpression call(JCExpression receiver, String method, JCExpression... args) {
        return call(select(receiver, method), args);
    }

    public JCExpression newClass(JCExpression type, JCExpression... args) {
        return maker.NewClass(null, List.nil(), type, list(args), null);
    }

    // ── statements ─────────────────────────────────────────────────────

    public JCStatement exec(JCExpression expression) {
        return maker.Exec(expression);
    }

    public JCStatement throw_(JCExpression expression) {
        return maker.Throw(expression);
    }

    public JCStatement ifThen(JCExpression condition, JCStatement then) {
        return maker.If(condition, then instanceof JCBlock ? then : block(then), null);
    }

    public JCStatement tryFinally(JCBlock body, JCStatement... cleanup) {
        return maker.Try(body, List.nil(), block(cleanup));
    }

    public JCBlock block(JCStatement... statements) {
        return maker.Block(0, list(statements));
    }

    public JCBlock block(Iterable<? extends JCStatement> statements) {
        ListBuffer<JCStatement> buffer = new ListBuffer<>();
        for (JCStatement statement : statements) {
            buffer.append(statement);
        }
        return maker.Block(0, buffer.toList());
    }

    public JCVariableDecl local(String name, JCExpression type, JCExpression init) {
        return maker.VarDef(maker.Modifiers(Flags.FINAL), this.name(name), type, init);
    }

    // ── members ────────────────────────────────────────────────────────

    public FieldBuilder field(String name, Type type, Symbol.ClassSymbol owner) {
        return new FieldBuilder(name, type, owner);
    }

    public MethodBuilder method(String name, Symbol.ClassSymbol owner) {
        return new MethodBuilder(name, owner);
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        ListBuffer<T> buffer = new ListBuffer<>();
        for (T item : items) {
            buffer.append(item);
        }
        return buffer.toList();
    }

    // ── builders ───────────────────────────────────────────────────────

    public final class FieldBuilder {
        private final String fieldName;
        private final Type type;
        private final Symbol.ClassSymbol owner;
        private long flags = Flags.PUBLIC | Flags.STATIC;

        private FieldBuilder(String fieldName, Type type, Symbol.ClassSymbol owner) {
            this.fieldName = fieldName;
            this.type = type;
            this.owner = owner;
        }

        public FieldBuilder flags(long flags) {
            this.flags = flags;
            return this;
        }

        public FieldBuilder publicStaticVolatile() {
            return flags(Flags.PUBLIC | Flags.STATIC | Flags.VOLATILE);
        }

        public JCVariableDecl build() {
            Name n = name(fieldName);
            Symbol.VarSymbol symbol = new Symbol.VarSymbol(flags, n, type, owner);
            owner.members().enter(symbol);
            return maker.VarDef(symbol, null);
        }
    }

    public final class MethodBuilder {
        private final String methodName;
        private final Symbol.ClassSymbol owner;
        private long flags = Flags.PUBLIC;
        private Type returnType = symtab.voidType;
        private boolean voidReturn = true;
        private final ListBuffer<Param> params = new ListBuffer<>();
        private JCBlock body = maker.Block(0, List.nil());

        private MethodBuilder(String methodName, Symbol.ClassSymbol owner) {
            this.methodName = methodName;
            this.owner = owner;
        }

        public MethodBuilder flags(long flags) {
            this.flags = flags;
            return this;
        }

        public MethodBuilder publicStatic() {
            return flags(Flags.PUBLIC | Flags.STATIC);
        }

        public MethodBuilder makePublic() {
            return flags(Flags.PUBLIC);
        }

        public MethodBuilder makePrivate() {
            return flags(Flags.PRIVATE);
        }

        public MethodBuilder returnsVoid() {
            this.returnType = symtab.voidType;
            this.voidReturn = true;
            return this;
        }

        public MethodBuilder returns(Type type) {
            this.returnType = type;
            this.voidReturn = type.hasTag(TypeTag.VOID);
            return this;
        }

        public MethodBuilder param(String name, Type type) {
            params.append(new Param(name, type, null));
            return this;
        }

        /** {@code type} is entered on the symbol; {@code typeTree} is what appears in the AST. */
        public MethodBuilder param(String name, Type type, JCExpression typeTree) {
            params.append(new Param(name, type, typeTree));
            return this;
        }

        public MethodBuilder body(JCStatement... statements) {
            this.body = block(statements);
            return this;
        }

        public MethodBuilder body(JCBlock body) {
            this.body = body;
            return this;
        }

        public MethodBuilder body(Iterable<? extends JCStatement> statements) {
            this.body = block(statements);
            return this;
        }

        public JCMethodDecl build() {
            Name n = name(methodName);
            ListBuffer<Type> paramTypes = new ListBuffer<>();
            ListBuffer<Symbol.VarSymbol> paramSymbols = new ListBuffer<>();
            ListBuffer<JCVariableDecl> paramDecls = new ListBuffer<>();

            for (Param param : params) {
                paramTypes.append(param.type);
                Symbol.VarSymbol symbol = new Symbol.VarSymbol(
                        Flags.PARAMETER | Flags.FINAL,
                        name(param.name),
                        param.type,
                        null);
                paramSymbols.append(symbol);

                JCExpression typeTree = param.typeTree != null ? param.typeTree : type(param.type);
                JCVariableDecl decl = maker.VarDef(
                        maker.Modifiers(Flags.PARAMETER | Flags.FINAL),
                        name(param.name),
                        typeTree,
                        null);
                decl.sym = symbol;
                paramDecls.append(decl);
            }

            Type.MethodType methodType = new Type.MethodType(
                    paramTypes.toList(),
                    returnType,
                    List.nil(),
                    owner);
            Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(flags, n, methodType, owner);
            methodSymbol.params = paramSymbols.toList();
            for (Symbol.VarSymbol paramSymbol : paramSymbols) {
                paramSymbol.owner = methodSymbol;
            }
            owner.members().enter(methodSymbol);

            JCExpression returnTree = voidReturn ? voidType() : type(returnType);
            JCMethodDecl method = maker.MethodDef(
                    maker.Modifiers(flags),
                    n,
                    returnTree,
                    List.nil(),
                    paramDecls.toList(),
                    List.nil(),
                    body,
                    null);
            method.sym = methodSymbol;
            return method;
        }

        private record Param(String name, Type type, JCExpression typeTree) {}
    }
}
