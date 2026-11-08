# ojAlgo-mosek

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-mosek%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-mosek/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-mosek</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Mosek](https://www.mosek.com). Use Mosek as a solver from `ExpressionsBasedModel`.

## Prerequisites

1. **The Mosek Java API JAR.** Mosek publishes `com.mosek:mosek` to Maven Central, so this is pulled automatically when you depend on `ojalgo-mosek`. The JAR bundles the native libraries for Linux (x86-64 and ARM64), Windows (x86-64) and macOS (ARM64), and loads them itself. No manual JAR or library path setup is needed.

2. **A Mosek license.** See [mosek.com](https://www.mosek.com/license/request/). Mosek looks for `mosek.lic` in `~/mosek/`, or wherever the `MOSEKLM_LICENSE_FILE` environment variable points.

### Native access warning

On recent JDKs, loading the native library prints a warning about restricted methods. To silence it, add this JVM argument:

```
--enable-native-access=ALL-UNNAMED
```

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
