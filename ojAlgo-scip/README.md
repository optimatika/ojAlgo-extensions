# ojAlgo-scip

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-scip/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-scip/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-scip</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with the [SCIP](https://github.com/scipopt/scip) solver using Java's Foreign Function & Memory API (Panama FFI).

SCIP is a solver for LP, QP, MILP, MIQP, QCQP, and MIQCQP problems.

## Native Library Installation (macOS)

```bash
brew install scip
```

To update to the latest version:

```bash
brew upgrade scip
```

The library is loaded from `$(brew --prefix)/opt/scip/lib/libscip.dylib`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverSCIP.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverSCIP.INTEGRATION);
```
