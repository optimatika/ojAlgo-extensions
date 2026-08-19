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

* Gurobi is installed and functional.
* When you run your program, the JVM property `java.library.path` must contain the path to the Gurobi native library.

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
