# ojAlgo-clarabel4j

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-clarabel4j/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-clarabel4j/)

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
