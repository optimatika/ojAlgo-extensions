# Changelog

All notable changes to ojAlgo-joptimizer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- Convexity check: non-convex quadratic objectives or constraints, and two-sided (or equality) quadratic constraints, are rejected with an `IllegalArgumentException`.
- An LP that fails is solved once more with JOptimizer's default tolerances. That second attempt can be customised by overriding `Configurator.configureFallback(...)`.

### Changed

- Tighter tolerances than JOptimizer's defaults, for more accurate solutions.
- JOptimizer's LP solver confines every variable to [-99999, 99999]. A solution at such an artificial limit is reported as `FEASIBLE` rather than `OPTIMAL`, and an LP that is infeasible within those limits as `FAILED` rather than `INFEASIBLE` (unless all variables are bounded within them).

### Fixed

- Unchecked exceptions thrown by JOptimizer (e.g. with rank deficient equality constraints) result in `FAILED` rather than being propagated.

## [3.1.0] – 2026-10-11

Built with ojAlgo 57.4.0 and JOptimizer 5.0.0.

### Changed

- The `Configurator` is looked up from the model's `Optimisation.Environment` (`ExpressionsBasedModel.setConfigurator(...)`), as ojAlgo no longer has `Options.setConfigurator(...)`.
- JOptimizer's log output is attached to JOptimizer's own log4j logger, once and only while `Options.logger_appender` is set, instead of adding an appender to (and setting DEBUG on) the root logger at every solve.

### Fixed

- LP: the bounds of variables not in the objective function were ignored.
- Quadratic models: variable bounds were ignored, only the first lower and upper limited constraints were used (repeated in place of the others), and lower limits had the wrong sign.

## [3.0.4] – 2026-06-21

Built with ojAlgo 57.0.0 and JOptimizer 5.0.0.

Dependency updates only.

## [3.0.3] – 2025-01-21

Built with ojAlgo 55.1.1 and JOptimizer 5.0.0. The published POM is missing the ojAlgo dependency – declare it yourself.

### Changed

- The package is renamed from `org.ojalgo.joptimizer` to `org.ojalgo.optimisation.solver.joptimizer`.
- `SolverJOptimizer.Configurator` is a class to subclass – override `configure(...)` or the new `newOptimizationRequestHandler(...)` – rather than a functional interface. A configured instance replaces the default configuration instead of being applied after it.
- `SolverJOptimizer.Integration` is no longer public – use `SolverJOptimizer.INTEGRATION`.
- Requires Java 11 (was Java 8).

## [3.0.2] – 2022-07-06

Built with ojAlgo 51.4.0 and JOptimizer 5.0.0.

### Added

- JOptimizer's log4j output is forwarded to `Options.logger_appender`, when set.

## [3.0.1] – 2022-01-02

Built with ojAlgo 50.0.0 and JOptimizer 5.0.0.

Dependency updates only.

## [3.0.0] – 2021-03-24

Built with ojAlgo 48.4.1 and JOptimizer 5.0.0.

### Added

- Quadratic models, solved with JOptimizer's Newton-based methods (`NewtonUnconstrained` and its chain) rather than the LP solver.

### Changed

- Versioned independently of the other ojAlgo extensions, and the published POM no longer has the `ojalgo-extensions` parent.

### Fixed

- The result state was always `OPTIMAL`. It is now `INFEASIBLE` when JOptimizer reports an infeasible problem, and `FAILED` when it fails otherwise.

## [2.47.2] – 2018-12-19

Built with ojAlgo 47.0.0 and JOptimizer 5.0.0.

Initial release. Use JOptimizer's primal-dual interior-point LP solver, via `SolverJOptimizer.INTEGRATION` (package `org.ojalgo.joptimizer`), for continuous linear models. A `SolverJOptimizer.Configurator` can adjust the JOptimizer request and solver.
