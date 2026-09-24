package dev.bukkitkit.processor.analysis;

import dev.bukkitkit.api.Command;
import dev.bukkitkit.api.Component;
import dev.bukkitkit.api.Config;
import dev.bukkitkit.api.OnDisable;
import dev.bukkitkit.api.OnEnable;
import dev.bukkitkit.api.OnEvent;
import dev.bukkitkit.api.Scheduled;
import dev.bukkitkit.api.TabComplete;
import dev.bukkitkit.api.Wire;
import dev.bukkitkit.processor.model.CommandMethod;
import dev.bukkitkit.processor.model.ComponentModel;
import dev.bukkitkit.processor.model.ComponentModel.ConfigMeta;
import dev.bukkitkit.processor.model.ComponentModel.InjectionKind;
import dev.bukkitkit.processor.model.ComponentModel.WiredField;
import dev.bukkitkit.processor.model.EventMethod;
import dev.bukkitkit.processor.model.LifecycleMethod;
import dev.bukkitkit.processor.model.ScheduledMethod;
import dev.bukkitkit.processor.model.TabCompleteMethod;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validates and extracts dependency / schedule / event / command / config metadata from a managed type.
 */
public final class ComponentAnalyzer {

    private static final String BUKKIT_EVENT = "org.bukkit.event.Event";
    private static final String BUKKIT_LISTENER = "org.bukkit.event.Listener";
    private static final String COMMAND_SENDER = "org.bukkit.command.CommandSender";
    private static final String STRING = "java.lang.String";
    private static final String LIST = "java.util.List";
    private static final String BOOLEAN = "java.lang.Boolean";
    private static final String INTEGER = "java.lang.Integer";
    private static final String LONG = "java.lang.Long";
    private static final String DOUBLE = "java.lang.Double";
    private static final String FLOAT = "java.lang.Float";

    private final Elements elements;
    private final Types types;
    private final Messager messager;

    public ComponentAnalyzer(Elements elements, Types types, Messager messager) {
        this.elements = elements;
        this.types = types;
        this.messager = messager;
    }

    public ComponentModel analyze(TypeElement type) {
        Config config = type.getAnnotation(Config.class);
        if (config != null) {
            return analyzeConfig(type, config);
        }
        return analyzeComponent(type);
    }

    private ComponentModel analyzeConfig(TypeElement type, Config config) {
        if (type.getAnnotation(Component.class) != null) {
            error(type, "Do not mix @Config and @Component on the same class");
            return null;
        }
        if (!validateManagedClassShape(type, "@Config")) {
            return null;
        }
        if (config.file().isBlank()) {
            error(type, "@Config file() must not be blank");
            return null;
        }
        if (!isSafeConfigFile(config.file())) {
            error(type, "@Config file() must be a relative path under the plugin data folder "
                    + "(no absolute paths or '..'): " + config.file());
            return null;
        }

        if (hasWireFields(type)) {
            error(type, "@Config classes must not use @Wire (inject the config into other classes instead)");
            return null;
        }
        if (hasNonConfigHooks(type)) {
            error(type, "@Config classes are data-only (no events, commands, schedules, or lifecycle hooks)");
            return null;
        }

        List<ExecutableElement> publicConstructors = ElementFilter.constructorsIn(type.getEnclosedElements())
                .stream()
                .filter(ctor -> ctor.getModifiers().contains(Modifier.PUBLIC))
                .toList();
        if (publicConstructors.size() != 1 || !publicConstructors.getFirst().getParameters().isEmpty()) {
            error(type, "@Config requires exactly one public no-arg constructor");
            return null;
        }

        boolean anyField = false;
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (!field.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (field.getModifiers().contains(Modifier.FINAL)) {
                error(field, "@Config fields must not be final (BukkitKit writes loaded values into them)");
                return null;
            }
            if (!isSupportedConfigType(field.asType())) {
                error(field, "Unsupported @Config field type (use boolean/Boolean, int/Integer, "
                        + "long/Long, double/Double, float/Float, String, or List<String>)");
                return null;
            }
            anyField = true;
        }
        if (!anyField) {
            error(type, "@Config class needs at least one public non-static non-final field");
            return null;
        }

        return new ComponentModel(
                type,
                type.getQualifiedName().toString(),
                type.getSimpleName().toString(),
                elements.getPackageOf(type).getQualifiedName().toString(),
                InjectionKind.CONSTRUCTOR,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                new ConfigMeta(config.file(), config.persistent()));
    }

    private ComponentModel analyzeComponent(TypeElement type) {
        if (!validateManagedClassShape(type, "@Component")) {
            return null;
        }

        List<WiredField> wiredFields = readWiredFields(type);
        List<ScheduledMethod> scheduledMethods = readScheduledMethods(type);
        List<EventMethod> eventMethods = readEventMethods(type);
        List<CommandMethod> commandMethods = readCommandMethods(type);
        List<TabCompleteMethod> tabCompleteMethods = readTabCompleteMethods(type);
        List<LifecycleMethod> onEnableMethods = readLifecycleMethods(type, OnEnable.class, "@OnEnable");
        List<LifecycleMethod> onDisableMethods = readLifecycleMethods(type, OnDisable.class, "@OnDisable");
        boolean alreadyListener = implementsListener(type);

        List<ExecutableElement> publicConstructors = ElementFilter.constructorsIn(type.getEnclosedElements())
                .stream()
                .filter(ctor -> ctor.getModifiers().contains(Modifier.PUBLIC))
                .toList();

        if (publicConstructors.isEmpty()) {
            error(type, "@Component requires exactly one public constructor");
            return null;
        }
        if (publicConstructors.size() > 1) {
            error(type, "@Component must have exactly one public constructor (found "
                    + publicConstructors.size() + ")");
            return null;
        }

        ExecutableElement constructor = publicConstructors.getFirst();

        if (!wiredFields.isEmpty()) {
            if (!constructor.getParameters().isEmpty()) {
                error(type, "@Wire field injection requires a public no-arg constructor "
                        + "(do not mix with constructor parameters)");
                return null;
            }
            List<String> dependencies = wiredFields.stream().map(WiredField::typeName).toList();
            return new ComponentModel(
                    type,
                    type.getQualifiedName().toString(),
                    type.getSimpleName().toString(),
                    elements.getPackageOf(type).getQualifiedName().toString(),
                    InjectionKind.FIELD,
                    List.copyOf(dependencies),
                    List.copyOf(wiredFields),
                    List.copyOf(scheduledMethods),
                    List.copyOf(eventMethods),
                    List.copyOf(commandMethods),
                    List.copyOf(tabCompleteMethods),
                    List.copyOf(onEnableMethods),
                    List.copyOf(onDisableMethods),
                    alreadyListener,
                    null);
        }

        List<String> dependencies = new ArrayList<>();
        for (VariableElement parameter : constructor.getParameters()) {
            TypeMirror paramType = parameter.asType();
            if (paramType.getKind() != TypeKind.DECLARED) {
                error(parameter, "Unsupported constructor parameter type: " + paramType);
                return null;
            }
            TypeElement paramElement = (TypeElement) ((DeclaredType) paramType).asElement();
            dependencies.add(paramElement.getQualifiedName().toString());
        }

        return new ComponentModel(
                type,
                type.getQualifiedName().toString(),
                type.getSimpleName().toString(),
                elements.getPackageOf(type).getQualifiedName().toString(),
                InjectionKind.CONSTRUCTOR,
                List.copyOf(dependencies),
                List.of(),
                List.copyOf(scheduledMethods),
                List.copyOf(eventMethods),
                List.copyOf(commandMethods),
                List.copyOf(tabCompleteMethods),
                List.copyOf(onEnableMethods),
                List.copyOf(onDisableMethods),
                alreadyListener,
                null);
    }

    private boolean validateManagedClassShape(TypeElement type, String label) {
        if (type.getKind() != ElementKind.CLASS) {
            error(type, label + " is only valid on concrete classes");
            return false;
        }
        if (type.getModifiers().contains(Modifier.ABSTRACT)) {
            error(type, label + " class must not be abstract");
            return false;
        }
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            error(type, label + " class must be public");
            return false;
        }
        if (type.getNestingKind().isNested() && !type.getModifiers().contains(Modifier.STATIC)) {
            error(type, label + " nested class must be static");
            return false;
        }
        return true;
    }

    private boolean isSupportedConfigType(TypeMirror mirror) {
        return switch (mirror.getKind()) {
            case BOOLEAN, INT, LONG, DOUBLE, FLOAT -> true;
            case DECLARED -> {
                TypeElement element = (TypeElement) ((DeclaredType) mirror).asElement();
                String name = element.getQualifiedName().toString();
                if (STRING.equals(name)
                        || BOOLEAN.equals(name)
                        || INTEGER.equals(name)
                        || LONG.equals(name)
                        || DOUBLE.equals(name)
                        || FLOAT.equals(name)) {
                    yield true;
                }
                yield isStringListType(mirror);
            }
            default -> false;
        };
    }

    private static boolean isSafeConfigFile(String file) {
        if (file.indexOf('\0') >= 0) {
            return false;
        }
        if (file.startsWith("/") || file.startsWith("\\")) {
            return false;
        }
        if (file.length() >= 2 && Character.isLetter(file.charAt(0)) && file.charAt(1) == ':') {
            return false;
        }
        String[] parts = file.split("[/\\\\]");
        for (String part : parts) {
            if (part.equals("..")) {
                return false;
            }
        }
        return !file.isBlank();
    }

    private boolean hasWireFields(TypeElement type) {
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getAnnotation(Wire.class) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNonConfigHooks(TypeElement type) {
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getAnnotation(Scheduled.class) != null
                    || method.getAnnotation(OnEvent.class) != null
                    || method.getAnnotation(Command.class) != null
                    || method.getAnnotation(TabComplete.class) != null
                    || method.getAnnotation(OnEnable.class) != null
                    || method.getAnnotation(OnDisable.class) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isStringListType(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declared = (DeclaredType) mirror;
        TypeElement list = elements.getTypeElement(LIST);
        TypeElement string = elements.getTypeElement(STRING);
        if (list == null || string == null) {
            return false;
        }
        if (!types.isSameType(types.erasure(mirror), types.erasure(list.asType()))) {
            return false;
        }
        List<? extends TypeMirror> args = declared.getTypeArguments();
        return args.size() == 1 && types.isSameType(args.getFirst(), string.asType());
    }

    private boolean implementsListener(TypeElement type) {
        TypeElement listener = elements.getTypeElement(BUKKIT_LISTENER);
        if (listener == null) {
            return false;
        }
        return types.isAssignable(type.asType(), listener.asType());
    }

    private List<WiredField> readWiredFields(TypeElement type) {
        List<WiredField> fields = new ArrayList<>();
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getAnnotation(Wire.class) == null) {
                continue;
            }
            if (field.getModifiers().contains(Modifier.STATIC)) {
                error(field, "@Wire field must not be static");
                continue;
            }
            if (field.getModifiers().contains(Modifier.FINAL)) {
                error(field, "@Wire field must not be final");
                continue;
            }
            TypeMirror fieldType = field.asType();
            if (fieldType.getKind() != TypeKind.DECLARED) {
                error(field, "Unsupported @Wire field type: " + fieldType);
                continue;
            }
            TypeElement fieldTypeElement = (TypeElement) ((DeclaredType) fieldType).asElement();
            fields.add(new WiredField(
                    field.getSimpleName().toString(),
                    fieldTypeElement.getQualifiedName().toString()));
        }
        return fields;
    }

    private List<ScheduledMethod> readScheduledMethods(TypeElement type) {
        List<ScheduledMethod> methods = new ArrayList<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            Scheduled annotation = method.getAnnotation(Scheduled.class);
            if (annotation == null) {
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, "@Scheduled method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, "@Scheduled method must be public");
                continue;
            }
            if (!method.getParameters().isEmpty()) {
                error(method, "@Scheduled method must not take parameters");
                continue;
            }
            TypeMirror returnType = method.getReturnType();
            boolean booleanReturn = returnType.getKind() == TypeKind.BOOLEAN;
            if (returnType.getKind() != TypeKind.VOID && !booleanReturn) {
                error(method, "@Scheduled method must return void or boolean");
                continue;
            }
            if (annotation.every() == 0) {
                error(method, "@Scheduled every() must be > 0 or -1 (one-shot)");
                continue;
            }
            if (annotation.every() < -1) {
                error(method, "@Scheduled every() must be > 0 or -1 (one-shot)");
                continue;
            }
            if (annotation.delay() < 0) {
                error(method, "@Scheduled delay() must be >= 0");
                continue;
            }
            methods.add(new ScheduledMethod(
                    method,
                    method.getSimpleName().toString(),
                    annotation.every(),
                    annotation.delay(),
                    annotation.unit(),
                    annotation.async(),
                    booleanReturn));
        }
        return methods;
    }

    private List<LifecycleMethod> readLifecycleMethods(
            TypeElement type,
            Class<? extends java.lang.annotation.Annotation> annotationType,
            String label) {
        List<LifecycleMethod> methods = new ArrayList<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getAnnotation(annotationType) == null) {
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, label + " method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, label + " method must be public");
                continue;
            }
            if (!method.getParameters().isEmpty()) {
                error(method, label + " method must not take parameters");
                continue;
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                error(method, label + " method must return void");
                continue;
            }
            methods.add(new LifecycleMethod(method, method.getSimpleName().toString()));
        }
        return methods;
    }

    private List<EventMethod> readEventMethods(TypeElement type) {
        List<EventMethod> methods = new ArrayList<>();
        TypeElement eventBase = elements.getTypeElement(BUKKIT_EVENT);
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            OnEvent annotation = method.getAnnotation(OnEvent.class);
            if (annotation == null) {
                continue;
            }
            if (eventBase == null) {
                error(method, "@OnEvent requires org.bukkit.event.Event on the classpath (Paper/Bukkit API)");
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, "@OnEvent method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, "@OnEvent method must be public");
                continue;
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                error(method, "@OnEvent method must return void");
                continue;
            }
            if (method.getParameters().size() != 1) {
                error(method, "@OnEvent method must take exactly one Event parameter");
                continue;
            }
            VariableElement parameter = method.getParameters().getFirst();
            TypeMirror paramType = parameter.asType();
            if (paramType.getKind() != TypeKind.DECLARED) {
                error(parameter, "@OnEvent parameter must be a Bukkit Event type");
                continue;
            }
            if (!types.isAssignable(paramType, eventBase.asType())) {
                error(parameter, "@OnEvent parameter must be a subtype of org.bukkit.event.Event");
                continue;
            }
            TypeElement eventType = (TypeElement) ((DeclaredType) paramType).asElement();
            methods.add(new EventMethod(
                    method,
                    method.getSimpleName().toString(),
                    eventType.getQualifiedName().toString(),
                    annotation.priority(),
                    annotation.ignoreCancelled()));
        }
        return methods;
    }

    private List<CommandMethod> readCommandMethods(TypeElement type) {
        List<CommandMethod> methods = new ArrayList<>();
        TypeElement senderType = elements.getTypeElement(COMMAND_SENDER);
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            Command annotation = method.getAnnotation(Command.class);
            if (annotation == null) {
                continue;
            }
            if (senderType == null) {
                error(method, "@Command requires org.bukkit.command.CommandSender on the classpath (Paper/Bukkit API)");
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, "@Command method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, "@Command method must be public");
                continue;
            }
            TypeMirror returnType = method.getReturnType();
            boolean booleanReturn = returnType.getKind() == TypeKind.BOOLEAN;
            if (returnType.getKind() != TypeKind.VOID && !booleanReturn) {
                error(method, "@Command method must return void or boolean");
                continue;
            }
            if (annotation.name().isBlank()) {
                error(method, "@Command name() must not be blank");
                continue;
            }
            CommandMethod.Signature signature = resolveCommandSignature(method, senderType);
            if (signature == null) {
                error(method, "@Command signature must be (CommandSender), "
                        + "(CommandSender, String[]), or (CommandSender, String, String[])");
                continue;
            }
            methods.add(new CommandMethod(
                    method,
                    method.getSimpleName().toString(),
                    annotation.name(),
                    annotation.description(),
                    annotation.usage(),
                    List.copyOf(Arrays.asList(annotation.aliases())),
                    annotation.permission(),
                    annotation.permissionMessage(),
                    signature,
                    booleanReturn));
        }
        return methods;
    }

    private CommandMethod.Signature resolveCommandSignature(
            ExecutableElement method,
            TypeElement senderType) {
        List<? extends VariableElement> params = method.getParameters();
        if (params.isEmpty() || params.size() > 3) {
            return null;
        }
        if (!isType(params.getFirst().asType(), senderType)) {
            return null;
        }
        if (params.size() == 1) {
            return CommandMethod.Signature.SENDER;
        }
        if (params.size() == 2) {
            return isStringArray(params.get(1).asType())
                    ? CommandMethod.Signature.SENDER_ARGS
                    : null;
        }
        if (isString(params.get(1).asType()) && isStringArray(params.get(2).asType())) {
            return CommandMethod.Signature.SENDER_LABEL_ARGS;
        }
        return null;
    }

    private List<TabCompleteMethod> readTabCompleteMethods(TypeElement type) {
        List<TabCompleteMethod> methods = new ArrayList<>();
        TypeElement senderType = elements.getTypeElement(COMMAND_SENDER);
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            TabComplete annotation = method.getAnnotation(TabComplete.class);
            if (annotation == null) {
                continue;
            }
            if (senderType == null) {
                error(method, "@TabComplete requires org.bukkit.command.CommandSender on the classpath (Paper/Bukkit API)");
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                error(method, "@TabComplete method must not be static");
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                error(method, "@TabComplete method must be public");
                continue;
            }
            if (!isListType(method.getReturnType())) {
                error(method, "@TabComplete method must return List<String>");
                continue;
            }
            if (annotation.value().isBlank()) {
                error(method, "@TabComplete value() must not be blank");
                continue;
            }
            TabCompleteMethod.Signature signature = resolveTabSignature(method, senderType);
            if (signature == null) {
                error(method, "@TabComplete signature must be (CommandSender, String[]) "
                        + "or (CommandSender, String, String[])");
                continue;
            }
            methods.add(new TabCompleteMethod(
                    method,
                    method.getSimpleName().toString(),
                    annotation.value(),
                    signature));
        }
        return methods;
    }

    private TabCompleteMethod.Signature resolveTabSignature(
            ExecutableElement method,
            TypeElement senderType) {
        List<? extends VariableElement> params = method.getParameters();
        if (params.size() < 2 || params.size() > 3) {
            return null;
        }
        if (!isType(params.getFirst().asType(), senderType)) {
            return null;
        }
        if (params.size() == 2) {
            return isStringArray(params.get(1).asType())
                    ? TabCompleteMethod.Signature.SENDER_ARGS
                    : null;
        }
        if (isString(params.get(1).asType()) && isStringArray(params.get(2).asType())) {
            return TabCompleteMethod.Signature.SENDER_ALIAS_ARGS;
        }
        return null;
    }

    private boolean isListType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement list = elements.getTypeElement(LIST);
        if (list == null) {
            return false;
        }
        TypeMirror erasure = types.erasure(type);
        return types.isSameType(erasure, types.erasure(list.asType()));
    }

    private boolean isType(TypeMirror mirror, TypeElement expected) {
        return types.isAssignable(mirror, expected.asType());
    }

    private boolean isString(TypeMirror mirror) {
        TypeElement string = elements.getTypeElement(STRING);
        return string != null && types.isSameType(mirror, string.asType());
    }

    private boolean isStringArray(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.ARRAY) {
            return false;
        }
        return isString(((javax.lang.model.type.ArrayType) mirror).getComponentType());
    }

    private void error(javax.lang.model.element.Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "BukkitKit: " + message, element);
    }
}
