# BukkitKit just recipes
# Requires a full JDK 25 (javac). Override with: JAVA_HOME=/path/to/jdk just demo

java_home := env("JAVA_HOME", "/home/denmeh/.jdks/ms-25.0.4.1-1")
mvnw := "./mvnw"

export JAVA_HOME := java_home
export PATH := java_home / "bin:" + env("PATH")

default:
    @just --list

# Build the shaded demo plugin JAR (and dependencies)
demo:
    {{mvnw}} -pl bukkitkit-demo -am package
    @echo "→ bukkitkit-demo/target/bukkitkit-demo-1.0-SNAPSHOT.jar"

# Full reactor verify
verify:
    {{mvnw}} clean verify

# Clean all modules
clean:
    {{mvnw}} clean
