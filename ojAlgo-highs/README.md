# ojAlgo-highs

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-highs%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-highs/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-highs</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with the [HiGHS](https://github.com/ERGO-Code/HiGHS) solver using Java's Foreign Function & Memory API (Panama FFI).

HiGHS is a high-performance solver for LP, QP, and MILP problems.

## Requirements

**Java 25 or later.** The Foreign Function & Memory API is used to call the HiGHS C library directly, and the module is compiled for release 25.

Calling native code requires the access to be enabled, otherwise the JVM warns (and, in later releases, refuses):

```
--enable-native-access=ALL-UNNAMED
```

## Native Library Installation (macOS)

```bash
brew install highs
```

To update to the latest version:

```bash
brew upgrade highs
```

The library is loaded from `$(brew --prefix)/opt/highs/lib/libhighs.dylib`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverHiGHS.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverHiGHS.INTEGRATION);
```
