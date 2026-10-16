# Changelog

All notable changes to ojAlgo-ortools will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- LP results include dual values and the reduced gradient – OR-Tools' own, which follow the convention introduced in ojAlgo 57.5.0.
- Interrupting the solving thread asks OR-Tools to stop. Whether, and how soon, it does depends on the backend – CP-SAT stops promptly, while SCIP and HiGHS may keep running. If the backend refuses, an error is logged once.
- `Options.time_suffice` is used as the time limit, and if no feasible solution has been found by then, solving continues until `Options.time_abort`.

### Fixed

- The reported objective function value includes the objective's constant term.

## [2.0.2] – 2026-06-21

Built with ojAlgo 57.0.0 and OR-Tools 9.15.6755.

### Added

- `SolverORTools` is an `UpdatableSolver`: variable bounds can be changed (`updateRange(...)`, `fixVariable(...)`) and the model solved again without rebuilding it, and dual values and reduced costs are available after a solve.
- `SolverORTools.InternalSolver` lists the backends available through OR-Tools (GLOP, PDLP, SCIP, HIGHS, SAT, GLPK, CPLEX, GUROBI, XPRESS) with their capabilities and licensing. Preferred backends can be given to the new `Configurator(InternalSolver...)` constructor.
- OR-Tools' log output is forwarded to `Options.logger_appender` when `Options.logger_detailed` is set.

### Changed

- The `Configurator` is looked up from the model's `Optimisation.Environment` (`ExpressionsBasedModel.setConfigurator(...)`) rather than from `Optimisation.Options`.

### Fixed

- An `UNBOUNDED` status from OR-Tools is reported as `UNBOUNDED`, not `INFEASIBLE`.

## [2.0.1] – 2025-08-24

Built with ojAlgo 56.0.0 and OR-Tools 9.14.6206.

Dependency updates only.

## [2.0.0] – 2025-01-18

Built with ojAlgo 55.1.1 and OR-Tools 9.11.4210. The published POM is missing the ojAlgo dependency – declare it yourself.

### Added

- `Options.time_abort` is passed on to OR-Tools as a time limit.

### Changed

- Reimplemented on OR-Tools' `ModelBuilder` API (`ModelBuilderHelper` and `ModelSolverHelper`) instead of `MPSolver`. This breaks custom configurators: `SolverORTools.Configurator` is now a class to subclass – override `getSolverName(...)`, `newModelBuilderHelper()` or `newModelSolverHelper(...)` – rather than a functional interface receiving the `MPSolver`.
- MIP models are solved with SCIP by default (was CBC). LP models are still solved with GLOP.
- OR-Tools' own log output is suppressed.
- `SolverORTools.Integration` is no longer public – use `SolverORTools.INTEGRATION`.

## [1.0.0] – 2022-09-28

Built with ojAlgo 52.0.0 and OR-Tools 9.4.1874.

Initial release. Use Google OR-Tools, via `SolverORTools.INTEGRATION` and its `MPSolver` API, for linear models – GLOP for LP and CBC for MIP. The `ortools-java` dependency, from Maven Central, bundles the native libraries.
