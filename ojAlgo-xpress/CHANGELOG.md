# Changelog

All notable changes to ojAlgo-xpress will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

## [1.2.0] – 2026-10-16

Built with ojAlgo 57.5.0 and FICO Xpress 47.01.

### Added

- Infeasibility certificates: an `INFEASIBLE` LP result has Xpress' dual ray as its dual values, plus the certificate's reduced gradient. There is one only when the dual simplex, not presolve, proved the infeasibility.

### Changed

- Dual values follow the convention introduced in ojAlgo 57.5.0: Xpress' own dual values, the rate of change of the objective function value with the constraint limits, are passed through unchanged.

### Fixed

- MIP status codes were mapped off by one: a MIP stopped with an integer solution (e.g. at the time limit) was reported `INFEASIBLE`, and an infeasible MIP `FEASIBLE`. A MIP with an unbounded LP relaxation is now reported `INVALID` (infeasible or unbounded) rather than `FAILED`.

## [1.1.0] – 2026-10-09

Built with ojAlgo 57.4.0 and FICO Xpress 47.01.

### Added

- A configured feasibility tolerance (`Options.getConfiguredFeasibilityTolerance()`) and MIP gap (`Options.getConfiguredGapTolerance()`) are passed on to Xpress.
- Results of optimal continuous models include dual values and the reduced gradient.

### Fixed

- The objective function value returned by the solver includes the objective's constant.
- Quadratic objective terms given for both (i,j) and (j,i) are combined into one element before being passed to Xpress.

## [1.0.0] – 2026-09-14

Built with ojAlgo 57.3.0 and FICO Xpress 47.01.

Initial release. `SolverXpress` lets `ExpressionsBasedModel` use FICO Xpress for LP, QP and their mixed-integer variants (no quadratic constraints), with time and iteration limits, parallelism, interruption, a customisable `Configurator` and an updatable solver (variable bounds can be changed between solves). The Xpress Java API (`xprs.jar`) is a `provided` dependency that you install into your local Maven repository yourself; the native library must be on the library path and `XPRESSDIR`/`XPAUTH_PATH` set.
