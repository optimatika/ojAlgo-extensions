# ojAlgo-cplex

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-cplex/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-cplex/)

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
