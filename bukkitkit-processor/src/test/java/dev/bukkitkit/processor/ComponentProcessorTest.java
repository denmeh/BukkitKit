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
                "@BukkitKit(",
                "  name = \"Hello\",",
                "  version = \"1.0.0\",",
                "  apiVersion = \"1.21\",",
                "  description = \"Demo\",",
                "  authors = {\"denmeh\"}",
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
}
