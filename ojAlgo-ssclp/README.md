# ojAlgo-ssclp

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-ssclp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-ssclp/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-ssclp</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [SSC-LP](https://www.ssclab.org/). Use SSC-LP as a solver from `ExpressionsBasedModel`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverSSCLP.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverSSCLP.INTEGRATION);
```
