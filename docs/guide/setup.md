# Setup

This page gets BukkitKit into a Paper plugin project. After this, the tutorial pages assume Maven is configured.

## Requirements

- A Paper plugin project (Maven)
- A JDK that can compile your plugin (BukkitKit itself is built with modern Java; match your target Paper version’s needs)
- Paper API on the classpath as `provided` (same as any Paper plugin)

## 1. Add the dependency

```xml
<dependency>
  <groupId>com.github.denmeh</groupId>
  <artifactId>bukkitkit-paper</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

`bukkitkit-paper` is the artifact you depend on for Paper plugins. It brings in the public API and the runtime.

## 2. Enable the annotation processor

BukkitKit generates code while `javac` runs. Point the compiler at the same artifact:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>com.github.denmeh</groupId>
        <artifactId>bukkitkit-paper</artifactId>
        <version>1.0-SNAPSHOT</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Without this step, annotations are ignored and nothing will be wired at runtime.

## 3. Shade BukkitKit into your plugin JAR

Players install one JAR. Shade the BukkitKit runtime modules into your plugin and merge the bootstrap index:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.6.0</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
      <configuration>
        <createDependencyReducedPom>false</createDependencyReducedPom>
        <artifactSet>
          <includes>
            <include>com.github.denmeh:bukkitkit-api</include>
            <include>com.github.denmeh:bukkitkit-core</include>
            <include>com.github.denmeh:bukkitkit-paper</include>
          </includes>
        </artifactSet>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
            <resource>META-INF/bukkitkit/bootstrap</resource>
          </transformer>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>
```

The `AppendingTransformer` line matters: without it, the server may not find BukkitKit’s generated bootstrap.

## 4. (Optional) Try the demo in this repo

If you cloned BukkitKit itself:

```bash
just demo
```

That packages `bukkitkit-demo` into a JAR you can drop into a Paper `plugins/` folder.

## Next

Continue to [Your first plugin](./your-first-plugin) — a minimal `@BukkitKit` class.
