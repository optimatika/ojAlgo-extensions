# ojAlgo-mosek

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-mosek%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-mosek/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-mosek</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Mosek](https://www.mosek.com). Use Mosek as a solver from `ExpressionsBasedModel`.

## Prerequisites

1. **Mosek installed and licensed.** Download from [mosek.com](https://www.mosek.com/downloads/) and place a valid license file (`mosek.lic`) in `~/mosek/`.

2. **The Mosek Java API JAR in your local Maven repository.** Mosek does not publish `mosek.jar` to Maven Central. Install it from your Mosek installation:

```bash
mvn install:install-file \
  -Dfile=/path/to/mosek/<ver>/tools/platform/<platform>/bin/mosek.jar \
  -DgroupId=mosek -DartifactId=mosek \
  -Dversion=11.0 -Dpackaging=jar
```

Adjust the file path and version to match your installation.

3. **Native library on the library path.** When you run your program, the JVM must be able to find the Mosek native library. Pass the path to the platform-specific binary directory:

```
-Djava.library.path=/path/to/mosek/<ver>/tools/platform/<platform>/bin
```

### Typical installation paths

The Mosek installation contains a `<ver>/tools/platform/<platform>/bin/` directory with both the JAR and native libraries.

| Platform | `<platform>` value |
| --- | --- |
| macOS (ARM) | `osxaarch64` |
| macOS (x86) | `osx64x86` |
| Linux (x86_64) | `linux64x86` |
| Windows (x86_64) | `win64x86` |

For example, on macOS ARM with Mosek 11.0 installed in `/opt/mosek`:

- JAR: `/opt/mosek/11.0/tools/platform/osxaarch64/bin/mosek.jar`
- Native library directory: `/opt/mosek/11.0/tools/platform/osxaarch64/bin/`

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverMosek.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverMosek.INTEGRATION);
```
