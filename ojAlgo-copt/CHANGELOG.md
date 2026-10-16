# Changelog

All notable changes to ojAlgo-copt will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- Infeasibility certificates: an `INFEASIBLE` LP result has COPT's Farkas ray as its dual values, plus the certificate's reduced gradient. The default `Configurator` requests the Farkas ray from COPT.

### Changed

- Dual values follow the convention introduced in ojAlgo 57.5.0: COPT's own dual values, the rate of change of the objective function value with the constraint limits, are passed through unchanged.
- COPT's `IMPRECISE` status is reported as `APPROXIMATE` (previously `FAILED`).

## [1.1.0] – 2026-10-09

Built with ojAlgo 57.4.0 and COPT 8.0.7.

### Added

- A configured feasibility tolerance (`Options.getConfiguredFeasibilityTolerance()`) and MIP gap (`Options.getConfiguredGapTolerance()`) are passed on to COPT.
- Results of optimal continuous models (without quadratic constraints) include dual values and the reduced gradient.

### Changed

- The model is passed to COPT as it is – all variables, including fixed ones, and unscaled constraints and objective – leaving presolve and scaling to COPT.

### Fixed

- A model stopped at a limit now returns the solution COPT has, and a MIP with an integer solution is then reported `FEASIBLE`. Previously the state was `APPROXIMATE` and no solution was returned.
- When COPT reports unbounded, or infeasible-or-unbounded, the integration determines which: `UNBOUNDED` with a feasible solution, or `INFEASIBLE`.
- The objective function value returned by the solver includes the objective's constant.
- `getDualMultiplier(int)` looked up the wrong constraint for models with quadratic constraints.

## [1.0.0] – 2026-09-14

Built with ojAlgo 57.3.0 and COPT 8.0.6.

Initial release. `SolverCOPT` lets `ExpressionsBasedModel` use COPT (Cardinal Optimizer) for LP, QP, QCQP and their mixed-integer variants, with time and iteration limits, parallelism, interruption, a customisable `Configurator` and an updatable solver (variable bounds can be changed between solves). The COPT Java API is a `provided` dependency that you install into your local Maven repository yourself, and the COPT native library must be on the library path.
