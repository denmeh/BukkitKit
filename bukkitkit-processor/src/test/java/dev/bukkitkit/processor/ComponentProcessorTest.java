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
