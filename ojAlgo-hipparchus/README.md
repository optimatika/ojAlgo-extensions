# ojAlgo-hipparchus

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-hipparchus%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-hipparchus/versions)

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
