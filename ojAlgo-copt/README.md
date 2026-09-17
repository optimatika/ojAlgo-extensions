# ojAlgo-copt

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-copt%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-copt/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-copt</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [COPT](https://www.shanshu.ai/copt) (Cardinal Optimizer). Use COPT as a solver from `ExpressionsBasedModel`.

## Prerequisites

1. **COPT installed and licensed.** Download from [shanshu.ai](https://www.shanshu.ai/copt) and activate a license.

2. **The COPT Java API JAR in your local Maven repository.** COPT does not publish `copt.jar` to Maven Central. Install it from your COPT installation:

```bash
mvn install:install-file \
  -Dfile=/path/to/copt/lib/copt.jar \
  -DgroupId=copt -DartifactId=copt \
  -Dversion=8.0.6 -Dpackaging=jar
```

Adjust the file path and version to match your installation.

3. **Native library on the library path.** When you run your program, the JVM must be able to find the COPT native library. Pass the path to the library directory:

```
-Djava.library.path=/path/to/copt/lib
```

### Typical installation paths

| Platform | Installation directory |
| --- | --- |
| macOS | `/Applications/copt<ver>` |
| Linux | `/opt/copt<ver>` |

Replace `<ver>` with the short version number, e.g. `copt80` for COPT 8.0. The JAR and native libraries are both in the `lib/` subdirectory.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverCOPT.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverCOPT.INTEGRATION);
```
