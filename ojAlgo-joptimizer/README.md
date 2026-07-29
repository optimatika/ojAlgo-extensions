# ojAlgo-joptimizer

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-joptimizer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-joptimizer/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-joptimizer</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [JOptimizer](http://www.joptimizer.com). Use JOptimizer as a solver from `ExpressionsBasedModel`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverJOptimizer.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverJOptimizer.INTEGRATION);
```
