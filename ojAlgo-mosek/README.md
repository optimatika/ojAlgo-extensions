# ojAlgo-mosek

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-mosek/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-mosek/)

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
