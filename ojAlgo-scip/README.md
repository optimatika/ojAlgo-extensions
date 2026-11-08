# ojAlgo-scip

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-scip%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-scip/versions)

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
