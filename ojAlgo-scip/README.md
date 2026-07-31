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

## Requirements

**Java 25 or later.** The Foreign Function & Memory API is used to call the SCIP C library directly, and the module is compiled for release 25 — unlike ojAlgo itself, which targets Java 11.

Calling native code requires the access to be enabled, otherwise the JVM warns (and, in later releases, refuses):

```
--enable-native-access=ALL-UNNAMED
```

## Accuracy of quadratic objectives

SCIP has no direct entry point for a quadratic objective, so one is passed via an auxiliary variable pinned to `x'Qx` by a quadratic constraint. The objective value that comes back is accurate, but the solution vector is only good to roughly 1E-3: at a quadratic optimum the objective is flat, so SCIP's default feasibility tolerance (`numerics/feastol`) admits that much error in x. Tightening the tolerance helps only by its square root. If you need an accurate *solution* to a QP, rather than an accurate objective value, use a dedicated QP solver.

This does not affect LP or MILP models, where SCIP is exact to its usual tolerances.

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
