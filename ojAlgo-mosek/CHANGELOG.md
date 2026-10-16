# Changelog

All notable changes to ojAlgo-mosek will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

## [4.0.0] – 2026-10-16

Built with ojAlgo 57.5.0 and MOSEK 11.2.6.

### Added

- An `INFEASIBLE` result of a continuous model carries MOSEK's certificate of primal infeasibility as an infeasibility certificate – dual values together with their reduced gradient (see ojAlgo 57.5.0).

### Changed

- MOSEK is now a regular dependency, `com.mosek:mosek` from Maven Central, instead of a `provided` `mosek:mosek` JAR you install yourself. The MOSEK JAR bundles and loads the native libraries for Linux (x86-64 and ARM64), Windows (x86-64) and macOS (ARM64), so no manual JAR installation or `java.library.path` setup is needed.
- Breaking: in that artifact MOSEK's Java API is in the package `com.mosek.mosek` (was `mosek`). Custom `SolverMosek.Configurator` subclasses, which work with MOSEK's `Env` and `Task`, need their imports updated.
- Dual values are MOSEK's own and follow ojAlgo 57.5.0's convention: shadow prices in the model's optimisation sense.

## [3.3.0] – 2026-10-09

Built with ojAlgo 57.4.0 and MOSEK 11.2.4.

### Added

- Dual values and reduced gradient with optimal solutions of continuous models (LP, QP and QCQP).
- Time and iteration limits: MOSEK stops at the `time_suffice`/`iterations_suffice` limits and, unless it then has a feasible solution, continues up to the `time_abort`/`iterations_abort` limits.
- The parallelism setting (`Options.getParallelism()`) is passed to MOSEK as its number of threads, and a configured (non-default) MIP gap and feasibility tolerance are passed on as well.
- Interrupting the solving thread stops MOSEK.
- A solve stopped at a limit returns `FEASIBLE` when MOSEK has a feasible solution, and `APPROXIMATE` otherwise, rather than `FAILED`.
- A model MOSEK finds unbounded, or cannot tell infeasible from unbounded, is solved again with a zero objective, so an `UNBOUNDED` result comes with a feasible solution (or the model is found infeasible).

### Changed

- The `mosek:mosek` dependency is `provided` (MOSEK 11.2.4) instead of a system-scope path: install MOSEK's JAR in your local Maven repository, and put MOSEK's native library on `java.library.path`.
- MOSEK gets the model as it is – all variables, including fixed ones, unscaled parameters and the objective's constant – and its own presolve and scaling do the rest.
- The configurator is looked up with `ExpressionsBasedModel.getConfigurator(...)`, so register a `SolverMosek.Configurator` with the model or its `Optimisation.Environment` (ojAlgo 57).

### Fixed

- Mixed-integer models solved to optimality were reported as `FAILED`.
- Unbounded models (a MOSEK certificate of dual infeasibility) were reported as `INFEASIBLE`.

## [3.2.0] – 2025-01-23

Built with ojAlgo 55.1.1 and MOSEK 11.0. The published POM is missing the ojAlgo dependency – declare it yourself.

### Changed

- Built against MOSEK 11 (was 7.1). The POM still declares the system-scope dependency `mosek:mosek` as version 7.1, with a fixed path to the JAR.
- Requires Java 17 (was Java 8).
- `SolverMosek.Configurator` is now a class: override `newTask(Env, int, int, Optimisation.Options)` to create and configure MOSEK's `Task`. It replaces the functional interface method `configure(Env, Task, Optimisation.Options)`.
- `SolverMosek.Integration` is public.
- MOSEK's log is printed to `options.logger_appender`, when set.
- The MOSEK environment is created when first needed, and disposed of after an hour without use.

### Removed

- The default configuration no longer passes `options.time_abort` to MOSEK as its MIP time limit.

## [3.1.1] – 2022-07-06

Built with ojAlgo 51.4.0 and MOSEK 7.1.

### Changed

- The published POM declares the system-scope MOSEK dependency with a fixed path to the JAR, instead of the overridable `path.jar.mosek` property.

## [3.1.0] – 2021-08-28

Built with ojAlgo 49.0.2 and MOSEK 7.1.

Dependency updates only.

## [3.0.0] – 2021-03-24

Built with ojAlgo 48.4.1 and MOSEK 7.1.

### Changed

- The module is versioned independently. The 2.47.x versions followed the ojAlgo major version.

## [2.47.2] – 2018-12-19

Built with ojAlgo 47.0.0 and MOSEK 7.1.

Initial release. `SolverMosek.INTEGRATION` lets `ExpressionsBasedModel` solve LP, QP, QCQP and mixed-integer models with MOSEK, and a `SolverMosek.Configurator` registered with the model's options can set MOSEK parameters. MOSEK's JAR (a system-scope dependency) and native libraries come from a local MOSEK installation.
