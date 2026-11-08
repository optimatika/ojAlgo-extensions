# ojAlgo-commons-math3

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-commons-math3%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-commons-math3/versions)

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
environment.addIntegration(SolverACM.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverACM.INTEGRATION);
```
