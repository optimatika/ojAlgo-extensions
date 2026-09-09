# ojAlgo-ortools

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-ortools%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-ortools/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-ortools</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Google OR-Tools](https://developers.google.com/optimization). Use OR-Tools as a solver from `ExpressionsBasedModel`.

The `ortools-java` Maven artifact (pulled transitively) bundles platform-specific native libraries, so no separate installation is needed.

For a lighter alternative that calls the OR-Tools C API directly via Panama FFI (requires Java 25 and a system-installed OR-Tools), see [ojAlgo-cpsat](../ojAlgo-cpsat/).

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
