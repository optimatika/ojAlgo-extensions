# ojAlgo-hipparchus

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-hipparchus/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-hipparchus/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-hipparchus</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Hipparchus](https://hipparchus.org).

- Classes to convert between ojAlgo and Hipparchus matrix implementations. In particular Hipparchus' matrix instances can have their elements supplied directly to ojAlgo's matrix decomposition internal storage without allocating intermediate memory.
- Use Hipparchus' `SimplexSolver` with ojAlgo's `ExpressionsBasedModel`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverHipparchus.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverHipparchus.INTEGRATION);
```
