# ojAlgo-cplex

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-cplex%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-cplex/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-cplex</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [IBM ILOG CPLEX](https://www.ibm.com/products/ilog-cplex-optimization-studio). Use CPLEX as a solver from `ExpressionsBasedModel`.

## Prerequisites

1. **CPLEX installed and licensed.** Download from [IBM](https://www.ibm.com/products/ilog-cplex-optimization-studio) and activate a license.

2. **The CPLEX Java API JAR in your local Maven repository.** IBM does not publish `cplex.jar` to Maven Central. Install it from your CPLEX installation:

```bash
mvn install:install-file \
  -Dfile=/path/to/cplex/lib/cplex.jar \
  -DgroupId=cplex -DartifactId=cplex \
  -Dversion=22.2.0.0 -Dpackaging=jar
```

Adjust the file path and version to match your installation.

3. **Native library on the library path.** When you run your program, the JVM must be able to find the CPLEX native library. Pass the path to the platform-specific binary directory:

```
-Djava.library.path=/path/to/cplex/bin/your-platform
```

### Typical installation paths

| Platform | CPLEX directory | `your-platform` (native lib subdirectory) |
| --- | --- | --- |
| macOS (ARM) | `/Applications/CPLEX_Studio<ver>/cplex` | `arm64_osx` |
| macOS (x86) | `/Applications/CPLEX_Studio<ver>/cplex` | `x86-64_osx` |
| Linux | `/opt/ibm/ILOG/CPLEX_Studio<ver>/cplex` | `x86-64_linux` |
| Windows | `C:\Program Files\IBM\ILOG\CPLEX_Studio<ver>\cplex` | `x64_win64` |

Replace `<ver>` with the version number, e.g. `2211` for CPLEX 22.1.1.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverCPLEX.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverCPLEX.INTEGRATION);
```
