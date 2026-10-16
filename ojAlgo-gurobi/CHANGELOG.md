# Changelog

All notable changes to ojAlgo-gurobi will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

## [3.3.0] – 2026-10-16

Built with ojAlgo 57.5.0 and Gurobi 13.0.3.

### Changed

- Dual values are Gurobi's own (`Pi` and `QCPi`) and follow ojAlgo 57.5.0's convention: shadow prices in the model's optimisation sense.

## [3.2.0] – 2026-10-09

Built with ojAlgo 57.4.0 and Gurobi 13.0.3.

### Added

- Dual values and reduced gradient with optimal solutions of continuous models, including ones with quadratic constraints.
- `SolverGurobi` implements `UpdatableSolver`: variable bounds can be changed (`updateRange`, `fixVariable`) and the model solved again.
- A feasible solution passed to `solve(...)` is used as Gurobi's MIP start.
- Time and iteration limits: Gurobi stops at the `time_suffice`/`iterations_suffice` limits and, unless it then has a feasible solution, continues up to the `time_abort`/`iterations_abort` limits. A MIP stopped at a limit with an incumbent is `FEASIBLE`.
- The parallelism setting (`Options.getParallelism()`) is passed to Gurobi as its number of threads, and a configured (non-default) MIP gap and feasibility tolerance are passed on as well.
- Interrupting the solving thread terminates Gurobi.
- `Configurator.configure(GRBModel, Optimisation.Options)` for per-model settings – `configure(GRBEnv, Optimisation.Options)` is for the cached environment shared by all models.
- When Gurobi can only tell that a model is infeasible or unbounded, it is solved again to find out which, and an `UNBOUNDED` result comes with a feasible solution.

### Changed

- The POM no longer declares properties for the paths of a local Gurobi installation. The `com.gurobi:gurobi` JAR bundles the native libraries.
- Gurobi gets the model as it is – all variables, including fixed ones, unscaled parameters and the objective's constant – and its own presolve and scaling do the rest.
- The configurator is looked up with `ExpressionsBasedModel.getConfigurator(...)`, so register a `SolverGurobi.Configurator` with the model or its `Optimisation.Environment` (ojAlgo 57).

## [3.1.0] – 2025-01-21

Built with ojAlgo 55.1.1 and Gurobi 12.0.0. The published POM is missing the ojAlgo dependency – declare it yourself.

### Added

- `SolverGurobi.newConfigurableIntegration()`: an integration whose Gurobi environments are created empty, to be configured with `Configurator.configure(GRBEnv, Optimisation.Options)` before they are started.

### Changed

- Gurobi is now a regular dependency, `com.gurobi:gurobi` from Maven Central, instead of a system-scope JAR from a local installation. Its Java API is in the package `com.gurobi.gurobi` (was `gurobi`).
- Requires Java 11 (was Java 8).
- `SolverGurobi.Configurator` is now a class, with `configure(GRBEnv, Optimisation.Options)` and `newGRBModel(GRBEnv)` to override. It replaces the functional interface method `configure(GRBEnv, GRBModel, Optimisation.Options)`.
- `SolverGurobi.Integration` is no longer public, and `INTEGRATION` is declared as `ExpressionsBasedModel.Integration<SolverGurobi>`. The Gurobi environment is created when first needed, and disposed of after an hour without use.

### Removed

- `SolverGurobi.newInstantCloudIntegration(String, String)` – `newConfigurableIntegration()` lets a configurator set the environment's parameters instead.

## [3.0.1] – 2022-07-06

Built with ojAlgo 51.4.0 and Gurobi 9.1.1.

### Added

- `SolverGurobi.newInstantCloudIntegration(String, String)`: an integration that uses Gurobi Instant Cloud, given an access key and a secret.

### Changed

- `SolverGurobi.Integration` is public and `AutoCloseable` – closing it disposes of its Gurobi environment.
- Gurobi exceptions when creating the environment or solving are rethrown, wrapped in a `RuntimeException`, rather than printed and ignored.
- The published POM declares the system-scope Gurobi dependency with a fixed path to the JAR, instead of the overridable `path.jar.gurobi` property.

## [3.0.0] – 2021-03-24

Built with ojAlgo 48.4.1 and Gurobi 9.1.1.

### Changed

- The module is versioned independently. The 2.47.x versions followed the ojAlgo major version.
- Built against Gurobi 9.1.1 (was 7.5.1), still a system-scope dependency on a local Gurobi installation.

## [2.47.2] – 2018-12-19

Built with ojAlgo 47.0.0 and Gurobi 7.5.1.

Initial release. `SolverGurobi.INTEGRATION` lets `ExpressionsBasedModel` solve LP, QP, QCQP and mixed-integer models with Gurobi, and a `SolverGurobi.Configurator` registered with the model's options can set Gurobi parameters. Gurobi's JAR (a system-scope dependency) and native libraries come from a local Gurobi installation.
