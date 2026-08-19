# ojAlgo-cplex

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-cplex%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-cplex/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-cplex</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [IBM ILOG CPLEX](https://www.ibm.com/products/ilog-cplex-optimization-studio). Use CPLEX as a solver from `ExpressionsBasedModel`.

## Prerequisites

* CPLEX is installed and functional.
* The POM declares properties for the CPLEX jar and native library paths. You need to set these to match your installation:

```xml
<properties>
    <path.installation.cplex>/path/to/cplex</path.installation.cplex>
    <path.jar.cplex>${path.installation.cplex}/lib/cplex.jar</path.jar.cplex>
    <path.native.cplex>${path.installation.cplex}/bin/your-platform</path.native.cplex>
</properties>
```

* When you run your program, the JVM property `java.library.path` must contain the path to the CPLEX native library.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverCPLEX.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverCPLEX.INTEGRATION);
```
