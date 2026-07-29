# ojAlgo-ortools

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-ortools/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-ortools/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-ortools</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Google OR-Tools](https://developers.google.com/optimization). Use OR-Tools as a solver from `ExpressionsBasedModel`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverORTools.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverORTools.INTEGRATION);
```
