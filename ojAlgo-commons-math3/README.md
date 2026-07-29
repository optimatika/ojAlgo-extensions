# ojAlgo-commons-math3

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-commons-math3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-commons-math3/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-commons-math3</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Apache Commons Math](http://commons.apache.org/proper/commons-math/).

- Classes to convert between ojAlgo and Commons Math matrix implementations. In particular Commons Math's matrix instances can have their elements supplied directly to ojAlgo's matrix decomposition internal storage without allocating intermediate memory.
- Use Commons Math's `SimplexSolver` with ojAlgo's `ExpressionsBasedModel`.
- Drop-in replacement for Commons Math's `SimplexSolver`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverCommonsMath.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverCommonsMath.INTEGRATION);
```
