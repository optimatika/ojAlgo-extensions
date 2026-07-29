# ojAlgo-gurobi

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-gurobi/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-gurobi/)

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
