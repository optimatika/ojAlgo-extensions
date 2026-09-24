# ojAlgo-gurobi

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-gurobi%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-gurobi/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-gurobi</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Gurobi](https://www.gurobi.com). Use Gurobi as a solver from `ExpressionsBasedModel`.

## Prerequisites

1. **The Gurobi Java API JAR.** Gurobi publishes `com.gurobi:gurobi` to Maven Central, so this is pulled automatically when you depend on `ojalgo-gurobi`. The JAR bundles the native libraries for Linux (x86-64 and ARM64), Windows (x86-64) and macOS (universal), and loads them itself. No manual JAR or library path setup is needed.

2. **A Gurobi license.** See [gurobi.com](https://www.gurobi.com/downloads/). Activating most license types uses the `grbgetkey` tool, which comes with the full Gurobi installation. A Web License Service (WLS) license only needs the `gurobi.lic` file. Gurobi looks for `gurobi.lic` in its default locations, or wherever the `GRB_LICENSE_FILE` environment variable points.

### Native access warning

On recent JDKs, loading the native library prints a warning about restricted methods. To silence it, add this JVM argument:

```
--enable-native-access=ALL-UNNAMED
```

### Smaller dependency

The default `com.gurobi:gurobi` JAR is large (~120 MB) because it contains the native libraries for all platforms. If you already have Gurobi installed locally, you can use the `core` classifier instead. It has the same Java classes but no native libraries:

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-gurobi</artifactId>
    <version>X.Y.Z</version>
    <exclusions>
        <exclusion>
            <groupId>com.gurobi</groupId>
            <artifactId>gurobi</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.gurobi</groupId>
    <artifactId>gurobi</artifactId>
    <version>A.B.C</version> <!-- the Gurobi version ojalgo-gurobi depends on -->
    <classifier>core</classifier>
</dependency>
```

The local installation must be the same Gurobi version. If the JVM can't find the native library, point `java.library.path` at the installation's `lib` directory, for example `-Djava.library.path=/Library/gurobi1303/macos_universal2/lib`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverGurobi.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverGurobi.INTEGRATION);
```
