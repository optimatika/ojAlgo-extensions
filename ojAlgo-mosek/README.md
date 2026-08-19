# ojAlgo-mosek

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-mosek%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-mosek/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-mosek</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Mosek](https://www.mosek.com). Use Mosek as a solver from `ExpressionsBasedModel`.

## Prerequisites

* Mosek is installed and functional.
* The POM declares properties for the Mosek jar and native library paths. You need to set these to match your installation:

```xml
<properties>
    <path.installation.mosek>/path/to/mosek</path.installation.mosek>
    <path.jar.mosek>${path.installation.mosek}/tools/platform/your-platform/bin/mosek.jar</path.jar.mosek>
    <path.native.mosek>${path.installation.mosek}/tools/platform/your-platform/bin</path.native.mosek>
</properties>
```

* When you run your program, the JVM property `java.library.path` must contain the path to the Mosek native library.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverMosek.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverMosek.INTEGRATION);
```
