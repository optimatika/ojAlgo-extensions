# ojAlgo-hipparchus

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-hipparchus%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-hipparchus/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-hipparchus</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Hipparchus](https://hipparchus.org).

- Classes to convert between ojAlgo and Hipparchus matrix implementations. In particular Hipparchus' matrix instances can have their elements supplied directly to ojAlgo's matrix decomposition internal storage without allocating intermediate memory.
- Use Hipparchus' optimisers with ojAlgo's `ExpressionsBasedModel`. `SolverHipparchus.INTEGRATION` solves LP with `SimplexSolver`, convex QP with `QPDualActiveSolver`, and models with convex quadratic constraints (QCQP) with `SQPOptimizerS2`. Each optimiser also has its own integration: `SimplexSolverImpl`, `QPDualActiveSolverImpl`, `ADMMQPOptimizerImpl` and `SQPOptimizerImplS2` (and the deprecated `SQPOptimizerImplGM` and `SQPOptimizerImplS`).

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

## Model size and time limits

Hipparchus' QP optimisers work with dense matrices, and each variable bound is a (dense) constraint row. `QPDualActiveSolver` is fast for small models – with the Maros-Meszaros test set comparable to ojAlgo's own solver up to about 600 variables. Beyond that it is much slower, and none of the models with 10 000 variables were solved within 5 minutes.

The time limits, `Options.time_abort` and `Options.time_suffice`, are ignored – Hipparchus' optimisers have no time limits, and do not react to the thread being interrupted. `Options.iterations_abort` is used by `ADMMQPOptimizerImpl` and `SQPOptimizerImplS2` only (not for LP, and not for QP with `SolverHipparchus`).
