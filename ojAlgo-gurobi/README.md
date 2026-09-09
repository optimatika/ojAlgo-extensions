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

1. **Gurobi installed and licensed.** Download from [gurobi.com](https://www.gurobi.com/downloads/) and activate a license.

2. **The Gurobi Java API JAR.** Gurobi publishes `com.gurobi:gurobi` to Maven Central, so this is pulled automatically when you depend on `ojalgo-gurobi` — no manual JAR setup needed.

3. **Native library on the library path.** When you run your program, the JVM must be able to find the Gurobi native library. Pass the path to the directory containing it:

```
-Djava.library.path=/path/to/gurobi/lib
```

### Typical installation paths

| Platform | Installation directory | Native library directory |
| --- | --- | --- |
| macOS | `/Library/gurobi<ver>/macos_universal2` | `lib/` |
| Linux | `/opt/gurobi<ver>/linux64` | `lib/` |
| Windows | `C:\gurobi<ver>\` | `lib\` |

Replace `<ver>` with the version number, e.g. `gurobi1200` for Gurobi 12.0.0.

If the `GUROBI_HOME` environment variable is set (the Gurobi installer typically sets it), the native library directory is `$GUROBI_HOME/lib`.

### Maven Surefire

To run tests via Maven, configure the surefire plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Djava.library.path=${env.GUROBI_HOME}/lib</argLine>
        <environmentVariables>
            <LD_LIBRARY_PATH>${env.GUROBI_HOME}/lib</LD_LIBRARY_PATH>
        </environmentVariables>
    </configuration>
</plugin>
```

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
