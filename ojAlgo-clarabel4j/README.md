# ojAlgo-clarabel4j

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-clarabel4j%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-clarabel4j/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-clarabel4j</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Clarabel](https://github.com/oxfordcontrol/Clarabel.rs) via [clarabel4j](https://github.com/ustermetrics/clarabel4j). Clarabel is an interior-point solver for convex conic programs supporting LP, QP, and QCQP.

This module uses the clarabel4j Java wrapper which bundles the native library. For a Panama FFI based alternative (no bundled native library), see [ojAlgo-clarabel](../ojAlgo-clarabel/).

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverClarabel4j.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverClarabel4j.INTEGRATION);
```
