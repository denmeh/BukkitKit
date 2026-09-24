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
                .contains("__bukkitKit_bind");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("__bukkitKit_instance");
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
                "import dev.bukkitkit.api.Lifecycle;",
                "import dev.bukkitkit.api.ScheduleUnit;",
                "import dev.bukkitkit.api.Scheduled;",
                "@Component",
                "public class Ticker implements Lifecycle {",
                "  public Ticker() {}",
                "  @Override public void onEnable() {}",
                "  @Override public void onDisable() {}",
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
                .contains("Lifecycles.enable");
        assertThat(compilation)
                .generatedSourceFile("demo.BukkitKitInit")
                .contentsAsUtf8String()
                .contains("Lifecycles.disable");
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
}
