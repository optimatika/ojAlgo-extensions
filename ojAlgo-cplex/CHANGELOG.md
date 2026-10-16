# Changelog

All notable changes to ojAlgo-cplex will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- An `INFEASIBLE` result of a continuous model without quadratic constraints carries CPLEX's Farkas proof as an infeasibility certificate – dual values together with their reduced gradient (see ojAlgo 57.5.0). There is one only when a simplex optimiser, not presolve, proved the infeasibility.

### Changed

- Dual values are CPLEX's own and follow ojAlgo 57.5.0's convention: shadow prices in the model's optimisation sense.

## [3.2.0] – 2026-10-09

Built with ojAlgo 57.4.0 and CPLEX 22.2.0.0.

### Added

- Dual values and reduced gradient with optimal solutions of continuous models without quadratic constraints (LP and QP).
- `SolverCPLEX` implements `UpdatableSolver`: variable bounds can be changed (`updateRange`, `fixVariable`) and the model solved again.
- Time and iteration limits: CPLEX stops at the `time_suffice`/`iterations_suffice` limits and, unless it then has a feasible solution, continues up to the `time_abort`/`iterations_abort` limits.
- The parallelism setting (`Options.getParallelism()`) is passed to CPLEX as its number of threads, and a configured (non-default) MIP gap and feasibility tolerance are passed on as well.
- Interrupting the solving thread aborts CPLEX.
- With `options.logger_appender` set, CPLEX also logs simplex and barrier progress, in more detail with `options.logger_detailed`.

### Changed

- The `cplex:cplex` dependency is `provided` (CPLEX 22.2.0.0) instead of a system-scope path: install `cplex.jar` in your local Maven repository, and put CPLEX's native library on `java.library.path`.
- A constraint with both a lower and an upper limit is passed to CPLEX as one range constraint (was two constraints).
- CPLEX's optimality tolerance (`EpOpt`) is set to 5e-9.
- The configurator is looked up with `ExpressionsBasedModel.getConfigurator(...)`, so register a `SolverCPLEX.Configurator` with the model or its `Optimisation.Environment` (ojAlgo 57).

## [3.1.0] – 2025-01-19

Built with ojAlgo 55.1.1 and CPLEX 22.1.1. The published POM is missing the ojAlgo dependency – declare it yourself.

### Changed

- Built against CPLEX 22.1.1 (was 12.8), still a system-scope dependency with a fixed path to the JAR.
- Requires Java 11 (was Java 8).
- `SolverCPLEX.Configurator` is now a class: override `newIloCplex(Optimisation.Options)` to create and configure the `IloCplex` instance. It replaces the functional interface method `configure(IloCplex, Optimisation.Options)`.
- CPLEX gets the model as it is: all variables, including fixed ones, and unscaled parameters.
- A CPLEX exception while building the model is rethrown, wrapped in a `RuntimeException`, rather than printed and ignored.

## [3.0.3] – 2022-07-06

Built with ojAlgo 51.4.0 and CPLEX 12.8.

### Added

- CPLEX's log is printed to `options.logger_appender`, when set (otherwise it is suppressed).

### Changed

- Variables are passed to CPLEX as integer only if the model has integer variables, so a relaxed copy of a MIP model is solved as a continuous model.
- The published POM declares the system-scope CPLEX dependency with a fixed path to the JAR, instead of the overridable `path.jar.cplex` property.

## [3.0.2] – 2021-10-28

Built with ojAlgo 49.2.1 and CPLEX 12.8.

### Fixed

- The POM depends on the released ojAlgo 49.2.1, rather than 49.2.1-SNAPSHOT.

## [3.0.1] – 2021-10-27

Built with ojAlgo 49.2.1-SNAPSHOT and CPLEX 12.8. The published POM depends on that SNAPSHOT version, which is not available from Maven Central – use 3.0.2 instead.

## [3.0.0] – 2021-03-24

Built with ojAlgo 48.4.1 and CPLEX 12.8.

### Changed

- The module is versioned independently. The 2.47.x versions followed the ojAlgo major version.
- Built against CPLEX 12.8 (was 12.6.2), still a system-scope dependency on a local CPLEX installation.

## [2.47.2] – 2018-12-19

Built with ojAlgo 47.0.0 and CPLEX 12.6.2.

Initial release. `SolverCPLEX.INTEGRATION` lets `ExpressionsBasedModel` solve LP, QP, QCQP and mixed-integer models with CPLEX, using its Concert Technology Java API, and a `SolverCPLEX.Configurator` registered with the model's options can set CPLEX parameters. CPLEX's JAR (a system-scope dependency) and native libraries come from a local CPLEX installation.
