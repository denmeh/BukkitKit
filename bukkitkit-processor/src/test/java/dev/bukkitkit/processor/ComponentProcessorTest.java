package dev.bukkitkit.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;

class ComponentProcessorTest {

    @Test
    void generatesBootstrapForInjectedComponents() {
        JavaFileObject repository = JavaFileObjects.forSourceLines(
                "demo.Repository",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Repository {",
                "  public Repository() {}",
                "}");

        JavaFileObject service = JavaFileObjects.forSourceLines(
                "demo.Service",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Service {",
                "  private final Repository repository;",
                "  public Service(Repository repository) {",
                "    this.repository = repository;",
                "  }",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(repository, service);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.repository = new Repository()");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.service = new Service(this.repository)");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/bukkitkit/bootstrap")
                .contentsAsUtf8String()
                .contains("demo.BukkitKitInit");
    }

    @Test
    void failsOnDependencyCycle() {
        JavaFileObject left = JavaFileObjects.forSourceLines(
                "demo.Left",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Left {",
                "  public Left(Right right) {}",
                "}");
        JavaFileObject right = JavaFileObjects.forSourceLines(
                "demo.Right",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Right {",
                "  public Right(Left left) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(left, right);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cycle");
    }

    @Test
    void failsOnMultiplePublicConstructors() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.Broken",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Broken {",
                "  public Broken() {}",
                "  public Broken(String value) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("exactly one public constructor");
    }

    @Test
    void generatesLifecycleAndScheduledCalls() {
        JavaFileObject service = JavaFileObjects.forSourceLines(
                "demo.Ticker",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.OnDisable;",
                "import dev.bukkitkit.api.OnEnable;",
                "import dev.bukkitkit.api.ScheduleUnit;",
                "import dev.bukkitkit.api.Scheduled;",
                "@Component",
                "public class Ticker {",
                "  public Ticker() {}",
                "  @OnEnable public void start() {}",
                "  @OnDisable public void stop() {}",
                "  @Scheduled(every = 5, unit = ScheduleUnit.SECONDS)",
                "  public boolean tick() { return true; }",
                "  @Scheduled(every = -1, delay = 1, unit = ScheduleUnit.TICKS)",
                "  public void once() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(service);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.ticker.start()");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.ticker.stop()");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("kitScheduler().runRepeating");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("kitScheduler().runLater");
    }

    @Test
    void failsOnInvalidScheduledReturnType() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.BadSchedule",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.Scheduled;",
                "@Component",
                "public class BadSchedule {",
                "  public BadSchedule() {}",
                "  @Scheduled(every = 1)",
                "  public int tick() { return 0; }",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("void or boolean");
    }

    @Test
    void failsOnNonPublicScheduledMethod() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.HiddenSchedule",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.Scheduled;",
                "@Component",
                "public class HiddenSchedule {",
                "  public HiddenSchedule() {}",
                "  @Scheduled(every = 1)",
                "  void tick() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must be public");
    }

    @Test
    void generatesEventRegistration() {
        JavaFileObject joinEvent = JavaFileObjects.forSourceLines(
                "demo.JoinEvent",
                "package demo;",
                "import org.bukkit.event.Event;",
                "public class JoinEvent extends Event {",
                "}");
        JavaFileObject listener = JavaFileObjects.forSourceLines(
                "demo.JoinListener",
                "package demo;",
                "import dev.bukkitkit.api.EventPriority;",
                "import dev.bukkitkit.api.OnEvent;",
                "public class JoinListener {",
                "  public JoinListener() {}",
                "  @OnEvent(priority = EventPriority.HIGH, ignoreCancelled = true)",
                "  public void onJoin(JoinEvent event) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(joinEvent, listener);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("pluginManager().registerEvent");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("EventPriority.HIGH");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("HandlerList.unregisterAll");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("joinListenerListener");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.joinListener = new JoinListener()");
    }

    @Test
    void componentWithOnEventIsBoundOnce() {
        JavaFileObject joinEvent = JavaFileObjects.forSourceLines(
                "demo.JoinEvent",
                "package demo;",
                "import org.bukkit.event.Event;",
                "public class JoinEvent extends Event {",
                "}");
        JavaFileObject both = JavaFileObjects.forSourceLines(
                "demo.Both",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.OnEvent;",
                "@Component",
                "public class Both {",
                "  public Both() {}",
                "  @OnEvent",
                "  public void onJoin(JoinEvent event) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(joinEvent, both);

        assertThat(compilation).succeeded();
        var initFile = compilation.generatedSourceFile("demo.BukkitKitInit").orElseThrow();
        String init;
        try {
            init = initFile.getCharContent(true).toString();
        } catch (java.io.IOException ex) {
            throw new AssertionError(ex);
        }
        int creates = init.split("new Both\\(", -1).length - 1;
        org.junit.jupiter.api.Assertions.assertEquals(1, creates);
    }

    @Test
    void failsOnInvalidOnEventSignature() {
        JavaFileObject badEvent = JavaFileObjects.forSourceLines(
                "demo.NotAnEvent",
                "package demo;",
                "public class NotAnEvent {",
                "}");
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.BadListener",
                "package demo;",
                "import dev.bukkitkit.api.OnEvent;",
                "public class BadListener {",
                "  public BadListener() {}",
                "  @OnEvent",
                "  public void onThing(NotAnEvent event) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(badEvent, source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("subtype of org.bukkit.event.Event");
    }

    @Test
    void generatesPluginYmlFromBukkitKitMarker() {
        JavaFileObject marker = JavaFileObjects.forSourceLines(
                "demo.HelloPlugin",
                "package demo;",
                "import dev.bukkitkit.api.BukkitKit;",
                "import dev.bukkitkit.api.Permission;",
                "import dev.bukkitkit.api.PermissionDefault;",
                "import dev.bukkitkit.api.PluginLoad;",
                "@BukkitKit(",
                "  name = \"Hello\",",
                "  version = \"1.0.0\",",
                "  apiVersion = \"1.21\",",
                "  description = \"Demo\",",
                "  authors = {\"denmeh\"},",
                "  website = \"https://example.com\",",
                "  prefix = \"Hello\",",
                "  load = PluginLoad.STARTUP,",
                "  depend = {\"Vault\"},",
                "  softDepend = {\"WorldGuard\"},",
                "  loadBefore = {\"OtherPlugin\"},",
                "  provides = {\"HelloApi\"},",
                "  permissions = {",
                "    @Permission(name = \"hello.use\", description = \"Use /hello\",",
                "        defaultValue = PermissionDefault.TRUE, children = {\"hello.admin\"})",
                "  }",
                ")",
                "public final class HelloPlugin {",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(marker);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.HelloPlugin_BukkitKit")
                .contentsAsUtf8String()
                .contains("extends JavaPlugin");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("name: Hello");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("main: demo.HelloPlugin_BukkitKit");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("api-version: \"1.21\"");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("website: \"https://example.com\"");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("load: STARTUP");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("depend: [Vault]");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("softdepend: [WorldGuard]");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("permissions:");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("hello.use:");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("default: true");
    }

    @Test
    void generatesCommandRegistrationAndPluginYml() {
        JavaFileObject marker = JavaFileObjects.forSourceLines(
                "demo.HelloPlugin",
                "package demo;",
                "import dev.bukkitkit.api.BukkitKit;",
                "@BukkitKit(name = \"Hello\", version = \"1\", apiVersion = \"1.21\")",
                "public final class HelloPlugin {",
                "}");
        JavaFileObject commands = JavaFileObjects.forSourceLines(
                "demo.GreetCommands",
                "package demo;",
                "import dev.bukkitkit.api.Command;",
                "import dev.bukkitkit.api.TabComplete;",
                "import org.bukkit.command.CommandSender;",
                "import java.util.List;",
                "public class GreetCommands {",
                "  public GreetCommands() {}",
                "  @Command(name = \"greet\", description = \"Greet\", usage = \"/greet\",",
                "      aliases = {\"hi\"}, permission = \"hello.greet\")",
                "  public void greet(CommandSender sender, String[] args) {}",
                "  @TabComplete(\"greet\")",
                "  public List<String> greetTab(CommandSender sender, String[] args) {",
                "    return List.of();",
                "  }",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(marker, commands);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("getCommand(\"greet\")");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("setExecutor");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("setTabCompleter");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("commands:");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("greet:");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "plugin.yml")
                .contentsAsUtf8String()
                .contains("aliases: [hi]");
    }

    @Test
    void failsOnDuplicateCommandNames() {
        JavaFileObject first = JavaFileObjects.forSourceLines(
                "demo.A",
                "package demo;",
                "import dev.bukkitkit.api.Command;",
                "import org.bukkit.command.CommandSender;",
                "public class A {",
                "  public A() {}",
                "  @Command(name = \"ping\")",
                "  public void ping(CommandSender sender) {}",
                "}");
        JavaFileObject second = JavaFileObjects.forSourceLines(
                "demo.B",
                "package demo;",
                "import dev.bukkitkit.api.Command;",
                "import org.bukkit.command.CommandSender;",
                "public class B {",
                "  public B() {}",
                "  @Command(name = \"ping\")",
                "  public void ping(CommandSender sender) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(first, second);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @Command name");
    }

    @Test
    void failsOnTabCompleteWithoutCommand() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.OrphanTab",
                "package demo;",
                "import dev.bukkitkit.api.TabComplete;",
                "import org.bukkit.command.CommandSender;",
                "import java.util.List;",
                "public class OrphanTab {",
                "  public OrphanTab() {}",
                "  @TabComplete(\"missing\")",
                "  public List<String> tab(CommandSender sender, String[] args) {",
                "    return List.of();",
                "  }",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("unknown @Command name");
    }

    @Test
    void failsOnInvalidCommandSignature() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.BadCommand",
                "package demo;",
                "import dev.bukkitkit.api.Command;",
                "public class BadCommand {",
                "  public BadCommand() {}",
                "  @Command(name = \"bad\")",
                "  public void bad(String only) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Command signature");
    }

    @Test
    void failsWhenBukkitKitExtendsJavaPlugin() {
        JavaFileObject marker = JavaFileObjects.forSourceLines(
                "demo.BadPlugin",
                "package demo;",
                "import dev.bukkitkit.api.BukkitKit;",
                "import org.bukkit.plugin.java.JavaPlugin;",
                "@BukkitKit(name = \"Bad\", version = \"1\", apiVersion = \"1.21\")",
                "public final class BadPlugin extends JavaPlugin {",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(marker);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must not extend JavaPlugin");
    }

    @Test
    void failsWhenWiringBukkitKitMarker() {
        JavaFileObject marker = JavaFileObjects.forSourceLines(
                "demo.HelloPlugin",
                "package demo;",
                "import dev.bukkitkit.api.BukkitKit;",
                "@BukkitKit(name = \"Hello\", version = \"1\", apiVersion = \"1.21\")",
                "public final class HelloPlugin {",
                "}");
        JavaFileObject service = JavaFileObjects.forSourceLines(
                "demo.Service",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.Wire;",
                "@Component",
                "public class Service {",
                "  @Wire private HelloPlugin plugin;",
                "  public Service() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(marker, service);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Cannot @Wire @BukkitKit marker");
    }

    @Test
    void failsOnUnresolvedLocalDependency() {
        JavaFileObject missing = JavaFileObjects.forSourceLines(
                "demo.NotAComponent",
                "package demo;",
                "public class NotAComponent {",
                "  public NotAComponent() {}",
                "}");
        JavaFileObject service = JavaFileObjects.forSourceLines(
                "demo.Service",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Service {",
                "  public Service(NotAComponent dependency) {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(missing, service);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Unresolved dependency");
    }

    @Test
    void generatesFieldWireCalls() {
        JavaFileObject repository = JavaFileObjects.forSourceLines(
                "demo.Repository",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "@Component",
                "public class Repository {",
                "  public Repository() {}",
                "}");
        JavaFileObject service = JavaFileObjects.forSourceLines(
                "demo.Service",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.Wire;",
                "@Component",
                "public class Service {",
                "  @Wire private Repository repository;",
                "  public Service() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(repository, service);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("FieldWire.set(this.service, \"repository\", this.repository)");
    }

    @Test
    void generatesConfigBindCall() {
        JavaFileObject config = JavaFileObjects.forSourceLines(
                "demo.DemoConfig",
                "package demo;",
                "import dev.bukkitkit.api.Config;",
                "@Config",
                "public class DemoConfig {",
                "  public String welcomeMessage = \"Hi\";",
                "  public DemoConfig() {}",
                "}");
        JavaFileObject service = JavaFileObjects.forSourceLines(
                "demo.Greeter",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.Wire;",
                "@Component",
                "public class Greeter {",
                "  @Wire private DemoConfig config;",
                "  public Greeter() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(config, service);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.demoConfig = new DemoConfig()");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("KitConfig.bind(s.javaPlugin(), this.demoConfig, \"config.yml\")");
    }

    @Test
    void skipsBindWhenConfigNotPersistent() {
        JavaFileObject config = JavaFileObjects.forSourceLines(
                "demo.MemoryConfig",
                "package demo;",
                "import dev.bukkitkit.api.Config;",
                "@Config(persistent = false)",
                "public class MemoryConfig {",
                "  public int value = 1;",
                "  public MemoryConfig() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(config);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("this.memoryConfig = new MemoryConfig()");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .doesNotContain("KitConfig.bind");
    }

    @Test
    void failsOnDuplicateConfigFiles() {
        JavaFileObject first = JavaFileObjects.forSourceLines(
                "demo.One",
                "package demo;",
                "import dev.bukkitkit.api.Config;",
                "@Config(file = \"shared.yml\")",
                "public class One {",
                "  public int a = 1;",
                "  public One() {}",
                "}");
        JavaFileObject second = JavaFileObjects.forSourceLines(
                "demo.Two",
                "package demo;",
                "import dev.bukkitkit.api.Config;",
                "@Config(file = \"shared.yml\")",
                "public class Two {",
                "  public int b = 2;",
                "  public Two() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(first, second);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @Config file");
    }

    @Test
    void failsWhenConfigMixedWithComponent() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.Broken",
                "package demo;",
                "import dev.bukkitkit.api.Component;",
                "import dev.bukkitkit.api.Config;",
                "@Config",
                "@Component",
                "public class Broken {",
                "  public int value = 1;",
                "  public Broken() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Do not mix @Config and @Component");
    }

    @Test
    void failsOnUnsupportedConfigFieldType() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.Broken",
                "package demo;",
                "import dev.bukkitkit.api.Config;",
                "import java.util.Map;",
                "@Config",
                "public class Broken {",
                "  public Map<String, String> values;",
                "  public Broken() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Unsupported @Config field type");
    }

    @Test
    void failsOnUnsafeConfigFilePath() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "demo.Broken",
                "package demo;",
                "import dev.bukkitkit.api.Config;",
                "@Config(file = \"../escape.yml\")",
                "public class Broken {",
                "  public int value = 1;",
                "  public Broken() {}",
                "}");

        Compilation compilation = Compiler.javac()
                .withProcessors(new ComponentProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("relative path");
    }
}
