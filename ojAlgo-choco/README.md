# ojAlgo-choco

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-choco%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-choco/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-choco</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Choco-solver](https://choco-solver.org). Use Choco as a solver from `ExpressionsBasedModel`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverChoco.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverChoco.INTEGRATION);
```
